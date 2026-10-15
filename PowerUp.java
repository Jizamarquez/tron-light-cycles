import greenfoot.*;

/**
 * Item que aparece ocasionalmente na arena. Ao ser tocado por um
 * jogador, aplica um efeito temporário:
 *  - VELOCIDADE: impulso de velocidade pra quem coletou
 *  - ESCUDO: imunidade temporária a colisões com rastros e com o adversário
 *  - LENTIDAO: reduz a velocidade do ADVERSÁRIO de quem coletou
 *
 * Some sozinho da tela depois de um tempo (piscando no final), se ninguém
 * coletar. Fica congelado durante pausa e contagem regressiva.
 */
public class PowerUp extends Actor {

    public enum Type { VELOCIDADE, ESCUDO, LENTIDAO }

    private static final int LIFETIME = 360; // ~6s a 60fps, se não for coletado
    private static final int BLINK_START = 90; // últimos frames: pisca avisando que vai sumir
    private static final int SIZE = 16;

    private final Type type;
    private final GreenfootImage imageOn;  // halo mais forte
    private final GreenfootImage imageOff; // halo mais fraco
    private int lifeLeft = LIFETIME;
    private int pulseTimer = 0;

    public PowerUp(Type type) {
        this.type = type;
        this.imageOn = buildImage(type, true);
        this.imageOff = buildImage(type, false);
        setImage(imageOn);
    }

    public Type getType() {
        return type;
    }

    public void act() {
        TronWorld world = (TronWorld) getWorld();
        if (world == null || !world.isPlaying()) return;

        lifeLeft--;
        pulseTimer++;
        if (lifeLeft <= 0) {
            world.removeObject(this);
            return;
        }

        GreenfootImage img = ((pulseTimer / 10) % 2 == 0) ? imageOn : imageOff;
        boolean hidden = lifeLeft < BLINK_START && (lifeLeft / 6) % 2 == 1;
        img.setTransparency(hidden ? 90 : 255);
        setImage(img);
    }

    // ---------- Aparência (também usada pela tela "Como jogar") ----------

    public static Color colorOf(Type type) {
        switch (type) {
            case VELOCIDADE: return new Color(50, 255, 100);
            case ESCUDO:     return new Color(120, 180, 255);
            case LENTIDAO:   return new Color(255, 60, 200);
            default:         return Color.WHITE;
        }
    }

    public static String symbolOf(Type type) {
        switch (type) {
            case VELOCIDADE: return ">>";
            case ESCUDO:     return "O";
            case LENTIDAO:   return "Z";
            default:         return "?";
        }
    }

    // Halo neon (mais forte ou mais fraco, pra dar o efeito de pulsar) + símbolo
    private static GreenfootImage buildImage(Type type, boolean pulseOn) {
        Color color = colorOf(type);
        int total = SIZE + 8;

        GreenfootImage image = new GreenfootImage(total, total);

        for (int i = 4; i >= 1; i--) {
            int alpha = (pulseOn ? 26 : 14) * i;
            int size = SIZE + i * 2;
            int offset = (total - size) / 2;
            image.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            image.fillOval(offset, offset, size, size);
        }

        int offset = (total - SIZE) / 2;
        image.setColor(color);
        image.fillOval(offset, offset, SIZE, SIZE);
        image.setColor(Color.WHITE);
        image.drawOval(offset, offset, SIZE - 1, SIZE - 1);

        image.setFont(new Font("SansSerif", true, false, 12));
        image.drawString(symbolOf(type), total / 2 - 6, total / 2 + 5);

        return image;
    }
}