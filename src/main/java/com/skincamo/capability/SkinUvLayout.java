package com.skincamo.capability;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Coordenadas UV do template padrão de skin 64x64, SEM nenhuma dependência
 * de classes gráficas (NativeImage etc.) — por isso vive no pacote
 * "capability" (código comum, que também roda no servidor dedicado, onde
 * classes de renderização não existem no classpath e dariam
 * ClassNotFoundException se fossem referenciadas aqui).
 *
 * Cada parte tem exatamente 6 retângulos na ordem:
 * [TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK] — essa ordem é usada tanto pelo
 * preenchimento de cor sólida (servidor) quanto pelo raycaster do "Modo
 * Pincel 3D" (cliente) para saber qual retângulo de UV corresponde a qual
 * face do cubo do modelo. NÃO REORDENE sem atualizar os dois usos.
 *
 * Retângulos no formato {x, y, largura, altura}.
 */
public final class SkinUvLayout {

    private SkinUvLayout() {}

    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int RIGHT = 2;
    public static final int FRONT = 3;
    public static final int LEFT = 4;
    public static final int BACK = 5;

    public static final Map<BodyPart, List<int[]>> BASE_LAYER = new EnumMap<>(BodyPart.class);

    public static final List<int[]> OVERLAY_LAYER = List.of(
            // Hat (cabeça)
            new int[]{32, 0, 8, 8}, new int[]{40, 0, 8, 8},
            new int[]{32, 8, 8, 8}, new int[]{40, 8, 8, 8}, new int[]{48, 8, 8, 8}, new int[]{56, 8, 8, 8},
            // Jacket (tronco)
            new int[]{20, 32, 8, 4}, new int[]{28, 32, 8, 4},
            new int[]{16, 36, 4, 12}, new int[]{20, 36, 8, 12}, new int[]{28, 36, 4, 12}, new int[]{32, 36, 8, 12},
            // Right sleeve (braço direito)
            new int[]{44, 32, 4, 4}, new int[]{48, 32, 4, 4},
            new int[]{40, 36, 4, 12}, new int[]{44, 36, 4, 12}, new int[]{48, 36, 4, 12}, new int[]{52, 36, 4, 12},
            // Left sleeve (braço esquerdo)
            new int[]{52, 48, 4, 4}, new int[]{56, 48, 4, 4},
            new int[]{48, 52, 4, 12}, new int[]{52, 52, 4, 12}, new int[]{56, 52, 4, 12}, new int[]{60, 52, 4, 12},
            // Right pant (perna direita)
            new int[]{4, 32, 4, 4}, new int[]{8, 32, 4, 4},
            new int[]{0, 36, 4, 12}, new int[]{4, 36, 4, 12}, new int[]{8, 36, 4, 12}, new int[]{12, 36, 4, 12},
            // Left pant (perna esquerda)
            new int[]{20, 48, 4, 4}, new int[]{24, 48, 4, 4},
            new int[]{16, 52, 4, 12}, new int[]{20, 52, 4, 12}, new int[]{24, 52, 4, 12}, new int[]{28, 52, 4, 12}
    );

    static {
        BASE_LAYER.put(BodyPart.HEAD, List.of(
                new int[]{8, 0, 8, 8}, new int[]{16, 0, 8, 8},
                new int[]{0, 8, 8, 8}, new int[]{8, 8, 8, 8}, new int[]{16, 8, 8, 8}, new int[]{24, 8, 8, 8}
        ));
        BASE_LAYER.put(BodyPart.BODY, List.of(
                new int[]{20, 16, 8, 4}, new int[]{28, 16, 8, 4},
                new int[]{16, 20, 4, 12}, new int[]{20, 20, 8, 12}, new int[]{28, 20, 4, 12}, new int[]{32, 20, 8, 12}
        ));
        BASE_LAYER.put(BodyPart.RIGHT_ARM, List.of(
                new int[]{44, 16, 4, 4}, new int[]{48, 16, 4, 4},
                new int[]{40, 20, 4, 12}, new int[]{44, 20, 4, 12}, new int[]{48, 20, 4, 12}, new int[]{52, 20, 4, 12}
        ));
        BASE_LAYER.put(BodyPart.RIGHT_LEG, List.of(
                new int[]{4, 16, 4, 4}, new int[]{8, 16, 4, 4},
                new int[]{0, 20, 4, 12}, new int[]{4, 20, 4, 12}, new int[]{8, 20, 4, 12}, new int[]{12, 20, 4, 12}
        ));
        BASE_LAYER.put(BodyPart.LEFT_LEG, List.of(
                new int[]{20, 48, 4, 4}, new int[]{24, 48, 4, 4},
                new int[]{16, 52, 4, 12}, new int[]{20, 52, 4, 12}, new int[]{24, 52, 4, 12}, new int[]{28, 52, 4, 12}
        ));
        BASE_LAYER.put(BodyPart.LEFT_ARM, List.of(
                new int[]{36, 48, 4, 4}, new int[]{40, 48, 4, 4},
                new int[]{32, 52, 4, 12}, new int[]{36, 52, 4, 12}, new int[]{40, 52, 4, 12}, new int[]{44, 52, 4, 12}
        ));
    }

    /** Retorna o retângulo {x,y,w,h} de uma face específica (TOP..BACK) de uma parte. */
    public static int[] faceRect(BodyPart part, int face) {
        return BASE_LAYER.get(part).get(face);
    }
}
