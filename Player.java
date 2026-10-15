import greenfoot.*;
import java.util.List;

/**
 * Um dos competidores do Tron. Pode ser controlado por um humano,
 * através de setNextDirection (chamado pelo TronWorld a cada frame),
 * ou servir de classe-base para um jogador de IA (veja AIPlayer, que
 * sobrescreve act() pra decidir sua própria direção antes de delegar
 * pra esta classe).
 *
 * Quando o jogador morre (parede, rastro ou colisão frontal), ele NÃO chama
 * endGame direto: apenas avisa o mundo (reportCrash). O mundo resolve todas as
 * mortes juntas, então se os dois morrem no mesmo frame o resultado é empate,
 * e o jogador 1 deixa de ter "azar" por agir primeiro.
 */
public class Player extends Actor {
    private static final int BASE_SPEED = 3;
    private static final int MAX_SPEED = 6;
    private static final int SPEEDUP_INTERVAL = 300; // a cada N frames, a velocidade sobe 1

    // ignora só o PRÓPRIO rastro mais recente (também usado pela IA)
    protected static final int SELF_TRAIL_IMMUNE_FRAMES = 12;

    private static final int BODY_SIZE = 10;
    private static final int GLOW_MARGIN = 4; // moldura de brilho neon ao redor do corpo
    private static final int WALL_MARGIN = BODY_SIZE / 2 + 1; // distância do centro até a parede que já conta como batida

    // Duração (em frames, ~60 por segundo) e intensidade dos efeitos de power-up
    private static final int SPEED_BOOST_DURATION = 180;
    private static final int SHIELD_DURATION = 150;
    private static final int SLOW_DURATION = 150;
    private static final int SPEED_BOOST_BONUS = 3;
    private static final int SLOW_PENALTY = 2;
    private static final int SHIELD_WARNING_FRAMES = 40; // escudo pisca quando está acabando

    private int direction;
    private int nextDirection;
    private int timer = 0;
    private boolean isPlayer1;
    private boolean dead = false;

    private Color bodyColor;
    private Color glowColor;
    private final GreenfootImage trailImageH;
    private final GreenfootImage trailImageV;

    private int speedBoostFrames = 0;
    private int shieldFrames = 0;
    private int slowFrames = 0;

    public Player(boolean isPlayer1) {
        this.isPlayer1 = isPlayer1;
        this.direction = isPlayer1 ? 0 : 180;
        this.nextDirection = this.direction;

        Color trailColor;
        if (isPlayer1) {
            bodyColor = new Color(0, 120, 255);
            trailColor = new Color(0, 200, 255);
        } else {
            bodyColor = new Color(255, 140, 0);
            trailColor = new Color(255, 200, 0);
        }
        glowColor = trailColor;

        // As imagens do rastro são criadas uma vez só e reaproveitadas
        trailImageH = Trail.createImage(trailColor, true);
        trailImageV = Trail.createImage(trailColor, false);

        updateAppearance();
    }

    // Chamado pelo TronWorld a cada frame, no caso de jogadores humanos.
    public void setNextDirection(int dir) {
        this.nextDirection = dir;
    }

    public int getDirection() {
        return direction;
    }

    public boolean isShielded() {
        return shieldFrames > 0;
    }

    public boolean isDead() {
        return dead;
    }

    // usado pela IA para saber a "idade" dos rastros
    protected int getTimer() {
        return timer;
    }

    // ---------- Efeitos de power-up ----------

    public void activateSpeedBoost() {
        speedBoostFrames = SPEED_BOOST_DURATION;
        updateAppearance();
    }

    public void activateShield() {
        shieldFrames = SHIELD_DURATION;
        updateAppearance();
    }

    public void activateSlow() {
        slowFrames = SLOW_DURATION;
        updateAppearance();
    }

    // Dificuldade progressiva: a velocidade sobe com o tempo, até um teto,
    // ainda ajustada por power-ups ativos (impulso aumenta, lentidão reduz)
    private int getCurrentSpeed() {
        int speed = Math.min(BASE_SPEED + timer / SPEEDUP_INTERVAL, MAX_SPEED);
        if (speedBoostFrames > 0) speed += SPEED_BOOST_BONUS;
        if (slowFrames > 0) speed = Math.max(1, speed - SLOW_PENALTY);
        return speed;
    }

    public void act() {
        TronWorld world = (TronWorld) getWorld();
        // Só age durante a partida (congela na contagem, na pausa e no fim da rodada)
        if (world == null || dead || !world.isPlaying()) return;

        timer++;
        direction = nextDirection;
        tickEffects();

        int oldX = getX();
        int oldY = getY();

        movePlayer();

        if (world.isOutOfArena(getX(), getY(), WALL_MARGIN)) {
            die();
            return;
        }

        checkTrailCollision();
        if (dead) return;

        checkPlayerCollision();
        if (dead) return;

        checkPowerUpCollision();

        boolean horizontal = (direction == 0 || direction == 180);
        world.addObject(new Trail(horizontal ? trailImageH : trailImageV, this, timer), oldX, oldY);
    }

    // Avisa o mundo; quem decide o resultado da rodada é o TronWorld
    private void die() {
        if (dead) return;
        dead = true;

        shieldFrames = 0;
        speedBoostFrames = 0;
        slowFrames = 0;
        bodyColor = Color.WHITE;
        glowColor = new Color(255, 60, 60);
        updateAppearance();

        TronWorld world = (TronWorld) getWorld();
        if (world != null) world.reportCrash(this);
    }

