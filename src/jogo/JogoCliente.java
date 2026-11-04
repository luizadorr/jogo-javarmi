package jogo;

import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cliente Swing. Uso: java jogo.JogoCliente [host=localhost] [porta=1099] */
public class JogoCliente extends JFrame {
    private final JogoRemoto jogo;
    private final int id;

    private final JLabel status = new JLabel("Conectado.", SwingConstants.CENTER);
    private final JLabel placar = new JLabel("", SwingConstants.CENTER);
    private final JButton[] botoes = new JButton[Jogada.values().length];
    private final JButton novaRodada = new JButton("Nova rodada");
    private final AtomicBoolean consultando = new AtomicBoolean(false);
    private volatile boolean pediuNova = false;

    public JogoCliente(JogoRemoto jogo, int id) {
        super("Pedra, Papel e Tesoura - Jogador " + id);
        this.jogo = jogo;
        this.id = id;

        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        JPanel topo = new JPanel(new GridLayout(2, 1, 0, 4));
        topo.add(status);
        topo.add(placar);

        JPanel centro = new JPanel(new GridLayout(1, 3, 8, 0));
        for (Jogada j : Jogada.values()) {
            JButton b = new JButton(j.name());
            b.addActionListener(e -> jogar(j));
            botoes[j.ordinal()] = b;
            centro.add(b);
        }

        novaRodada.addActionListener(e -> pedirNovaRodada());
        novaRodada.setEnabled(false);

        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        raiz.add(topo, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(novaRodada, BorderLayout.SOUTH);
        setContentPane(raiz);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { sairEFechar(); }
        });
        setSize(480, 240);
        setLocationRelativeTo(null);

        atualizarBotoes(false);
        new Timer(500, e -> consultar()).start();
    }

    private void atualizarBotoes(boolean habilitar) {
        for (JButton b : botoes) b.setEnabled(habilitar);
    }

    private void jogar(Jogada j) {
        atualizarBotoes(false);
        executar(() -> jogo.jogar(id, j), "Você jogou " + j + ". Aguardando adversário...");
    }

    private void pedirNovaRodada() {
        novaRodada.setEnabled(false);
        pediuNova = true;
        executar(() -> jogo.novaRodada(id), "Aguardando o adversário aceitar nova rodada...");
    }

    private interface Chamada { void run() throws Exception; }

    private void executar(Chamada c, String msgOk) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { c.run(); return null; }
            @Override protected void done() {
                try { get(); status.setText(msgOk); }
                catch (Exception ex) { erro(ex); }
            }
        }.execute();
    }

    private void consultar() {
        if (!consultando.compareAndSet(false, true)) return;
        new SwingWorker<EstadoRodada, Void>() {
            @Override protected EstadoRodada doInBackground() throws Exception { return jogo.consultar(id); }
            @Override protected void done() {
                try { aplicar(get()); }
                catch (Exception ex) { erro(ex); }
                finally { consultando.set(false); }
            }
        }.execute();
    }

    private void aplicar(EstadoRodada s) {
        placar.setText("Vitórias: " + s.vitorias + "   Derrotas: " + s.derrotas + "   Empates: " + s.empates);
        if (!s.adversarioConectado) {
            status.setText("Aguardando adversário entrar...");
            atualizarBotoes(false);
            novaRodada.setEnabled(false);
        } else if (s.rodadaFinalizada) {
            atualizarBotoes(false);
            String txt = switch (s.resultado) {
                case VITORIA -> "Você VENCEU!";
                case DERROTA -> "Você perdeu.";
                case EMPATE -> "Empate.";
            };
            status.setText(txt + " Adversário jogou " + s.jogadaAdversario + ".");
            novaRodada.setEnabled(!pediuNova);
        } else {
            pediuNova = false;
            novaRodada.setEnabled(false);
            if (s.euJoguei) {
                atualizarBotoes(false);
                status.setText("Aguardando adversário jogar...");
            } else {
                atualizarBotoes(true);
                status.setText("Sua vez: escolha uma jogada.");
            }
        }
    }

    private void erro(Exception ex) {
        Throwable t = ex.getCause() != null ? ex.getCause() : ex;
        status.setText("Erro: " + t.getMessage());
        atualizarBotoes(false);
    }

    private void sairEFechar() {
        try { jogo.sair(id); } catch (Exception ignorado) { }
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : Registry.REGISTRY_PORT;
        try {
            Registry registry = LocateRegistry.getRegistry(host, porta);
            JogoRemoto jogo = (JogoRemoto) registry.lookup(JogoRemoto.NOME);
            int id = jogo.entrar();
            SwingUtilities.invokeLater(() -> new JogoCliente(jogo, id).setVisible(true));
        } catch (Exception e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            JOptionPane.showMessageDialog(null, "Não foi possível entrar: " + t.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
