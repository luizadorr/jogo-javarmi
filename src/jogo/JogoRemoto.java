package jogo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JogoRemoto extends Remote {
    String NOME = "PedraPapelTesoura";

    int entrar() throws RemoteException;

    void jogar(int id, Jogada jogada) throws RemoteException;

    EstadoRodada consultar(int id) throws RemoteException;

    void novaRodada(int id) throws RemoteException;

    void sair(int id) throws RemoteException;
}
