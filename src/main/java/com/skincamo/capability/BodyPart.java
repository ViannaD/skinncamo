package com.skincamo.capability;

/**
 * Partes do corpo que podem ser pintadas independentemente.
 * A ordem é usada no índice de serialização de rede, então NÃO REORDENE
 * sem atualizar os pacotes (ou use o nome em vez do ordinal se for alterar).
 */
public enum BodyPart {
    HEAD,
    BODY,
    RIGHT_ARM,
    LEFT_ARM,
    RIGHT_LEG,
    LEFT_LEG;

    public static final BodyPart[] VALUES = values();
}
