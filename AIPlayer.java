import greenfoot.*;
import java.util.List;

/**
 * Jogador controlado por IA, usado no modo solo (1 jogador vs IA).
 *
 * A cada intervalo de "reação" (que varia com a dificuldade), mede o
 * espaço livre que existe reto, à esquerda e à direita (até onde bate
 * numa parede ou num rastro perigoso) e escolhe a direção com mais
 * espaço. Isso evita que ela bata "no escuro" e faz ela reagir antes
 * de chegar perto demais, em vez de só desviar no último instante.
 *
 * Dificuldades mais altas reagem mais rápido (intervalo menor) e
 * conseguem "ver" mais longe (probeMax maior), por isso jogam melhor.
 * No FÁCIL a IA ainda comete erros ocasionais de propósito.
 *
 * Rastros perigosos = os do adversário + os PRÓPRIOS rastros antigos
 * (os recentes não contam, mesma regra de imunidade usada em Player).
 */
public class AIPlayer extends Player {

    public enum Difficulty { FACIL, MEDIO, DIFICIL }

    private static final int PROBE_STEP = 8; // resolução da medição de espaço livre
    private static final int WALL_MARGIN = 8;
    private static final int KEEP_DIRECTION_BONUS = 14; // evita ficar trocando de direção sem necessidade
    private static final int EASY_MISTAKE_PERCENT = 4; // chance de errar de propósito a cada decisão (só no FÁCIL)

    private final Difficulty difficulty;
    private int reactionCounter = 0;
    private int chosenDirection;

    public AIPlayer(boolean isPlayer1, Difficulty difficulty) {
        super(isPlayer1);
        this.difficulty = difficulty;
        this.chosenDirection = getDirection();
    }

    public void act() {
        TronWorld world = (TronWorld) getWorld();
        if (world == null || isDead() || !world.isPlaying()) return;

        decideDirection(world);
        super.act();
    }

    private void decideDirection(TronWorld world) {
        reactionCounter++;
        if (reactionCounter < reactionInterval()) {
            setNextDirection(chosenDirection);
            return;
        }
        reactionCounter = 0;

        int probeMax = probeDistance();
        int current = getDirection();
        int left = turnLeft(current);
        int right = turnRight(current);

        int spaceCurrent = openSpace(world, current, probeMax);
        int spaceLeft = openSpace(world, left, probeMax);
        int spaceRight = openSpace(world, right, probeMax);

        // Pequeno bônus pra direção atual: evita "tremer" trocando de
        // direção toda hora quando os espaços são quase iguais
        int bestAlt = Math.max(spaceLeft, spaceRight);
        if (spaceCurrent + KEEP_DIRECTION_BONUS >= bestAlt) {
            chosenDirection = current;
        } else if (spaceLeft >= spaceRight) {
            chosenDirection = left;
        } else {
            chosenDirection = right;
        }

        // No FÁCIL, às vezes faz uma curva aleatória (erro humano de propósito)
        if (difficulty == Difficulty.FACIL && Greenfoot.getRandomNumber(100) < EASY_MISTAKE_PERCENT) {
            chosenDirection = (Greenfoot.getRandomNumber(2) == 0) ? left : right;
        }

        setNextDirection(chosenDirection);
    }

    private int reactionInterval() {
        if (difficulty == Difficulty.FACIL) return 12;
        if (difficulty == Difficulty.MEDIO) return 5;
        return 1; // DIFICIL: reage a cada frame
    }

    private int probeDistance() {
        if (difficulty == Difficulty.FACIL) return 60;
        if (difficulty == Difficulty.MEDIO) return 100;
        return 160; // DIFICIL: "vê" bem mais longe
    }

    private int turnLeft(int dir) {
        switch (dir) {
            case 0:   return 90;
            case 90:  return 180;
            case 180: return 270;
            default:  return 0; // 270 -> 0
        }
    }

    private int turnRight(int dir) {
        switch (dir) {
            case 0:   return 270;
            case 270: return 180;
            case 180: return 90;
            default:  return 0; // 90 -> 0
        }
    }

    // Mede, em pixels, até onde dá pra andar na direção "dir" antes de bater
    // numa parede ou num rastro perigoso (do adversário, ou o próprio já "velho").
    private int openSpace(TronWorld world, int dir, int maxDistance) {
        int safeDistance = 0;

        for (int d = PROBE_STEP; d <= maxDistance; d += PROBE_STEP) {
            int dx = 0, dy = 0;
            switch (dir) {
                case 0:   dx = d; break;
                case 180: dx = -d; break;
                case 90:  dy = -d; break;
                case 270: dy = d; break;
            }

            if (world.isOutOfArena(getX() + dx, getY() + dy, WALL_MARGIN)) {
                return safeDistance;
            }

            List<Trail> trails = getObjectsAtOffset(dx, dy, Trail.class);
            for (Trail trail : trails) {
                boolean own = trail.getOwner() == this;
                boolean old = (getTimer() - trail.getCreatedAt()) > SELF_TRAIL_IMMUNE_FRAMES;
                if (!own || old) {
                    return safeDistance; // rastro perigoso
                }
            }

            safeDistance = d;
        }

        return safeDistance;
    }
}