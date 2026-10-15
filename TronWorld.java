import greenfoot.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Mundo do Tron: controla menus, HUD, estados do jogo e o resultado das rodadas.
 *
 * Layout (640 x 480):
 *   - faixa de HUD no topo (nomes, pontos e título)
 *   - arena com grade e borda neon logo abaixo
 *
 * Menus: use SETAS (ou W/S) + ENTER/ESPAÇO. Atalhos numéricos também funcionam.
 * Durante o jogo: P (ou ESC) pausa.
 */
public class TronWorld extends World
{
    // ---------- Dimensões ----------
    public static final int WORLD_W = 640;
    public static final int WORLD_H = 480;
    public static final int HUD_H = 56;
    public static final int ARENA_LEFT = 12;
    public static final int ARENA_TOP = HUD_H + 8;
    public static final int ARENA_RIGHT = WORLD_W - 12;
    public static final int ARENA_BOTTOM = WORLD_H - 12;
    private static final int GRID = 20;

    // ---------- Estados ----------
    private enum State { MENU, DIFFICULTY, OPTIONS, HOW_TO, COUNTDOWN, PLAYING, PAUSED, ROUND_OVER, MATCH_OVER }
    private enum GameMode { DOIS_JOGADORES, VS_IA }
    private enum Result { P1, P2, EMPATE }

    // ---------- Configurações de jogo ----------
    private static final int COUNTDOWN_STEP_FRAMES = 50; // duração de cada número da contagem
    private static final int POWERUP_SPAWN_INTERVAL = 240; // ~4s a 60fps entre tentativas de spawn
    private static final int MAX_POWERUPS_ON_SCREEN = 2;
    private static final int[] SCORE_CHOICES = {3, 5, 7, 10};

    // ---------- Cores ----------
    private static final Color P1_COLOR = new Color(0, 180, 255);
    private static final Color P2_COLOR = new Color(255, 150, 0);
    private static final Color NEON_CYAN = new Color(0, 220, 255);
    private static final Color NEON_GREEN = new Color(50, 255, 100);
    private static final Color NEON_YELLOW = new Color(255, 210, 0);
    private static final Color NEON_RED = new Color(255, 70, 70);
    private static final Color TEXT_DIM = new Color(150, 165, 185);
    private static final Color TEXT_DARK = new Color(100, 112, 130);
    private static final Color SILVER = new Color(190, 200, 220);

    // ---------- Textos dos menus ----------
    private static final String[] MAIN_LABELS = {"JOGAR COM AMIGO", "JOGAR CONTRA A IA", "OPÇÕES", "COMO JOGAR"};
    private static final String[] MAIN_DESCS = {
        "2 jogadores no mesmo teclado",
        "1 jogador - escolha a dificuldade",
        "Pontos para vencer e power-ups",
        "Controles e power-ups"
    };
    private static final Color[] MAIN_COLORS = {P1_COLOR, P2_COLOR, NEON_YELLOW, SILVER};

    private static final String[] DIFF_LABELS = {"FÁCIL", "MÉDIO", "DIFÍCIL", "VOLTAR"};
    private static final String[] DIFF_DESCS = {
        "A IA reage devagar e erra de vez em quando",
        "A IA reage rápido e enxerga bem",
        "A IA reage a cada instante e enxerga longe",
        "Voltar ao menu principal"
    };
    private static final Color[] DIFF_COLORS = {NEON_GREEN, NEON_YELLOW, NEON_RED, SILVER};

    private static final String[] OPTION_LABELS = {"PONTOS PARA VENCER", "POWER-UPS", "VOLTAR"};

    // ---------- Alinhamento de texto / setas ----------
    private static final int ALIGN_LEFT = 0;
    private static final int ALIGN_CENTER = 1;
    private static final int ALIGN_RIGHT = 2;
    private static final int ARROW_NONE = 0;
    private static final int ARROW_UP = 1;
    private static final int ARROW_DOWN = 2;
    private static final int ARROW_LEFT = 3;
    private static final int ARROW_RIGHT = 4;

    // ---------- Estado do jogo ----------
    private State state;
    private int stateFrames = 0; // frames desde a última troca de estado
    private GameMode gameMode = GameMode.DOIS_JOGADORES;
    private AIPlayer.Difficulty difficulty = AIPlayer.Difficulty.MEDIO;

    private int scoreChoiceIndex = 1;
    private int winningScore = SCORE_CHOICES[1];
    private boolean powerUpsEnabled = true;

    private int mainIndex = 0;
    private int difficultyIndex = 1;
    private int optionsIndex = 0;

    private int scoreP1 = 0;
    private int scoreP2 = 0;
    private Result lastResult = Result.EMPATE;

    private Player player1;
    private Player player2;
    private final List<Player> crashed = new ArrayList<>();

