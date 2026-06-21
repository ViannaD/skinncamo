package com.skincamo.client.brush;

import com.skincamo.capability.BodyPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * Geometria SIMPLIFICADA do modelo do jogador, usada só para o raycast do
 * "Modo Pincel 3D" (mirar e pintar diretamente no próprio corpo, em
 * terceira pessoa).
 *
 * SIMPLIFICAÇÕES DELIBERADAS (documentadas, não são bugs - são cortes de
 * escopo conscientes para reduzir risco num código que não pode ser
 * compilado/testado neste ambiente):
 *  - Ignora a animação de balanço de braços/pernas ao andar — assume pose
 *    "parado em pé". Pintar andando pode ficar levemente desalinhado;
 *    recomende ficar parado enquanto pinta.
 *  - Ignora a rotação independente da CABEÇA (olhar pra cima/baixo/lados
 *    sem virar o corpo todo) — a cabeça é tratada como se girasse junto
 *    com o corpo. Pra acertar a cabeça, gire o corpo todo (ande virando)
 *    em vez de só inclinar o olhar.
 *  - Não compensa poses especiais (nadando, dormindo, elytra, agachado).
 *
 * Replicar a animação completa do modelo (HumanoidModel#setupAnim) reduziria
 * essas limitações, mas aumentaria muito o risco de erro; é o ponto mais
 * fácil de melhorar numa próxima iteração, sem precisar tocar no resto do
 * sistema (rede, GUI, etc.).
 *
 * EIXOS LOCAIS DO CORPO ("right", "up", "forward"): um referencial
 * ortonormal alinhado com a rotação do CORPO do jogador (não da câmera).
 *  - "up"      = sempre (0,1,0) — não inclinamos o corpo, só giramos no eixo Y.
 *  - "forward" = direção que o corpo está virado (yaw do corpo, não da cabeça).
 *  - "right"   = forward × up (regra da mão direita).
 *
 * Todas as caixas abaixo foram derivadas das caixas padrão do HumanoidModel
 * vanilla (cabeça 8x8x8, tronco 8x12x4, braços/pernas 4x12x4, em unidades
 * de 1/16 de bloco), convertidas para blocos e re-centradas nesses eixos,
 * relativas à raiz do corpo (root).
 */
public final class PlayerBodyGeometry {

    private PlayerBodyGeometry() {}

    /**
     * ===== CALIBRAÇÃO #1 =====
     * Altura (em blocos) da raiz do modelo acima dos pés do jogador.
     * 24 unidades de modelo / 16 = 1.5 blocos (pernas ocupam 0 a 1.5 acima
     * dos pés; a raiz fica bem no topo das pernas / base do tronco).
     * Se o ponto pintado aparecer sistematicamente ACIMA ou ABAIXO de onde
     * você mirou, ajuste este valor primeiro.
     */
    public static final double ROOT_HEIGHT_ABOVE_FEET = 1.5;

    /**
     * ===== CALIBRAÇÃO #2 =====
     * Se braço/perna esquerdo e direito aparecerem TROCADOS ao testar no
     * jogo (ex.: pintar olhando pro seu braço direito pinta o esquerdo),
     * troque este valor de +1.0 para -1.0. É a única linha que precisa
     * mudar para corrigir uma inversão de lado.
     */
    public static final double RIGHT_AXIS_SIGN = 1.0;

    public static final class Box {
        public final double rMin, rMax, uMin, uMax, fMin, fMax;

        Box(double rMin, double rMax, double uMin, double uMax, double fMin, double fMax) {
            this.rMin = rMin;
            this.rMax = rMax;
            this.uMin = uMin;
            this.uMax = uMax;
            this.fMin = fMin;
            this.fMax = fMax;
        }
    }

    private static final Map<BodyPart, Box> BOXES = new EnumMap<>(BodyPart.class);

    static {
        // cabeça: 8x8x8, presa na raiz, sem rotação extra (ver simplificação acima)
        BOXES.put(BodyPart.HEAD, new Box(-0.25, 0.25, 0.0, 0.5, -0.25, 0.25));
        // tronco: 8x12x4, logo abaixo da raiz
        BOXES.put(BodyPart.BODY, new Box(-0.25, 0.25, -0.75, 0.0, -0.125, 0.125));
        // braços: 4x12x4, ombros na altura da raiz
        BOXES.put(BodyPart.RIGHT_ARM, new Box(0.25, 0.5, -0.75, 0.0, -0.125, 0.125));
        BOXES.put(BodyPart.LEFT_ARM, new Box(-0.5, -0.25, -0.75, 0.0, -0.125, 0.125));
        // pernas: 4x12x4, abaixo do tronco
        BOXES.put(BodyPart.RIGHT_LEG, new Box(-0.00625, 0.24375, -1.5, -0.75, -0.125, 0.125));
        BOXES.put(BodyPart.LEFT_LEG, new Box(-0.24375, 0.00625, -1.5, -0.75, -0.125, 0.125));
    }

    public static Box boxFor(BodyPart part) {
        return BOXES.get(part);
    }

    /** Posição (mundo) da raiz do corpo, interpolada para suavidade entre ticks. */
    public static Vec3 rootWorldPosition(AbstractClientPlayer player, float partialTick) {
        Vec3 feet = player.getPosition(partialTick);
        return feet.add(0.0, ROOT_HEIGHT_ABOVE_FEET, 0.0);
    }

    /** Direção "para frente" do CORPO (não da cabeça/câmera) - orienta a caixa do modelo. */
    public static Vec3 bodyForward(AbstractClientPlayer player, float partialTick) {
        float yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
        double yawRad = Math.toRadians(yaw);
        return new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
    }

    public static Vec3 worldUp() {
        return new Vec3(0.0, 1.0, 0.0);
    }

    /** "Direita" do corpo, perpendicular a forward e up (ver CALIBRAÇÃO #2 acima). */
    public static Vec3 bodyRight(Vec3 forward, Vec3 up) {
        Vec3 right = forward.cross(up).normalize();
        return RIGHT_AXIS_SIGN < 0 ? right.scale(-1.0) : right;
    }
}
