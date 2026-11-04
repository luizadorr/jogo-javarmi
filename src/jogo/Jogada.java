package jogo;

public enum Jogada {
    PEDRA, PAPEL, TESOURA;

    /** Pedra > Tesoura > Papel > Pedra. */
    public boolean venceDe(Jogada outra) {
        return (this == PEDRA && outra == TESOURA)
                || (this == TESOURA && outra == PAPEL)
                || (this == PAPEL && outra == PEDRA);
    }
}