    // Guarda a próxima direção de cada jogador
    private int nextDirP1 = 0;   // P1 começa indo pra direita
    private int nextDirP2 = 180; // P2 começa indo pra esquerda

    private int countdownNumber;
    private int countdownFramesLeft;
    private int powerUpSpawnCooldown = POWERUP_SPAWN_INTERVAL;

    // Cenário pronto (fundo, HUD, grade e borda): criado uma única vez
    private final GreenfootImage baseImage = buildBaseImage();

    // "Cartaz" que aparece por cima do jogo (contagem, pausa, fim de rodada)
    private final Actor banner = new Actor() {};
    private boolean bannerShown = false;

    private GreenfootSound crashSound = loadSound("crash.wav");
    private GreenfootSound victorySound = loadSound("victory.wav");
    private GreenfootSound countdownSound = loadSound("countdown.wav");

    public TronWorld() {
        super(WORLD_W, WORLD_H, 1);
        setPaintOrder(banner.getClass(), Player.class, PowerUp.class, Trail.class);
        showMainMenu();
    }

    // =====================================================================
    //  Ciclo principal
    // =====================================================================

    public void act() {
        String key = Greenfoot.getKey();
        if (key != null) key = key.toLowerCase();
        stateFrames++;

        switch (state) {
            case MENU:       updateMenu(key); break;
            case DIFFICULTY: updateDifficulty(key); break;
            case OPTIONS:    updateOptions(key); break;
            case HOW_TO:
                if (key != null && (isConfirm(key) || key.equals("escape"))) showMainMenu();
                break;
            case COUNTDOWN:  updateCountdown(key); break;
            case PLAYING:    updatePlaying(key); break;
            case PAUSED:     updatePaused(key); break;
            case ROUND_OVER: updateRoundOver(key); break;
            case MATCH_OVER: updateMatchOver(key); break;
        }
    }

    private void enter(State newState) {
        state = newState;
        stateFrames = 0;
    }

    // Usado pelos Actors para saber se a partida está rolando (congelam em pausa/contagem/fim)
    public boolean isPlaying() {
        return state == State.PLAYING;
    }

    // Verdadeiro se um jogador (centro em x,y) encostou na parede da arena
    public boolean isOutOfArena(int x, int y, int margin) {
        return x - margin <= ARENA_LEFT || x + margin >= ARENA_RIGHT
            || y - margin <= ARENA_TOP || y + margin >= ARENA_BOTTOM;
    }

    // =====================================================================
    //  Menus
    // =====================================================================

    private boolean isConfirm(String key) {
        return key != null && (key.equals("enter") || key.equals("space"));
    }

    private boolean isUp(String key) {
        return key.equals("up") || key.equals("w");
    }

    private boolean isDown(String key) {
        return key.equals("down") || key.equals("s");
    }

    // "1" -> 0, "2" -> 1 ... (-1 se não for um dígito de 1 a 9)
    private int digitOf(String key) {
        if (key != null && key.length() == 1 && key.charAt(0) >= '1' && key.charAt(0) <= '9') {
            return key.charAt(0) - '1';
        }
        return -1;
    }

    private int moveIndex(String key, int index, int count) {
        if (isUp(key)) return (index + count - 1) % count;
        if (isDown(key)) return (index + 1) % count;
        return index;
    }

    private void showMainMenu() {
        cleanupRound();
        enter(State.MENU);
        drawMainMenu();
    }

    private void updateMenu(String key) {
        if (key == null) return;

        int next = moveIndex(key, mainIndex, MAIN_LABELS.length);
        if (next != mainIndex) {
            mainIndex = next;
            drawMainMenu();
            return;
        }

        int digit = digitOf(key);
        if (digit >= 0 && digit < MAIN_LABELS.length) {
            mainIndex = digit;
            selectMainItem();
        } else if (isConfirm(key)) {
            selectMainItem();
        }
    }

    private void selectMainItem() {
        switch (mainIndex) {
            case 0:
                gameMode = GameMode.DOIS_JOGADORES;
                startMatch();
                break;
            case 1:
                gameMode = GameMode.VS_IA;
                showDifficulty();
                break;
            case 2:
                showOptions();
                break;
            default:
                enter(State.HOW_TO);
                drawHowTo();
                break;
        }
    }

    private void showDifficulty() {
        enter(State.DIFFICULTY);
        drawDifficulty();
    }

    private void updateDifficulty(String key) {
        if (key == null) return;
        if (key.equals("escape")) {
            showMainMenu();
            return;
        }

        int next = moveIndex(key, difficultyIndex, DIFF_LABELS.length);
        if (next != difficultyIndex) {
            difficultyIndex = next;
            drawDifficulty();
            return;
        }

        int digit = digitOf(key);
        if (digit >= 0 && digit < 3) {
            difficultyIndex = digit;
            chooseDifficulty();
        } else if (isConfirm(key)) {
            chooseDifficulty();
        }
    }