    // Só redesenha a imagem quando algum efeito acaba (ou o escudo está piscando)
    private void tickEffects() {
        boolean changed = false;

        if (speedBoostFrames > 0 && --speedBoostFrames == 0) changed = true;
        if (slowFrames > 0 && --slowFrames == 0) changed = true;
        if (shieldFrames > 0) {
            shieldFrames--;
            if (shieldFrames == 0) changed = true;
            else if (shieldFrames < SHIELD_WARNING_FRAMES && shieldFrames % 4 == 0) changed = true;
        }

        if (changed) updateAppearance();
    }

    private void movePlayer() {
        int speed = getCurrentSpeed();
        switch (direction) {
            case 0:   setLocation(getX() + speed, getY()); break;
            case 180: setLocation(getX() - speed, getY()); break;
            case 90:  setLocation(getX(), getY() - speed); break;
            case 270: setLocation(getX(), getY() + speed); break;
        }
    }

    // Varre TODOS os pontos que o jogador vai percorrer no próximo frame (e não
    // só um), para nunca "pular" um rastro, mesmo em velocidade alta.
    private void checkTrailCollision() {
        if (isShielded()) return; // escudo: atravessa rastros sem morrer

        int speed = getCurrentSpeed();
        int ux = 0, uy = 0;
        switch (direction) {
            case 0:   ux = 1;  break;
            case 180: ux = -1; break;
            case 90:  uy = -1; break;
            case 270: uy = 1;  break;
        }

        for (int d = 1; d <= speed + 2; d += 2) {
            List<Trail> trails = getObjectsAtOffset(ux * d, uy * d, Trail.class);
            for (Trail trail : trails) {
                boolean isOwnTrail = trail.getOwner() == this;
                boolean isOldEnough = (timer - trail.getCreatedAt()) > SELF_TRAIL_IMMUNE_FRAMES;

                // Rastro do adversário: sempre fatal, desde o primeiro frame.
                // Rastro próprio: só fatal se for "antigo" (evita morrer no pedaço
                // que você mesmo acabou de criar atrás de você).
                if (!isOwnTrail || isOldEnough) {
                    die();
                    return;
                }
            }
        }
    }

    private void checkPlayerCollision() {
    Player other = (Player) getOneObjectAtOffset(0, 0, Player.class);
    if (other != null && other != this) {
        if (isShielded() || other.isShielded()) return; // escudo: atravessam um ao outro
        die();
        other.die(); // os dois morrem => o mundo decide "empate"
    }
}

private void checkPowerUpCollision() {
    PowerUp powerUp = (PowerUp) getOneObjectAtOffset(0, 0, PowerUp.class);
    if (powerUp != null) {
        applyPowerUp(powerUp);
        getWorld().removeObject(powerUp);
    }
}
    private void applyPowerUp(PowerUp powerUp) {
        switch (powerUp.getType()) {
            case VELOCIDADE:
                activateSpeedBoost();
                break;
            case ESCUDO:
                activateShield();
                break;
            case LENTIDAO:
                Player other = getOtherPlayer();
                if (other != null) other.activateSlow();
                break;
        }
    }

    private Player getOtherPlayer() {
        if (getWorld() == null) return null;
        List<Player> players = getWorld().getObjects(Player.class);
        for (Player p : players) {
            if (p != this) return p;
        }
        return null;
    }

    // Monta a imagem do jogador: corpo sólido + halo neon ao redor.
    // Com power-up ativo, desenha um anel por cima:
    // branco = escudo, amarelo = impulso de velocidade, rosa = lentidão.
    private void updateAppearance() {
        int total = BODY_SIZE + GLOW_MARGIN * 2;
        GreenfootImage image = new GreenfootImage(total, total);

        for (int i = GLOW_MARGIN; i >= 1; i--) {
            int alpha = 12 + (GLOW_MARGIN - i) * 12;
            int size = BODY_SIZE + i * 2;
            int offset = (total - size) / 2;
            image.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alpha));
            image.fillRect(offset, offset, size, size);
        }

        int bodyOffset = GLOW_MARGIN;
        image.setColor(bodyColor);
        image.fillRect(bodyOffset, bodyOffset, BODY_SIZE, BODY_SIZE);
        image.setColor(dead ? new Color(255, 60, 60) : Color.WHITE);
        image.fillRect(bodyOffset + 3, bodyOffset + 3, 4, 4);

        boolean shieldVisible = shieldFrames > 0
                && (shieldFrames >= SHIELD_WARNING_FRAMES || (shieldFrames / 4) % 2 == 0);
        if (shieldVisible) {
            image.setColor(new Color(255, 255, 255, 180));
            image.drawOval(0, 0, total - 1, total - 1);
            image.drawOval(1, 1, total - 3, total - 3);
        }

        if (speedBoostFrames > 0) {
            image.setColor(new Color(255, 255, 0, 150));
            image.drawOval(1, 1, total - 3, total - 3);
        }

        if (slowFrames > 0) {
            image.setColor(new Color(255, 60, 200, 170));
            image.drawOval(2, 2, total - 5, total - 5);
        }

        setImage(image);
    }
}