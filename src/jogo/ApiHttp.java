
package jogo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

public class ApiHttp {

    private static final int PORTA_HTTP = 8080;
    private static JogoRemoto jogo;

    public static void main(String[] args) throws Exception {

        Registry registro =
                LocateRegistry.getRegistry("localhost", 1099);

        jogo = (JogoRemoto) registro.lookup(JogoRemoto.NOME);

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress(PORTA_HTTP), 0
        );

        servidor.createContext("/entrar", ApiHttp::entrar);
        servidor.createContext("/jogar", ApiHttp::jogar);
        servidor.createContext("/consultar", ApiHttp::consultar);
        servidor.createContext("/nova-rodada", ApiHttp::novaRodada);
        servidor.createContext("/sair", ApiHttp::sair);

        servidor.setExecutor(null);
        servidor.start();

        System.out.println(
                "API HTTP iniciada na porta " + PORTA_HTTP
        );
    }

    // Lê os parâmetros enviados pela URL
    private static Map<String, String> parametros(
            HttpExchange exchange) throws IOException {

        Map<String, String> mapa = new HashMap<>();

        String consulta = exchange.getRequestURI().getRawQuery();

        if (consulta != null) {
            for (String parte : consulta.split("&")) {
                String[] item = parte.split("=", 2);

                if (item.length == 2) {
                    mapa.put(
                            URLDecoder.decode(item[0], "UTF-8"),
                            URLDecoder.decode(item[1], "UTF-8")
                    );
                }
            }
        }

        return mapa;
    }

    // Obtém o ID do jogador
    private static int obterId(Map<String, String> dados) {
        return Integer.parseInt(dados.get("id"));
    }

    // Prepara mensagens de erro
    private static String erro(Exception e) {

        String mensagem = e.getMessage();

        if (mensagem == null) {
            mensagem = "Erro na comunicação com o servidor";
        }

        mensagem = mensagem.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        return "{\"erro\":\"" + mensagem + "\"}";
    }

    // Envia a resposta HTTP
    private static void responder(
            HttpExchange exchange, String json) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "GET, OPTIONS"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type"
        );

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        byte[] resposta = json.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, resposta.length);

        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(resposta);
        }
    }

    // Registra um jogador
    private static void entrar(HttpExchange exchange)
            throws IOException {

        try {
            int id = jogo.entrar();

            responder(exchange, "{\"id\":" + id + "}");

        } catch (Exception e) {
            responder(exchange, erro(e));
        }
    }

    // Envia a jogada
    private static void jogar(HttpExchange exchange)
            throws IOException {

        try {
            Map<String, String> dados = parametros(exchange);

            int id = obterId(dados);

            String jogadaTexto = dados.get("jogada");

            Jogada jogada = Jogada.valueOf(
                    jogadaTexto.toUpperCase()
            );

            jogo.jogar(id, jogada);

            responder(exchange, "{\"sucesso\":true}");

        } catch (Exception e) {
            responder(exchange, erro(e));
        }
    }

    // Consulta o estado da partida
    private static void consultar(HttpExchange exchange)
            throws IOException {

        try {
            Map<String, String> dados = parametros(exchange);

            int id = obterId(dados);

            EstadoRodada estado = jogo.consultar(id);

            String resultado = estado.resultado == null
                    ? "null"
                    : "\"" + estado.resultado.name() + "\"";

            String jogadaAdversario =
                    estado.jogadaAdversario == null
                    ? "null"
                    : "\"" + estado.jogadaAdversario.name() + "\"";

            String json = "{"
                    + "\"adversarioConectado\":"
                    + estado.adversarioConectado + ","
                    + "\"euJoguei\":" + estado.euJoguei + ","
                    + "\"rodadaFinalizada\":"
                    + estado.rodadaFinalizada + ","
                    + "\"resultado\":" + resultado + ","
                    + "\"jogadaAdversario\":"
                    + jogadaAdversario + ","
                    + "\"vitorias\":" + estado.vitorias + ","
                    + "\"derrotas\":" + estado.derrotas + ","
                    + "\"empates\":" + estado.empates
                    + "}";

            responder(exchange, json);

        } catch (Exception e) {
            responder(exchange, erro(e));
        }
    }

    // Solicita uma nova rodada
    private static void novaRodada(HttpExchange exchange)
            throws IOException {

        try {
            Map<String, String> dados = parametros(exchange);

            int id = obterId(dados);

            jogo.novaRodada(id);

            responder(exchange, "{\"sucesso\":true}");

        } catch (Exception e) {
            responder(exchange, erro(e));
        }
    }

    // Desconecta o jogador
    private static void sair(HttpExchange exchange)
            throws IOException {

        try {
            Map<String, String> dados = parametros(exchange);

            int id = obterId(dados);

            jogo.sair(id);

            responder(exchange, "{\"sucesso\":true}");

        } catch (Exception e) {
            responder(exchange, erro(e));
        }
    }
}