    private void chooseDifficulty() {
        if (difficultyIndex == 3) {
            showMainMenu();
            return;
        }
        difficulty = AIPlayer.Difficulty.values()[difficultyIndex];
        startMatch();
    }

    private void showOptions() {
        optionsIndex = 0;
        enter(State.OPTIONS);
        drawOptions();
    }

    private void updateOptions(String key) {
        if (key == null) return;
        if (key.equals("escape")) {
            showMainMenu();
            return;
        }

        int next = moveIndex(key, optionsIndex, OPTION_LABELS.length);
        if (next != optionsIndex) {
            optionsIndex = next;
            drawOptions();
            return;
        }

        int delta = 0;
        if (key.equals("left") || key.equals("a")) delta = -1;
        else if (key.equals("right") || key.equals("d")) delta = 1;
        else if (isConfirm(key)) delta = 1;
        if (delta == 0) return;

        if (optionsIndex == 0) {
            scoreChoiceIndex = (scoreChoiceIndex + delta + SCORE_CHOICES.length) % SCORE_CHOICES.length;
            winningScore = SCORE_CHOICES[scoreChoiceIndex];
            drawOptions();
        } else if (optionsIndex == 1) {
            powerUpsEnabled = !powerUpsEnabled;
            drawOptions();
        } else if (isConfirm(key)) {
            showMainMenu();
        }
    }

    // =====================================================================
    //  Partida: contagem, jogo, pausa, fim de rodada / partida
    // =====================================================================

    private void startMatch() {
        scoreP1 = 0;
        scoreP2 = 0;
        startCountdown();
    }

    private void startCountdown() {
        cleanupRound();
        drawBase();
        drawHud();
        spawnPlayers();

        countdownNumber = 3;
        countdownFramesLeft = COUNTDOWN_STEP_FRAMES;
        enter(State.COUNTDOWN);
        showBanner(buildCountdownBanner());
        playSafely(countdownSound);
    }

    private void updateCountdown(String key) {
        if ("escape".equals(key)) {
            showMainMenu();
            return;
        }

        countdownFramesLeft--;
        if (countdownFramesLeft > 0) return;

        countdownNumber--;
        if (countdownNumber < 0) {
            clearBanner();
            powerUpSpawnCooldown = POWERUP_SPAWN_INTERVAL;
            enter(State.PLAYING);
            return;
        }

        countdownFramesLeft = COUNTDOWN_STEP_FRAMES;
        showBanner(buildCountdownBanner());
        playSafely(countdownSound);
    }

    private void updatePlaying(String key) {
        if (key != null && (key.equals("p") || key.equals("escape"))) {
            enter(State.PAUSED);
            showBanner(buildPauseBanner());
            return;
        }

        resolveCrashes();
        if (state != State.PLAYING) return;

        handleInput();
        updatePowerUps();
    }

    private void updatePaused(String key) {
        if (key == null || stateFrames < 15) return; // evita "repetição de tecla" retomar na hora

        if (key.equals("p") || key.equals("escape") || isConfirm(key)) {
            clearBanner();
            enter(State.PLAYING);
        } else if (key.equals("m")) {
            showMainMenu();
        }
    }

    private void updateRoundOver(String key) {
        if (key == null || stateFrames < 40) return; // pequena trava para não pular o resultado sem querer

        if (isConfirm(key)) startCountdown();
        else if (key.equals("m") || key.equals("escape")) showMainMenu();
    }

    private void updateMatchOver(String key) {
        if (key == null || stateFrames < 40) return;

        if (isConfirm(key)) startMatch();
        else if (key.equals("m") || key.equals("escape")) showMainMenu();
    }

    // ---------- Mortes ----------

    // Os jogadores avisam aqui quando morrem; o mundo resolve tudo junto.
    public void reportCrash(Player p) {
        if (!crashed.contains(p)) crashed.add(p);
    }

    private void resolveCrashes() {
        if (crashed.isEmpty()) return;

        boolean p1Died = crashed.contains(player1);
        boolean p2Died = crashed.contains(player2);
        crashed.clear();

        if (p1Died && p2Died) endRound(Result.EMPATE);
        else if (p1Died) endRound(Result.P2);
        else endRound(Result.P1);
    }

    private void endRound(Result result) {
        if (state != State.PLAYING) return; // evita processar 2x

        playSafely(crashSound);

        if (result == Result.P1) scoreP1++;
        else if (result == Result.P2) scoreP2++;
        lastResult = result;

        // O resto da rodada fica congelado na tela (rastros à vista) com o cartaz por cima
        drawBase();
        drawHud();

        if (scoreP1 >= winningScore || scoreP2 >= winningScore) {
            enter(State.MATCH_OVER);
            playSafely(victorySound);
            showBanner(buildMatchOverBanner());
        } else {
            enter(State.ROUND_OVER);
            showBanner(buildRoundOverBanner());
        }
    }

