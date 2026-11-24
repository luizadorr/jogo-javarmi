package jogo;

import java.io.Serializable;

public class EstadoRodada implements Serializable {
    private static final long serialVersionUID = 1L;

    public final boolean adversarioConectado;
    public final boolean euJoguei;
    public final boolean rodadaFinalizada;
    public final Resultado resultado;          
    public final Jogada jogadaAdversario;      
    public final int vitorias;
    public final int derrotas;
    public final int empates;

    public EstadoRodada(boolean adversarioConectado, boolean euJoguei, boolean rodadaFinalizada,
                        Resultado resultado, Jogada jogadaAdversario,
                        int vitorias, int derrotas, int empates) {
        this.adversarioConectado = adversarioConectado;
        this.euJoguei = euJoguei;
        this.rodadaFinalizada = rodadaFinalizada;
        this.resultado = resultado;
        this.jogadaAdversario = jogadaAdversario;
        this.vitorias = vitorias;
        this.derrotas = derrotas;
        this.empates = empates;
    }
}
