package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameMapper gameMapper;
    private final DeckService deckService;

    @Override
    public Mono<GameResponse> createGame(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return Mono.error(PlayerNotFoundException.forInvalidInput());
        }

        return playerRepository.findByName(playerName)
                .switchIfEmpty(Mono.error(PlayerNotFoundException.forMissingName(playerName)))
                .flatMap(player -> {
                    Games game = Games.builder()
                            .playerId(player.getId())
                            .createdAt(LocalDateTime.now())
                            .status(GameStatus.IN_PROGRESS)
                            .playerScore(0)
                            .dealerScore(0)
                            .build();
                    return gameRepository.save(game);
                })
                .map(gameMapper::toResponse);
    }

    @Override
    public Mono<GameResponse> getGameById(Long gameId) {
        if (gameId == null) {
            return Mono.error(new GameNotFoundException("Game ID must not be null."));
        }

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .map(gameMapper::toResponse);
    }

    @Override
    public Flux<GameResponse> getAllGames() {
        return gameRepository.findAll()
                .map(gameMapper::toResponse);
    }

    @Override
    public Mono<Void> deleteGame(Long gameId) {
        if (gameId == null) {
            return Mono.error(new GameNotFoundException("Game ID must not be null."));
        }

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(gameRepository::delete);
    }


    @Override
    public Mono<GameResponse> playGame(Long gameId) {
        if (gameId == null) {
            return Mono.error(new GameNotFoundException("Game ID must not be null"));
        }

        return gameRepository.findById(gameId)
                .switchIfEmpty(Mono.error(new GameNotFoundException(gameId)))
                .flatMap(game -> {
                    if (game.getStatus() != GameStatus.IN_PROGRESS) {
                        return Mono.just(gameMapper.toResponse(game));
                    }

                    TurnResult playerTurn = simulateTurn(deckService);
                    TurnResult dealerTurn = simulateTurn(deckService);

                    int playerScore = playerTurn.score();
                    int dealerScore = dealerTurn.score();

                    GameStatus result;
                    if (playerScore > 21) {
                        result = GameStatus.DEALER_WON;
                    } else if (dealerScore > 21) {
                        result = GameStatus.PLAYER_WON;
                    } else if (playerScore > dealerScore) {
                        result = GameStatus.PLAYER_WON;
                    } else if (dealerScore > playerScore) {
                        result = GameStatus.DEALER_WON;
                    } else {
                        result = GameStatus.DRAW;
                    }

                    game.setPlayerScore(playerScore);
                    game.setDealerScore(dealerScore);
                    game.setStatus(result);

                    return gameRepository.save(game)
                            .map(updatedGame -> new GameResponse(
                                    updatedGame.getId(),
                                    updatedGame.getPlayerId(),
                                    updatedGame.getCreatedAt(),
                                    updatedGame.getStatus(),
                                    updatedGame.getPlayerScore(),
                                    updatedGame.getDealerScore(),
                                    playerTurn.cards(),
                                    dealerTurn.cards()
                            ));
                });
    }
    private TurnResult simulateTurn(DeckService deckService) {
        List<Card> cards = new ArrayList<>();
        int score = 0;

        while (score < 17) {
            Card card = deckService.drawCard();
            cards.add(card);
            score = calculateScore(cards);
        }

        return new TurnResult(score, cards);
    }

    private int calculateScore(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getValue().getPoints())
                .sum();
    }

}

