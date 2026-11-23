
// Elementos da tela
const mensagem = document.getElementById("mensagem");
const botoes = document.querySelectorAll(".opcao");
const botaoNovaRodada = document.getElementById("novaRodada");

// Pontuação
let vitorias = 0;
let derrotas = 0;
let empates = 0;

// Opções do jogo
const jogadas = ["pedra", "papel", "tesoura"];

// Recebe a jogada do jogador
botoes.forEach(botao => {
    botao.addEventListener("click", () => {
        const jogadaJogador = botao.id;

        // Simulação temporária do adversário
        const jogadaAdversario =
            jogadas[Math.floor(Math.random() * jogadas.length)];

        verificarResultado(jogadaJogador, jogadaAdversario);
    });
});

// Compara as jogadas
function verificarResultado(jogador, adversario) {

    if (jogador === adversario) {
        mensagem.textContent =
            `Empate! Os dois escolheram ${adversario}.`;
        empates++;

    } else if (
        (jogador === "pedra" && adversario === "tesoura") ||
        (jogador === "papel" && adversario === "pedra") ||
        (jogador === "tesoura" && adversario === "papel")
    ) {
        mensagem.textContent =
            `Você venceu! O adversário escolheu ${adversario}.`;
        vitorias++;

    } else {
        mensagem.textContent =
            `Você perdeu! O adversário escolheu ${adversario}.`;
        derrotas++;
    }

    atualizarPlacar();
}

// Atualiza os números do placar
function atualizarPlacar() {
    document.getElementById("vitorias").textContent = vitorias;
    document.getElementById("derrotas").textContent = derrotas;
    document.getElementById("empates").textContent = empates;
}

// Reinicia a rodada
botaoNovaRodada.addEventListener("click", () => {
    mensagem.textContent = "Aguardando sua jogada...";
});

// Estado inicial
atualizarPlacar();

