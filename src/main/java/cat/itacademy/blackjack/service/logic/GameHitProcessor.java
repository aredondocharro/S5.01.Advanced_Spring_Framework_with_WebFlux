package cat.itacademy.blackjack.service.logic;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.InsufficientCardsException;
import cat.itacademy.blackjack.exception.InvalidGameStateException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.engine.BlackjackEngine;
import cat.itacademy.blackjack.service.engine.DeckManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameHitProcessor {

    private final GameRepository gameRepository;
    private final DeckManager deckManager;
    private final BlackjackEngine blackjackEngine;
    private final GameMapper gameMapper;
    private final PlayerStatsUpdater playerStatsUpdater;

    public Mono<GameResponse> processHit(Long gameId) {
        if (gameId == null) {
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    if (game.getTurn() != GameTurn.PLAYER_TURN || game.getStatus() != GameStatus.IN_PROGRESS) {
                        return Mono.error(new InvalidGameStateException("Game is already finished or not in player's turn."));
                    }

                    return Mono.zip(
                            deckManager.deserializeCardsReactive(game.getDeckJson()),
                            deckManager.deserializeCardsReactive(game.getPlayerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getDealerCardsJson())
                    ).flatMap(tuple -> {
                        List<Card> deck = new ArrayList<>(tuple.getT1());
                        List<Card> playerCards = new ArrayList<>(tuple.getT2());
                        List<Card> dealerCards = tuple.getT3();

                        if (deck.isEmpty()) {
                            return Mono.error(new InsufficientCardsException("No cards left in deck"));
                        }

                        // Añadir nueva carta al jugador
                        Card newCard = deck.remove(0);
                        playerCards.add(newCard);

                        int playerScore = blackjackEngine.calculateScore(playerCards);

                        // Actualizar el juego
                        game.setPlayerCardsJson(deckManager.serializeDeck(playerCards));
                        game.setDeckJson(deckManager.serializeDeck(deck));
                        game.setPlayerScore(playerScore);

                        // Evaluar si el jugador ha terminado
                        if (playerScore > 21) {
                            game.setStatus(GameStatus.FINISHED_DEALER_WON);
                            game.setTurn(GameTurn.FINISHED);
                        } else if (playerScore == 21) {
                            // Puede que en tu lógica quieras resolver automáticamente contra dealer aquí
                            GameStatus resolved = blackjackEngine.determineWinner(playerScore, game.getDealerScore());
                            game.setStatus(resolved);
                            game.setTurn(GameTurn.FINISHED);
                        }

                        return gameRepository.save(game)
                                .flatMap(updated ->
                                        playerStatsUpdater.updateAfterGameIfFinished(updated)
                                                .thenReturn(gameMapper.toResponse(updated, playerCards, dealerCards))
                                );
                    });
                });
    }
}