    // Remove tudo da rodada anterior (jogadores, rastros, power-ups e cartaz)
    private void cleanupRound() {
        removeObjects(getObjects(Player.class));
        removeObjects(getObjects(Trail.class));
        removeObjects(getObjects(PowerUp.class));
        clearBanner();
        crashed.clear();
        player1 = null;
        player2 = null;
    }

    private void spawnPlayers() {
        nextDirP1 = 0;
        nextDirP2 = 180;

        player1 = new Player(true);
        if (gameMode == GameMode.VS_IA) {
            player2 = new AIPlayer(false, difficulty);
        } else {
            player2 = new Player(false);
        }

        int centerY = (ARENA_TOP + ARENA_BOTTOM) / 2;
        addObject(player1, ARENA_LEFT + 120, centerY);
        addObject(player2, ARENA_RIGHT - 120, centerY);
    }

    // ---------- Entrada dos jogadores ----------

    private void handleInput() {
        // Lê TODAS as teclas pressionadas neste frame
        if (Greenfoot.isKeyDown("up"))    trySetP1(90);
        if (Greenfoot.isKeyDown("down"))  trySetP1(270);
        if (Greenfoot.isKeyDown("left"))  trySetP1(180);
        if (Greenfoot.isKeyDown("right")) trySetP1(0);
        if (player1 != null) player1.setNextDirection(nextDirP1);

        // No modo vs IA, o player2 (AIPlayer) decide a própria direção sozinho
        if (gameMode == GameMode.DOIS_JOGADORES) {
            if (Greenfoot.isKeyDown("w")) trySetP2(90);
            if (Greenfoot.isKeyDown("s")) trySetP2(270);
            if (Greenfoot.isKeyDown("a")) trySetP2(180);
            if (Greenfoot.isKeyDown("d")) trySetP2(0);
            if (player2 != null) player2.setNextDirection(nextDirP2);
        }
    }

    // Só muda se não for direção oposta à atual do P1
    private void trySetP1(int dir) {
        if (player1 != null && !saoOpostos(dir, player1.getDirection())) {
            nextDirP1 = dir;
        }
    }

    // Só muda se não for direção oposta à atual do P2
    private void trySetP2(int dir) {
        if (player2 != null && !saoOpostos(dir, player2.getDirection())) {
            nextDirP2 = dir;
        }
    }

    private boolean saoOpostos(int d1, int d2) {
        return (d1 == 0 && d2 == 180) || (d1 == 180 && d2 == 0) ||
               (d1 == 90 && d2 == 270) || (d1 == 270 && d2 == 90);
    }

    // ---------- Power-ups ----------

    private void updatePowerUps() {
        if (!powerUpsEnabled) return;

        powerUpSpawnCooldown--;
        if (powerUpSpawnCooldown > 0) return;
        powerUpSpawnCooldown = POWERUP_SPAWN_INTERVAL;

        if (getObjects(PowerUp.class).size() >= MAX_POWERUPS_ON_SCREEN) return;
        spawnRandomPowerUp();
    }

    // Tenta algumas posições até achar uma livre (sem jogador, rastro ou outro power-up)
    private void spawnRandomPowerUp() {
        int margin = 40;
        for (int tries = 0; tries < 20; tries++) {
            int x = ARENA_LEFT + margin + Greenfoot.getRandomNumber(ARENA_RIGHT - ARENA_LEFT - margin * 2);
            int y = ARENA_TOP + margin + Greenfoot.getRandomNumber(ARENA_BOTTOM - ARENA_TOP - margin * 2);

            if (isFree(x, y)) {
                PowerUp.Type[] types = PowerUp.Type.values();
                PowerUp.Type type = types[Greenfoot.getRandomNumber(types.length)];
                addObject(new PowerUp(type), x, y);
                return;
            }
        }
    }

    private boolean isFree(int x, int y) {
        for (int dx = -14; dx <= 14; dx += 14) {
            for (int dy = -14; dy <= 14; dy += 14) {
                if (!getObjectsAt(x + dx, y + dy, Actor.class).isEmpty()) return false;
            }
        }
        return true;
    }

    // =====================================================================
    //  Nomes e textos
    // =====================================================================

    private String nameOf(boolean isP1) {
        if (isP1) return "JOGADOR 1";
        return gameMode == GameMode.VS_IA ? "IA" : "JOGADOR 2";
    }

    private String difficultyLabel() {
        switch (difficulty) {
            case FACIL:   return "FÁCIL";
            case MEDIO:   return "MÉDIO";
            default:      return "DIFÍCIL";
        }
    }

    // =====================================================================
    //  Desenho: cenário base e HUD
    // =====================================================================

