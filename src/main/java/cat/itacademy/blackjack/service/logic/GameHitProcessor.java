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

                    // 🔁 Cambiamos parseDeckReactive por deserializeCardsReactive
                    return deckManager.deserializeCardsReactive(game.getDeckJson())
                            .flatMap(deck -> {
                                if (deck.isEmpty()) {
                                    return Mono.error(new InsufficientCardsException("No cards left in deck"));
                                }

                                Card newCard = deck.remove(0);

                                return deckManager.deserializeCardsReactive(game.getPlayerCardsJson())
                                        .flatMap(playerCards -> {
                                            List<Card> updatedPlayerCards = new ArrayList<>(playerCards);
                                            updatedPlayerCards.add(newCard);

                                            int newScore = blackjackEngine.calculateScore(updatedPlayerCards);

                                            // 💾 Actualizar el estado del juego
                                            game.setPlayerCardsJson(deckManager.serializeDeck(updatedPlayerCards));
                                            game.setPlayerScore(newScore);
                                            game.setDeckJson(deckManager.serializeDeck(deck));

                                            if (newScore > 21) {
                                                game.setStatus(GameStatus.FINISHED_DEALER_WON);
                                                game.setTurn(GameTurn.FINISHED);
                                            }

                                            return gameRepository.save(game)
                                                    .flatMap(updated ->
                                                            deckManager.deserializeCardsReactive(game.getDealerCardsJson())
                                                                    .map(dealerCards ->
                                                                            gameMapper.toResponse(updated, updatedPlayerCards, dealerCards)
                                                                    )
                                                    );
                                        });
                            });
                });
    }
}




