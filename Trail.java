import greenfoot.*;

/**
 * Um segmento do rastro deixado por um jogador.
 *
 * Guarda quem criou o segmento e em que momento (timer do dono), para que
 * o próprio jogador não morra ao tocar no rastro que acabou de criar, mas
 * continue morrendo ao tocar no rastro do adversário desde o primeiro instante.
 *
 * SIZE precisa ser MAIOR que a velocidade máxima do jogador (6 + 3 do power-up = 9),
 * senão ficam "buracos" no rastro e dá para atravessá-lo.
 *
 * A imagem é criada uma única vez por jogador e compartilhada por todos os
 * segmentos (muito mais leve do que criar uma imagem por segmento).
 */
public class Trail extends Actor {
    public static final int SIZE = 10;

    private final Player owner;
    private final int createdAt;

    public Trail(GreenfootImage image, Player owner, int createdAt) {
        this.owner = owner;
        this.createdAt = createdAt;
        setImage(image);
    }

    public Player getOwner() {
        return owner;
    }

    public int getCreatedAt() {
        return createdAt;
    }

    /**
     * Cria a imagem do segmento: faixa escura nas bordas, cor viva no meio e uma
     * linha clara no centro (efeito neon). A linha clara acompanha o sentido
     * do movimento, por isso existem a versão horizontal e a vertical.
     */
    public static GreenfootImage createImage(Color color, boolean horizontal) {
        GreenfootImage img = new GreenfootImage(SIZE, SIZE);

        Color outer = new Color(color.getRed() * 55 / 100,
                                color.getGreen() * 55 / 100,
                                color.getBlue() * 55 / 100);
        Color core = new Color(Math.min(255, color.getRed() + 110),
                               Math.min(255, color.getGreen() + 110),
                               Math.min(255, color.getBlue() + 110));

        img.setColor(outer);
        img.fillRect(0, 0, SIZE, SIZE);

        img.setColor(color);
        if (horizontal) {
            img.fillRect(0, 2, SIZE, SIZE - 4);
            img.setColor(core);
            img.fillRect(0, SIZE / 2 - 1, SIZE, 2);
        } else {
            img.fillRect(2, 0, SIZE - 4, SIZE);
            img.setColor(core);
            img.fillRect(SIZE / 2 - 1, 0, 2, SIZE);
        }
        return img;
    }
}