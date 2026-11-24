package jogo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class JogoServidor implements JogoRemoto {
    private static final int PORTA_OBJETO = 1100;
    private static final long TIMEOUT_MS = 20_000;

    private static Registry registry;

    private final boolean[] conectado = new boolean[3];
    private final long[] ultimaAtividade = new long[3];
    private final Jogada[] jogada = new Jogada[3];
    private final boolean[] pediuNova = new boolean[3];
    private final int[] vitorias = new int[3];
    private int empates = 0;
    private boolean finalizada = false;
    private Resultado resultadoJ1;

    private static int outro(int id) {
        return id == 1 ? 2 : 1;
    }

    private void verificarTimeout() {
        long agora = System.currentTimeMillis();
        for (int id = 1; id <= 2; id++) {
            if (conectado[id] && (agora - ultimaAtividade[id]) > TIMEOUT_MS) {
                System.out.println("Jogador " + id + " desconectado por inatividade.");
                sair(id);
            }
        }
    }

    private void validar(int id) {
        if (id < 1 || id > 2 || !conectado[id]) {
            throw new IllegalStateException("Jogador inválido ou desconectado: " + id);
        }
        ultimaAtividade[id] = System.currentTimeMillis();
    }

    private void reiniciarRodada() {
        jogada[1] = jogada[2] = null;
        pediuNova[1] = pediuNova[2] = false;
        finalizada = false;
        resultadoJ1 = null;
    }

    @Override
    public synchronized int entrar() {
        verificarTimeout();
        for (int id = 1; id <= 2; id++) {
            if (!conectado[id]) {
                conectado[id] = true;
                ultimaAtividade[id] = System.currentTimeMillis();
                vitorias[1] = 0;
                vitorias[2] = 0;
                empates = 0;
                reiniciarRodada();
                System.out.println("Jogador " + id + " entrou.");
                return id;
            }
        }
        throw new IllegalStateException("A partida já está cheia.");
    }

    @Override
    public synchronized void jogar(int id, Jogada j) {
        verificarTimeout();
        validar(id);
        if (j == null) {
            throw new IllegalArgumentException("Jogada nula.");
        }
        if (!conectado[outro(id)]) {
            throw new IllegalStateException("Aguardando adversário.");
        }
        if (finalizada || jogada[id] != null) {
            throw new IllegalStateException("Você já jogou nesta rodada.");
        }

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
        verificarTimeout();
        validar(id);
        int adv = outro(id);
        Resultado r = null;
        Jogada jAdv = null;

        if (finalizada) {
            jAdv = jogada[adv];
            if (resultadoJ1 == Resultado.EMPATE) {
                r = Resultado.EMPATE;
            } else if ((id == 1) == (resultadoJ1 == Resultado.VITORIA)) {
                r = Resultado.VITORIA;
            } else {
                r = Resultado.DERROTA;
            }
        }

        return new EstadoRodada(
                conectado[adv],
                jogada[id] != null,
                finalizada,
                r,
                jAdv,
                vitorias[id],
                vitorias[adv],
                empates
        );
    }

    @Override
    public synchronized void novaRodada(int id) {
        verificarTimeout();
        validar(id);
        if (!finalizada) {
            return;
        }
        pediuNova[id] = true;
        if (!conectado[outro(id)] || pediuNova[outro(id)]) {
            reiniciarRodada();
        }
    }

    @Override
    public synchronized void sair(int id) {
        if (id < 1 || id > 2 || !conectado[id]) {
            return;
        }
        conectado[id] = false;
        ultimaAtividade[id] = 0;
        vitorias[1] = 0;
        vitorias[2] = 0;
        empates = 0;
        reiniciarRodada();
        System.out.println("Jogador " + id + " saiu.");
    }

    public static void main(String[] args) throws RemoteException {
        int porta = Registry.REGISTRY_PORT;
        if (args.length > 0 && !args[0].isBlank()) {
            try {
                porta = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Porta inválida: " + args[0], e);
            }
        }

        if (args.length > 1 && !args[1].isBlank()) {
            System.setProperty("java.rmi.server.hostname", args[1].trim());
        }

        JogoServidor servidor = new JogoServidor();
        JogoRemoto stub = (JogoRemoto) UnicastRemoteObject.exportObject(servidor, PORTA_OBJETO);
        registry = LocateRegistry.createRegistry(porta);
        registry.rebind(NOME, stub);
        System.out.println("Servidor pronto (registry " + porta + ", objeto " + PORTA_OBJETO + ").");
    }
}