    private GreenfootImage buildBaseImage() {
        GreenfootImage img = new GreenfootImage(WORLD_W, WORLD_H);

        // fundo em degradê escuro
        for (int y = 0; y < WORLD_H; y++) {
            img.setColor(new Color(4 + y * 6 / WORLD_H, 8 + y * 10 / WORLD_H, 20 + y * 18 / WORLD_H));
            img.drawLine(0, y, WORLD_W - 1, y);
        }

        // faixa do HUD
        img.setColor(new Color(2, 5, 12));
        img.fillRect(0, 0, WORLD_W, HUD_H);
        img.setColor(new Color(0, 200, 255, 70));
        img.drawLine(0, HUD_H, WORLD_W, HUD_H);

        // piso da arena + grade
        int arenaW = ARENA_RIGHT - ARENA_LEFT;
        int arenaH = ARENA_BOTTOM - ARENA_TOP;
        img.setColor(new Color(3, 7, 15));
        img.fillRect(ARENA_LEFT, ARENA_TOP, arenaW, arenaH);

        img.setColor(new Color(0, 200, 255, 18));
        for (int x = ARENA_LEFT + GRID; x < ARENA_RIGHT; x += GRID) {
            img.drawLine(x, ARENA_TOP, x, ARENA_BOTTOM);
        }
        for (int y = ARENA_TOP + GRID; y < ARENA_BOTTOM; y += GRID) {
            img.drawLine(ARENA_LEFT, y, ARENA_RIGHT, y);
        }

        // borda neon: camadas translúcidas (halo) + linha sólida no centro
        for (int t = 7; t >= 3; t -= 2) {
            img.setColor(new Color(0, 200, 255, 10 + (7 - t) * 9));
            drawFrame(img, t);
        }
        img.setColor(NEON_CYAN);
        drawFrame(img, 2);

        return img;
    }

    // Moldura retangular de espessura 2*t, centrada na borda da arena
    private void drawFrame(GreenfootImage img, int t) {
        int w = ARENA_RIGHT - ARENA_LEFT;
        int h = ARENA_BOTTOM - ARENA_TOP;
        img.fillRect(ARENA_LEFT - t, ARENA_TOP - t, w + 2 * t, 2 * t);
        img.fillRect(ARENA_LEFT - t, ARENA_BOTTOM - t, w + 2 * t, 2 * t);
        img.fillRect(ARENA_LEFT - t, ARENA_TOP - t, 2 * t, h + 2 * t);
        img.fillRect(ARENA_RIGHT - t, ARENA_TOP - t, 2 * t, h + 2 * t);
    }

    private void drawBase() {
        getBackground().drawImage(baseImage, 0, 0);
    }

    // HUD da partida: nomes, "bolinhas" de pontos e título/modo no centro
    private void drawHud() {
        GreenfootImage bg = getBackground();

        bg.setColor(P1_COLOR);
        bg.fillRect(14, 12, 12, 12);
        drawText(bg, nameOf(true), 32, 18, 16, Color.WHITE, ALIGN_LEFT);
        drawPips(bg, 18, 34, scoreP1, P1_COLOR, false);

        bg.setColor(P2_COLOR);
        bg.fillRect(WORLD_W - 26, 12, 12, 12);
        drawText(bg, nameOf(false), WORLD_W - 32, 18, 16, Color.WHITE, ALIGN_RIGHT);
        drawPips(bg, WORLD_W - 28, 34, scoreP2, P2_COLOR, true);

        drawGlowText(bg, "TRON", WORLD_W / 2, 20, 26, NEON_CYAN, ALIGN_CENTER);
        String mode = (gameMode == GameMode.VS_IA) ? "VS IA " + difficultyLabel() : "2 JOGADORES";
        drawText(bg, mode + "  -  PRIMEIRO A " + winningScore, WORLD_W / 2, 42, 11, TEXT_DIM, ALIGN_CENTER);
    }

    private void drawPips(GreenfootImage bg, int startX, int y, int score, Color color, boolean rightToLeft) {
        for (int i = 0; i < winningScore; i++) {
            int x = rightToLeft ? startX - i * 14 : startX + i * 14;
            if (i < score) {
                bg.setColor(color);
                bg.fillOval(x, y, 10, 10);
            } else {
                bg.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
                bg.drawOval(x, y, 10, 10);
            }
        }
    }

    // =====================================================================
    //  Desenho: telas de menu
    // =====================================================================

    // Cenário + título grande na faixa do topo (usado por todos os menus)
    private void drawMenuBackdrop() {
        drawBase();
        GreenfootImage bg = getBackground();
        drawGlowText(bg, "TRON", WORLD_W / 2, 22, 32, NEON_CYAN, ALIGN_CENTER);
        drawText(bg, "CICLOS DE LUZ", WORLD_W / 2, 46, 11, TEXT_DIM, ALIGN_CENTER);
    }

