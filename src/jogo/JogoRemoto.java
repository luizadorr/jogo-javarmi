package jogo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JogoRemoto extends Remote {
    String NOME = "PedraPapelTesoura";

    /** Entra na partida e devolve o id do jogador (1 ou 2). Lança IllegalStateException se cheia. */
    int entrar() throws RemoteException;

    /** Registra a jogada na rodada atual. */
    void jogar(int id, Jogada jogada) throws RemoteException;

    /** Consulta o estado atual (usado em polling). */
    EstadoRodada consultar(int id) throws RemoteException;

    /** Pede nova rodada; ela começa quando ambos pedirem (ou se o adversário saiu). */
    void novaRodada(int id) throws RemoteException;

    /** Sai da partida, liberando a vaga. */
    void sair(int id) throws RemoteException;
}
