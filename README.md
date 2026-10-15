# Tron: Ciclos de Luz

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Greenfoot](https://img.shields.io/badge/Greenfoot-3.x-2ea44f?style=flat)
![Status](https://img.shields.io/badge/status-em%20evolu%C3%A7%C3%A3o-00c8ff?style=flat)

Jogo de ciclos de luz inspirado no clássico Tron, feito em **Java** com o **Greenfoot**. Dois jogadores (ou um contra a IA) disputam a arena deixando um rastro de luz: quem encostar na parede ou em algum rastro perde a rodada. O último vivo marca o ponto.

> **English summary:** a Tron-inspired light-cycle game built in Java with Greenfoot, featuring local 2-player mode, an AI opponent with 3 difficulty levels, power-ups, a pause system and neon-style menus/HUD.

<img width="1609" height="926" alt="Captura de tela 2026-10-15 195246" src="https://github.com/user-attachments/assets/2e881e4d-92fa-4621-9aa2-20f85cacb09d" />


https://github.com/user-attachments/assets/2b79af0b-b83b-483b-86fd-a710a79e5ee9




https://github.com/user-attachments/assets/bebd4121-fd7e-41be-bf18-42ad9d629327






## Funcionalidades

- **Dois modos de jogo:** 2 jogadores no mesmo teclado ou 1 jogador contra a IA
- **IA com 3 dificuldades** (Fácil, Médio e Difícil)
- **Power-ups** que mudam o rumo da rodada (velocidade, escudo e lentidão)
- **Dificuldade progressiva:** a velocidade dos jogadores aumenta com o tempo
- **Menus navegáveis** (menu principal, escolha de dificuldade, opções e tela "Como jogar")
- **Opções configuráveis:** pontos para vencer (3, 5, 7 ou 10) e power-ups ligados/desligados
- **Pausa** durante a partida
- **Interface neon:** HUD com placar, arena com grade, rastros luminosos e cartazes de contagem, pausa e fim de rodada
- Sons de contagem, batida e vitória (opcionais: o jogo roda normalmente sem os arquivos de som)

## Como rodar

1. Instale o [Greenfoot](https://www.greenfoot.org/download) (ele já inclui o Java necessário).
2. Baixe este repositório (**Code → Download ZIP**) e extraia a pasta.
3. No Greenfoot, vá em **Scenario → Open** e escolha a pasta do projeto (a que contém o arquivo `project.greenfoot`).
4. Clique em **Compile All** e depois em **Run**.

## Controles

| Ação | Jogador 1 (azul) | Jogador 2 (laranja) |
|------|------------------|---------------------|
| Cima | ↑ | W |
| Baixo | ↓ | S |
| Esquerda | ← | A |
| Direita | → | D |

- **Menus:** setas (ou W/S) para navegar e **Enter/Espaço** para escolher. Os números também funcionam como atalho.
- **P** ou **Esc:** pausar durante a partida.
- Contra a IA, você joga como Jogador 1 (setas).

## Power-ups

| Power-up | Efeito |
|----------|--------|
| **Velocidade** (`>>`) | Impulso de velocidade para quem coletar |
| **Escudo** (`O`) | Atravessa rastros e o adversário por alguns segundos |
| **Lentidão** (`Z`) | Reduz a velocidade do adversário |

## Como a IA funciona

A IA não usa trapaça nem conhece o futuro: a cada intervalo de reação, ela **mede o espaço livre** à frente, à esquerda e à direita (até bater numa parede ou num rastro perigoso) e escolhe a direção com mais espaço. Para evitar que fique "tremendo" trocando de direção, a direção atual recebe um pequeno bônus.

As dificuldades mudam dois parâmetros:

| Dificuldade | Reação | Alcance de visão | Observação |
|-------------|--------|------------------|------------|
| Fácil | lenta | curto | comete erros ocasionais de propósito |
| Médio | média | médio | |
| Difícil | a cada frame | longo | |

## Estrutura do projeto

| Classe | Responsabilidade |
|--------|------------------|
| `TronWorld` | Mundo do jogo: menus, HUD, máquina de estados, resultado das rodadas e geração de power-ups |
| `Player` | Jogador: movimento, colisões, efeitos de power-up e aparência |
| `AIPlayer` | Jogador controlado por IA (herda de `Player`) |
| `PowerUp` | Itens coletáveis com três tipos de efeito |
| `Trail` | Segmento do rastro deixado pelos jogadores |

## Decisões técnicas

- **Máquina de estados** no `TronWorld` (menu, contagem, jogando, pausa, fim de rodada, fim de partida), o que deixa o fluxo do jogo organizado e fácil de estender.
- **Resolução de mortes centralizada:** os jogadores apenas *avisam* o mundo quando morrem, e o mundo decide o resultado de todos de uma vez. Assim, se os dois morrem no mesmo frame, o resultado é empate, sem depender de quem "age primeiro".
- **Colisão com varredura:** em vez de checar um único ponto à frente, o jogador verifica todos os pontos que vai percorrer no próximo frame, para nunca "pular" um rastro em velocidades altas.
- **Imunidade ao próprio rastro recente:** o jogador não morre no pedaço de rastro que acabou de criar, mas morre ao tocar em rastros antigos (próprios ou do adversário).
- **Herança e polimorfismo:** `AIPlayer` herda de `Player` e só sobrescreve a decisão de direção.
- **Desempenho:** as imagens dos rastros são criadas uma única vez por jogador e reaproveitadas, e as imagens dos jogadores só são redesenhadas quando um efeito muda.

## Próximos passos

- [ ] IA que busca power-ups
- [ ] Novos power-ups (apagar rastro do adversário, inverter controles)
- [ ] Tela de recordes
- [ ] Desenhar os rastros em uma matriz para melhorar o desempenho em partidas longas

## Sobre

Projeto desenvolvido por **Jiselle Marques dos Santos**, estudante de Computação na UFBA, como estudo de Java e programação orientada a objetos. 

Projeto educacional, inspirado no jogo *Tron* e sem fins comerciais. Não possui qualquer vínculo com os detentores da marca.