    private void drawMainMenu() {
        drawMenuBackdrop();
        GreenfootImage bg = getBackground();

        drawPanel(bg, 130, 92, 380, 340, NEON_CYAN);
        drawText(bg, "MENU PRINCIPAL", 320, 116, 15, TEXT_DIM, ALIGN_CENTER);

        for (int i = 0; i < MAIN_LABELS.length; i++) {
            drawButton(bg, 320, 166 + i * 60, 330, 50, MAIN_LABELS[i], MAIN_DESCS[i], i == mainIndex, MAIN_COLORS[i]);
        }

        drawText(bg, "SETAS: navegar    ENTER: escolher    1-4: atalho", 320, 412, 12, TEXT_DARK, ALIGN_CENTER);
    }

    private void drawDifficulty() {
        drawMenuBackdrop();
        GreenfootImage bg = getBackground();

        drawPanel(bg, 130, 92, 380, 340, P2_COLOR);
        drawText(bg, "JOGAR CONTRA A IA", 320, 116, 15, P2_COLOR, ALIGN_CENTER);
        drawText(bg, "Escolha a dificuldade", 320, 138, 13, TEXT_DIM, ALIGN_CENTER);

        for (int i = 0; i < DIFF_LABELS.length; i++) {
            drawButton(bg, 320, 188 + i * 56, 330, 48, DIFF_LABELS[i], DIFF_DESCS[i], i == difficultyIndex, DIFF_COLORS[i]);
        }

        drawText(bg, "SETAS: navegar    ENTER: escolher    1-3: atalho    ESC: voltar", 320, 412, 11, TEXT_DARK, ALIGN_CENTER);
    }

    private void drawOptions() {
        drawMenuBackdrop();
        GreenfootImage bg = getBackground();

        drawPanel(bg, 130, 92, 380, 340, NEON_YELLOW);
        drawText(bg, "OPÇÕES", 320, 116, 15, NEON_YELLOW, ALIGN_CENTER);

        String[] values = {
            "<   " + winningScore + "   >",
            powerUpsEnabled ? "<   LIGADOS   >" : "<   DESLIGADOS   >",
            "Voltar ao menu principal"
        };
        Color[] colors = {NEON_YELLOW, NEON_GREEN, SILVER};

        for (int i = 0; i < OPTION_LABELS.length; i++) {
            drawButton(bg, 320, 176 + i * 64, 330, 52, OPTION_LABELS[i], values[i], i == optionsIndex, colors[i]);
        }

        drawText(bg, "SETAS: escolher/ajustar    ENTER: alterar    ESC: voltar", 320, 412, 11, TEXT_DARK, ALIGN_CENTER);
    }

    private void drawHowTo() {
        drawMenuBackdrop();
        GreenfootImage bg = getBackground();

        drawPanel(bg, 50, 72, 540, 380, NEON_CYAN);
        drawGlowText(bg, "COMO JOGAR", 320, 96, 20, Color.WHITE, ALIGN_CENTER);
        drawText(bg, "Não encoste nas paredes nem nos rastros. O último vivo ganha o ponto!", 320, 122, 13, TEXT_DIM, ALIGN_CENTER);

        // Jogador 1: setas
        drawText(bg, "JOGADOR 1", 170, 156, 14, P1_COLOR, ALIGN_CENTER);
        drawKey(bg, 170, 186, "", ARROW_UP, P1_COLOR);
        drawKey(bg, 136, 220, "", ARROW_LEFT, P1_COLOR);
        drawKey(bg, 170, 220, "", ARROW_DOWN, P1_COLOR);
        drawKey(bg, 204, 220, "", ARROW_RIGHT, P1_COLOR);

        // Jogador 2: WASD
        drawText(bg, "JOGADOR 2", 470, 156, 14, P2_COLOR, ALIGN_CENTER);
        drawKey(bg, 470, 186, "W", ARROW_NONE, P2_COLOR);
        drawKey(bg, 436, 220, "A", ARROW_NONE, P2_COLOR);
        drawKey(bg, 470, 220, "S", ARROW_NONE, P2_COLOR);
        drawKey(bg, 504, 220, "D", ARROW_NONE, P2_COLOR);

        // Pausa
        drawText(bg, "PAUSAR", 320, 156, 14, Color.WHITE, ALIGN_CENTER);
        drawKey(bg, 320, 186, "P", ARROW_NONE, Color.WHITE);
        drawText(bg, "ou ESC", 320, 212, 11, TEXT_DIM, ALIGN_CENTER);

        drawText(bg, "Contra a IA, você joga com as setas (Jogador 1).", 320, 252, 12, TEXT_DIM, ALIGN_CENTER);

        // Legenda dos power-ups
        drawText(bg, "POWER-UPS", 320, 280, 14, NEON_YELLOW, ALIGN_CENTER);
        PowerUp.Type[] types = PowerUp.Type.values();
        String[] names = {"VELOCIDADE", "ESCUDO", "LENTIDÃO"};
        String[] descs = {
            "Impulso de velocidade para quem pegar",
            "Atravessa rastros e o adversário por alguns segundos",
            "Reduz a velocidade do adversário"
        };
        for (int i = 0; i < types.length; i++) {
            int cy = 316 + i * 40;
            Color c = PowerUp.colorOf(types[i]);
            bg.setColor(c);
            bg.fillOval(100, cy - 11, 22, 22);
            bg.setColor(Color.WHITE);
            bg.drawOval(100, cy - 11, 21, 21);
            drawText(bg, PowerUp.symbolOf(types[i]), 111, cy, 11, Color.WHITE, ALIGN_CENTER);
            drawText(bg, names[i], 140, cy - 8, 14, c, ALIGN_LEFT);
            drawText(bg, descs[i], 140, cy + 9, 12, TEXT_DIM, ALIGN_LEFT);
        }

        drawText(bg, "ENTER ou ESC: voltar", 320, 436, 12, TEXT_DARK, ALIGN_CENTER);
    }

