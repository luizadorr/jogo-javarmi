
const API = "http://localhost:8080";

const mensagem = document.getElementById("mensagem");
const botoes = document.querySelectorAll(".opcao");
const botaoNovaRodada = document.getElementById("novaRodada");

let idJogador = null;
let consultando = false;
let intervaloConsulta = null;

async function requisicao(endpoint) {
    const resposta = await fetch(`${API}${endpoint}`);
    const dados = await resposta.json();

    if (!resposta.ok || dados.erro) {
        throw new Error(dados.erro || "Erro na comunicação com o servidor.");
    }

    return dados;
}

async function entrar() {
    try {
        const dados = await requisicao("/entrar");

        idJogador = dados.id;

        mensagem.textContent =
            `Você entrou como Jogador ${idJogador}. Aguardando adversário...`;

        consultarEstado();

        intervaloConsulta = setInterval(consultarEstado, 1000);

    } catch (erro) {
        mensagem.textContent = erro.message;
    }
}

async function jogar(jogada) {
    if (idJogador === null) return;

    try {
        await requisicao(
            `/jogar?id=${idJogador}&jogada=${jogada}`
        );

        botoes.forEach(botao => botao.disabled = true);

        mensagem.textContent =
            "Jogada enviada! Aguardando o adversário...";

        consultarEstado();

    } catch (erro) {
        mensagem.textContent = erro.message;
    }
}

async function consultarEstado() {
    if (idJogador === null || consultando) return;

    consultando = true;

    try {
        const estado = await requisicao(
            `/consultar?id=${idJogador}`
        );

        document.getElementById("vitorias").textContent =
            estado.vitorias;

        document.getElementById("derrotas").textContent =
            estado.derrotas;

        document.getElementById("empates").textContent =
            estado.empates;

        if (!estado.adversarioConectado) {
            mensagem.textContent =
                "Aguardando o segundo jogador entrar...";

        } else if (!estado.rodadaFinalizada) {

            if (estado.euJoguei) {
                mensagem.textContent =
                    "Jogada enviada! Aguardando o adversário...";
            } else {
                mensagem.textContent =
                    "Adversário conectado! Escolha sua jogada.";

                botoes.forEach(botao => botao.disabled = false);
            }

        } else {
            const adversario = estado.jogadaAdversario
                ? estado.jogadaAdversario.toLowerCase()
                : "";

            if (estado.resultado === "VITORIA") {
                mensagem.textContent =
                    `Você venceu! O adversário escolheu ${adversario}.`;

            } else if (estado.resultado === "DERROTA") {
                mensagem.textContent =
                    `Você perdeu! O adversário escolheu ${adversario}.`;

            } else {
                mensagem.textContent =
                    `Empate! Os dois escolheram ${adversario}.`;
            }
        }

    } catch (erro) {
        mensagem.textContent = erro.message;
    } finally {
        consultando = false;
    }
}

botoes.forEach(botao => {
    botao.disabled = true;

    botao.addEventListener("click", () => {
        jogar(botao.id.toUpperCase());
    });
});

botaoNovaRodada.addEventListener("click", async () => {
    if (idJogador === null) return;

    try {
        await requisicao(`/nova-rodada?id=${idJogador}`);

        botoes.forEach(botao => botao.disabled = true);

        mensagem.textContent =
            "Solicitação de nova rodada enviada. Aguardando o adversário...";

        consultarEstado();

    } catch (erro) {
        mensagem.textContent = erro.message;
    }
});


entrar();

window.addEventListener("beforeunload", () => {
    if (idJogador !== null) {
        fetch(`${API}/sair?id=${idJogador}`, {
            keepalive: true
        });
    }

    clearInterval(intervaloConsulta);
});
