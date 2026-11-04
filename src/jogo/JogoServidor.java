package jogo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Árbitro do jogo. Uso: java jogo.JogoServidor [porta-registry=1099] [hostname-publico]
 */
public class JogoServidor implements JogoRemoto {
    private static final int PORTA_OBJETO = 1100;

    private final boolean[] conectado = new boolean[3];
    private final Jogada[] jogada = new Jogada[3];
    private final boolean[] pediuNova = new boolean[3];
    private final int[] vitorias = new int[3];
    private int empates = 0;
    private boolean finalizada = false;
    private Resultado resultadoJ1; // resultado do ponto de vista do jogador 1

    private static int outro(int id) {
        return id == 1 ? 2 : 1;
    }

    private void validar(int id) {
        if (id < 1 || id > 2 || !conectado[id]) {
            throw new IllegalStateException("Jogador inválido ou desconectado: " + id);
        }
    }

    private void reiniciarRodada() {
        jogada[1] = jogada[2] = null;
        pediuNova[1] = pediuNova[2] = false;
        finalizada = false;
        resultadoJ1 = null;
    }

    @Override
    public synchronized int entrar() {
        for (int id = 1; id <= 2; id++) {
            if (!conectado[id]) {
                conectado[id] = true;
                vitorias[id] = 0;
                empates = 0;
                vitorias[outro(id)] = 0;
                reiniciarRodada();
                System.out.println("Jogador " + id + " entrou.");
                return id;
            }
        }
        throw new IllegalStateException("A partida já está cheia.");
    }

    @Override
    public synchronized void jogar(int id, Jogada j) {
        validar(id);
        if (j == null) throw new IllegalArgumentException("Jogada nula.");
        if (!conectado[outro(id)]) throw new IllegalStateException("Aguardando adversário.");
        if (finalizada || jogada[id] != null) throw new IllegalStateException("Você já jogou nesta rodada.");
        jogada[id] = j;
        System.out.println("Jogador " + id + " jogou.");
        if (jogada[1] != null && jogada[2] != null) {
            finalizada = true;
            if (jogada[1] == jogada[2]) {
                resultadoJ1 = Resultado.EMPATE;
                empates++;
            } else if (jogada[1].venceDe(jogada[2])) {
                resultadoJ1 = Resultado.VITORIA;
                vitorias[1]++;
            } else {
                resultadoJ1 = Resultado.DERROTA;
                vitorias[2]++;
            }
            System.out.println("Rodada: " + jogada[1] + " x " + jogada[2] + " -> J1 " + resultadoJ1);
        }
    }

    @Override
    public synchronized EstadoRodada consultar(int id) {
        validar(id);
        int adv = outro(id);
        Resultado r = null;
        Jogada jAdv = null;
        if (finalizada) {
            jAdv = jogada[adv];
            if (resultadoJ1 == Resultado.EMPATE) r = Resultado.EMPATE;
            else if ((id == 1) == (resultadoJ1 == Resultado.VITORIA)) r = Resultado.VITORIA;
            else r = Resultado.DERROTA;
        }
        return new EstadoRodada(conectado[adv], jogada[id] != null, finalizada, r, jAdv,
                vitorias[id], vitorias[adv], empates);
    }

    @Override
    public synchronized void novaRodada(int id) {
        validar(id);
        if (!finalizada) return;
        pediuNova[id] = true;
        if (!conectado[outro(id)] || pediuNova[outro(id)]) {
            reiniciarRodada();
        }
    }

    @Override
    public synchronized void sair(int id) {
        if (id < 1 || id > 2 || !conectado[id]) return;
        conectado[id] = false;
        vitorias[1] = vitorias[2] = 0;
        empates = 0;
        reiniciarRodada();
        System.out.println("Jogador " + id + " saiu.");
    }

    public static void main(String[] args) throws RemoteException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;
        if (args.length > 1) System.setProperty("java.rmi.server.hostname", args[1]);

        JogoServidor servidor = new JogoServidor();
        JogoRemoto stub = (JogoRemoto) UnicastRemoteObject.exportObject(servidor, PORTA_OBJETO);
        Registry registry = LocateRegistry.createRegistry(porta);
        registry.rebind(NOME, stub);
        System.out.println("Servidor pronto (registry " + porta + ", objeto " + PORTA_OBJETO + ").");
    }
}