    // =====================================================================
    //  Desenho: cartazes (contagem, pausa, fim de rodada / partida)
    // =====================================================================

    private GreenfootImage buildCountdownBanner() {
        boolean go = countdownNumber <= 0;
        Color accent = go ? NEON_GREEN : NEON_CYAN;

        GreenfootImage img = new GreenfootImage(150, 110);
        drawPanel(img, 4, 4, 142, 102, accent);
        drawGlowText(img, go ? "VAI!" : String.valueOf(countdownNumber), 75, 55, go ? 44 : 64,
                     go ? NEON_GREEN : Color.WHITE, ALIGN_CENTER);
        return img;
    }

    private GreenfootImage buildPauseBanner() {
        GreenfootImage img = new GreenfootImage(340, 130);
        drawPanel(img, 4, 4, 332, 122, NEON_YELLOW);
        drawGlowText(img, "PAUSADO", 170, 38, 30, NEON_YELLOW, ALIGN_CENTER);
        drawText(img, "P, ESC ou ENTER: continuar", 170, 78, 14, Color.WHITE, ALIGN_CENTER);
        drawText(img, "M: menu principal", 170, 102, 12, TEXT_DIM, ALIGN_CENTER);
        return img;
    }

    private Color accentFor(Result result) {
        if (result == Result.P1) return P1_COLOR;
        if (result == Result.P2) return P2_COLOR;
        return new Color(220, 220, 240);
    }

    private GreenfootImage buildRoundOverBanner() {
        Color accent = accentFor(lastResult);
        String title = (lastResult == Result.EMPATE)
                ? "EMPATE!"
                : nameOf(lastResult == Result.P1) + " VENCEU A RODADA!";

        GreenfootImage img = new GreenfootImage(460, 150);
        drawPanel(img, 4, 4, 452, 142, accent);
        drawGlowText(img, title, 230, 40, 24, accent, ALIGN_CENTER);
        drawText(img, "PLACAR   " + scoreP1 + "  x  " + scoreP2, 230, 80, 18, Color.WHITE, ALIGN_CENTER);
        drawText(img, "ENTER ou ESPAÇO: próxima rodada", 230, 112, 13, TEXT_DIM, ALIGN_CENTER);
        drawText(img, "M: menu principal", 230, 132, 11, TEXT_DARK, ALIGN_CENTER);
        return img;
    }

    private GreenfootImage buildMatchOverBanner() {
        boolean p1Won = scoreP1 > scoreP2;
        Color accent = p1Won ? P1_COLOR : P2_COLOR;

        GreenfootImage img = new GreenfootImage(480, 170);
        drawPanel(img, 4, 4, 472, 162, accent);
        drawGlowText(img, nameOf(p1Won) + " VENCEU A PARTIDA!", 240, 40, 22, accent, ALIGN_CENTER);
        drawText(img, "PLACAR FINAL   " + scoreP1 + "  x  " + scoreP2, 240, 80, 18, Color.WHITE, ALIGN_CENTER);
        drawText(img, "ENTER ou ESPAÇO: nova partida", 240, 116, 14, Color.WHITE, ALIGN_CENTER);
        drawText(img, "M: menu principal", 240, 140, 12, TEXT_DIM, ALIGN_CENTER);
        return img;
    }

    private void showBanner(GreenfootImage img) {
        banner.setImage(img);
        if (!bannerShown) {
            addObject(banner, WORLD_W / 2, (ARENA_TOP + ARENA_BOTTOM) / 2);
            bannerShown = true;
        }
    }

    private void clearBanner() {
        if (bannerShown) {
            removeObject(banner);
            bannerShown = false;
        }
    }

    // =====================================================================
    //  Ferramentas de desenho
    // =====================================================================

