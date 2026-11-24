package jogo;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class ApiHttp {

    private static final int PORTA_HTTP = 8080;
    private static final String HOST_RMI = "localhost";
    private static final int PORTA_RMI = 1099;

    private static volatile JogoRemoto jogo;

    @FunctionalInterface
    private interface RotaHandler {
        String executar(Map<String, String> dados) throws Exception;
    }

    @FunctionalInterface
    private interface ChamadaRemota<T> {
        T executar(JogoRemoto stub) throws RemoteException;
    }

    public static void main(String[] args) throws Exception {
        try {
            conectarRmi();
        } catch (Exception e) {
            System.err.println("Aviso: servidor RMI ainda não disponível na inicialização. Tentará reconectar sob demanda.");
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA_HTTP), 0);

        servidor.createContext("/entrar", exchange -> tratarRota(exchange, ApiHttp::entrar));
        servidor.createContext("/jogar", exchange -> tratarRota(exchange, ApiHttp::jogar));
        servidor.createContext("/consultar", exchange -> tratarRota(exchange, ApiHttp::consultar));
        servidor.createContext("/nova-rodada", exchange -> tratarRota(exchange, ApiHttp::novaRodada));
        servidor.createContext("/sair", exchange -> tratarRota(exchange, ApiHttp::sair));

        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();

        System.out.println("API HTTP iniciada na porta " + PORTA_HTTP);
    }

    private static synchronized JogoRemoto conectarRmi() throws RemoteException, NotBoundException {
        Registry registro = LocateRegistry.getRegistry(HOST_RMI, PORTA_RMI);
        jogo = (JogoRemoto) registro.lookup(JogoRemoto.NOME);
        return jogo;
    }

    private static <T> T chamarRmi(ChamadaRemota<T> acao) throws Exception {
        JogoRemoto stub = jogo;
        if (stub == null) {
            stub = conectarRmi();
        }

        try {
            return acao.executar(stub);
        } catch (RemoteException e) {
            stub = conectarRmi();
            return acao.executar(stub);
        }
    }

    private static void tratarRota(HttpExchange exchange, RotaHandler handler) throws IOException {
        aplicarHeadersCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, "{\"erro\":\"Método não permitido. Use GET.\"}");
            return;
        }

        try {
            Map<String, String> dados = parametros(exchange);
            String jsonResposta = handler.executar(dados);
            responderJson(exchange, 200, jsonResposta);
        } catch (IllegalArgumentException e) {
            responderJson(exchange, 400, formatarErro(e.getMessage()));
        } catch (RemoteException | NotBoundException e) {
            responderJson(exchange, 502, formatarErro("Falha na comunicação com o servidor do jogo: " + e.getMessage()));
        } catch (Exception e) {
            responderJson(exchange, 500, formatarErro(e.getMessage()));
        }
    }

    private static void aplicarHeadersCors(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void responderJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] resposta = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, resposta.length);

        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(resposta);
        }
    }

    private static Map<String, String> parametros(HttpExchange exchange) {
        Map<String, String> mapa = new HashMap<>();
        String consulta = exchange.getRequestURI().getRawQuery();

        if (consulta != null && !consulta.isBlank()) {
            for (String parte : consulta.split("&")) {
                String[] item = parte.split("=", 2);
                if (item.length == 2) {
                    mapa.put(
                            URLDecoder.decode(item[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(item[1], StandardCharsets.UTF_8)
                    );
                }
            }
        }

        return mapa;
    }

    private static int obterId(Map<String, String> dados) {
        String idTexto = dados.get("id");
        if (idTexto == null || idTexto.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'id' é obrigatório.");
        }

        try {
            return Integer.parseInt(idTexto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O parâmetro 'id' deve ser um número inteiro válido.");
        }
    }

    private static String formatarErro(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            mensagem = "Erro interno no servidor.";
        }

        String escapada = mensagem
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", " ");

        return "{\"erro\":\"" + escapada + "\"}";
    }

    private static String entrar(Map<String, String> dados) throws Exception {
        int id = chamarRmi(JogoRemoto::entrar);
        return "{\"id\":" + id + "}";
    }

    private static String jogar(Map<String, String> dados) throws Exception {
        int id = obterId(dados);
        String jogadaTexto = dados.get("jogada");

        if (jogadaTexto == null || jogadaTexto.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'jogada' é obrigatório.");
        }

        Jogada jogada;
        try {
            jogada = Jogada.valueOf(jogadaTexto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Jogada inválida: " + jogadaTexto);
        }

        chamarRmi(stub -> {
            stub.jogar(id, jogada);
            return null;
        });

        return "{\"sucesso\":true}";
    }

    private static String consultar(Map<String, String> dados) throws Exception {
        int id = obterId(dados);
        EstadoRodada estado = chamarRmi(stub -> stub.consultar(id));

        String resultado = estado.resultado == null
                ? "null"
                : "\"" + estado.resultado.name() + "\"";

        String jogadaAdversario = estado.jogadaAdversario == null
                ? "null"
                : "\"" + estado.jogadaAdversario.name() + "\"";

        return "{"
                + "\"adversarioConectado\":" + estado.adversarioConectado + ","
                + "\"euJoguei\":" + estado.euJoguei + ","
                + "\"rodadaFinalizada\":" + estado.rodadaFinalizada + ","
                + "\"resultado\":" + resultado + ","
                + "\"jogadaAdversario\":" + jogadaAdversario + ","
                + "\"vitorias\":" + estado.vitorias + ","
                + "\"derrotas\":" + estado.derrotas + ","
                + "\"empates\":" + estado.empates
                + "}";
    }

    private static String novaRodada(Map<String, String> dados) throws Exception {
        int id = obterId(dados);
        chamarRmi(stub -> {
            stub.novaRodada(id);
            return null;
        });

        return "{\"sucesso\":true}";
    }

    private static String sair(Map<String, String> dados) throws Exception {
        int id = obterId(dados);
        chamarRmi(stub -> {
            stub.sair(id);
            return null;
        });

        return "{\"sucesso\":true}";
    }
}