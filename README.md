# Pedra, Papel e Tesoura com Java RMI

Jogo para 2 jogadores remotos. O servidor RMI é o árbitro; cada cliente tem uma GUI Swing.
Requer JDK 14+ (usa `switch` com setas).

## Compilar
```
javac -d out src/jogo/*.java
```

## Executar
```
java -cp out jogo.JogoServidor [porta=1099] [ip-publico]
java -cp out jogo.JogoCliente [host=localhost] [porta=1099]   # rode duas vezes
```

Entre máquinas diferentes: passe o IP do servidor como 2º argumento do servidor e libere
as portas TCP 1099 (registry) e 1100 (objeto remoto).

## Regras
Pedra > Tesoura > Papel > Pedra. Uma nova rodada começa quando os dois jogadores clicam em "Nova rodada".