    // Texto ancorado em (x, y), onde y é o centro vertical do texto
    private void drawText(GreenfootImage target, String text, int x, int y, int size, Color color, int align) {
        GreenfootImage img = new GreenfootImage(text, size, color, new Color(0, 0, 0, 0));
        int w = img.getWidth();
        int h = img.getHeight();
        int left = (align == ALIGN_LEFT) ? x : (align == ALIGN_RIGHT ? x - w : x - w / 2);
        target.drawImage(img, left, y - h / 2);
    }

    // Texto com "brilho" neon (cópias translúcidas deslocadas por baixo do texto)
    private void drawGlowText(GreenfootImage target, String text, int x, int y, int size, Color color, int align) {
        Color glow = new Color(color.getRed(), color.getGreen(), color.getBlue(), 70);
        drawText(target, text, x - 2, y, size, glow, align);
        drawText(target, text, x + 2, y, size, glow, align);
        drawText(target, text, x, y - 2, size, glow, align);
        drawText(target, text, x, y + 2, size, glow, align);
        drawText(target, text, x, y, size, color, align);
    }

    // Painel escuro semitransparente com borda neon e "cantoneiras"
    private void drawPanel(GreenfootImage target, int left, int top, int w, int h, Color accent) {
        target.setColor(new Color(2, 6, 14, 220));
        target.fillRect(left, top, w, h);

        target.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
        target.drawRect(left - 2, top - 2, w + 3, h + 3);

        target.setColor(accent);
        target.drawRect(left, top, w - 1, h - 1);

        int c = 16;
        target.fillRect(left - 2, top - 2, c, 3);
        target.fillRect(left - 2, top - 2, 3, c);
        target.fillRect(left + w - c + 1, top - 2, c, 3);
        target.fillRect(left + w - 1, top - 2, 3, c);
        target.fillRect(left - 2, top + h - 1, c, 3);
        target.fillRect(left - 2, top + h - c + 1, 3, c);
        target.fillRect(left + w - c + 1, top + h - 1, c, 3);
        target.fillRect(left + w - 1, top + h - c + 1, 3, c);
    }

    // Botão de menu: rótulo + descrição; o selecionado ganha destaque e uma seta
    private void drawButton(GreenfootImage target, int cx, int cy, int w, int h,
                            String label, String desc, boolean selected, Color accent) {
        int left = cx - w / 2;
        int top = cy - h / 2;

        if (selected) {
            target.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));
            target.fillRect(left, top, w, h);
            target.setColor(accent);
            target.drawRect(left, top, w - 1, h - 1);
            target.drawRect(left + 1, top + 1, w - 3, h - 3);
            target.fillPolygon(new int[]{left + 10, left + 10, left + 19},
                               new int[]{cy - 7, cy + 7, cy}, 3);
        } else {
            target.setColor(new Color(255, 255, 255, 10));
            target.fillRect(left, top, w, h);
            target.setColor(new Color(70, 82, 105));
            target.drawRect(left, top, w - 1, h - 1);
        }

        drawText(target, label, cx, top + 16, 18, selected ? Color.WHITE : SILVER, ALIGN_CENTER);
        drawText(target, desc, cx, top + h - 13, 12, selected ? accent : TEXT_DARK, ALIGN_CENTER);
    }

    // Tecla desenhada (com letra ou com seta)
    private void drawKey(GreenfootImage target, int cx, int cy, String label, int arrow, Color color) {
        target.setColor(new Color(255, 255, 255, 14));
        target.fillRect(cx - 15, cy - 15, 30, 30);
        target.setColor(color);
        target.drawRect(cx - 15, cy - 15, 29, 29);

        switch (arrow) {
            case ARROW_UP:
                target.fillPolygon(new int[]{cx - 6, cx + 6, cx}, new int[]{cy + 4, cy + 4, cy - 6}, 3);
                break;
            case ARROW_DOWN:
                target.fillPolygon(new int[]{cx - 6, cx + 6, cx}, new int[]{cy - 4, cy - 4, cy + 6}, 3);
                break;
            case ARROW_LEFT:
                target.fillPolygon(new int[]{cx + 4, cx + 4, cx - 6}, new int[]{cy - 6, cy + 6, cy}, 3);
                break;
            case ARROW_RIGHT:
                target.fillPolygon(new int[]{cx - 4, cx - 4, cx + 6}, new int[]{cy - 6, cy + 6, cy}, 3);
                break;
            default:
                drawText(target, label, cx, cy, 16, color, ALIGN_CENTER);
                break;
        }
    }

    // =====================================================================
    //  Som, com proteção caso o arquivo ainda não exista no projeto
    // =====================================================================

    private GreenfootSound loadSound(String filename) {
        try {
            return new GreenfootSound(filename);
        } catch (Exception e) {
            return null;
        }
    }

    private void playSafely(GreenfootSound sound) {
        if (sound == null) return;
        try {
            sound.play();
        } catch (Exception e) {
            // arquivo de som ausente ou inválido - ignora silenciosamente
        }
    }
}