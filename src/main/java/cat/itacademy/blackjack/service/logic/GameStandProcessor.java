package cat.itacademy.blackjack.service.logic;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
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
public class GameStandProcessor {

    private final GameRepository gameRepository;
    private final DeckManager deckManager;
    private final BlackjackEngine blackjackEngine;
    private final GameMapper gameMapper;

    public Mono<GameResponse> processStand(Long gameId) {
        if (gameId == null) {
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    if (game.getTurn() != GameTurn.PLAYER_TURN || game.getStatus() != GameStatus.IN_PROGRESS) {
                        return Mono.error(new InvalidGameStateException("Game is not in player's turn"));
                    }

                    return Mono.zip(
                            deckManager.deserializeCardsReactive(game.getDealerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getPlayerCardsJson()),
                            deckManager.deserializeCardsReactive(game.getDeckJson())
                    ).flatMap(tuple -> {
                        List<Card> dealerCards = new ArrayList<>(tuple.getT1());
                        List<Card> playerCards = tuple.getT2();
                        List<Card> deck = new ArrayList<>(tuple.getT3());


                        int dealerScore = blackjackEngine.calculateScore(dealerCards);
                        while (dealerScore < 17 && !deck.isEmpty()) {
                            Card newCard = deck.remove(0);
                            dealerCards.add(newCard);
                            dealerScore = blackjackEngine.calculateScore(dealerCards);
                        }

                        int playerScore = blackjackEngine.calculateScore(playerCards);
                        GameStatus finalStatus = resolveGameStatus(playerScore, dealerScore);

                        game.setDealerCards(dealerCards);
                        game.setDealerScore(dealerScore);
                        game.setDealerCardsJson(deckManager.serializeCards(dealerCards));
                        game.setDeckJson(deckManager.serializeCards(deck));
                        game.setStatus(finalStatus);
                        game.setTurn(GameTurn.FINISHED);

                        return gameRepository.save(game)
                                .map(updated -> gameMapper.toResponse(updated, playerCards, dealerCards));
                    });
                });
    }


    private GameStatus resolveGameStatus(int playerScore, int dealerScore) {
        if (dealerScore > 21) {
            return GameStatus.FINISHED_PLAYER_WON;
        } else if (dealerScore == playerScore) {
            return GameStatus.FINISHED_DRAW;
        } else if (dealerScore > playerScore) {
            return GameStatus.FINISHED_DEALER_WON;
        } else {
            return GameStatus.FINISHED_PLAYER_WON;
        }
    }
}

