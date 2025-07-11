package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.Games;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.model.GameStatus;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.GameServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameMapper gameMapper;

    @InjectMocks
    private GameServiceImpl gameService;

    private final String playerName = "John";

    @Test
    void createGame_shouldCreateGameForExistingPlayer() {
        // given
        Player existingPlayer = Player.builder().id("abc123").name(playerName).build();
        Games newGame = Games.builder()
                .id(1L)
                .playerId("abc123")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .build();

        GameResponse gameResponse = new GameResponse(
                newGame.getId(),
                newGame.getPlayerId(),
                newGame.getCreatedAt(),
                newGame.getStatus(),
                newGame.getPlayerScore(),
                newGame.getDealerScore()
        );

        when(playerRepository.findByName(playerName)).thenReturn(Mono.just(existingPlayer));
        when(gameRepository.save(any())).thenReturn(Mono.just(newGame));
        when(gameMapper.toResponse(newGame)).thenReturn(gameResponse);

        // when
        Mono<GameResponse> result = gameService.createGame(playerName);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.playerId().equals("abc123") &&
                                response.status().equals(GameStatus.IN_PROGRESS))
                .verifyComplete();

        verify(playerRepository).findByName(playerName);
        verify(gameRepository).save(any(Games.class));
        verify(gameMapper).toResponse(newGame);
    }

    @Test
    void createGame_shouldCreateGameForNewPlayer() {
        // Arrange
        Player player = Player.builder()
                .id("1")
                .name("John")
                .build();

        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);

        Games game = Games.builder()
                .id(123L)
                .playerId(player.getId())
                .createdAt(now)
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .build();

        GameResponse expectedResponse = new GameResponse(
                123L,
                "John",
                now,
                GameStatus.IN_PROGRESS,
                0,
                0
        );

        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(gameRepository.save(any(Games.class))).thenReturn(Mono.just(game));
        when(gameMapper.toResponse(game)).thenReturn(expectedResponse);

        // Act & Assert
        StepVerifier.create(gameService.createGame("John"))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(playerRepository).findByName("John");
        verify(gameRepository).save(any(Games.class));
        verify(gameMapper).toResponse(game);
    }




    @Test
    void createGame_shouldFailWhenGameSaveFails() {
        Player player = Player.builder().id("abc123").name(playerName).build();

        when(playerRepository.findByName(playerName)).thenReturn(Mono.just(player));
        when(gameRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB error")));

        Mono<GameResponse> result = gameService.createGame(playerName);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("DB error"))
                .verify();

        verify(playerRepository).findByName(playerName);
        verify(gameRepository).save(any());
    }

    @Test
    void createGame_shouldFailWhenSavingNewPlayerFails() {
        String playerName = "John";
        when(playerRepository.findByName(playerName)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.createGame(playerName))
                .expectErrorMatches(throwable ->
                        throwable instanceof PlayerNotFoundException &&
                                throwable.getMessage().equals("Player with name 'John' not found."))
                .verify();

        verify(playerRepository).findByName(playerName);
    }

    @Test
    void getGameById_shouldReturnGameIfExists() {
        Games game = Games.builder()
                .id(123L)
                .playerId("player1")
                .status(GameStatus.IN_PROGRESS)
                .build();

        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);

        GameResponse response = new GameResponse(
                123L,
                "player1",
                now,
                GameStatus.IN_PROGRESS,
                0,
                0
        );

        when(gameRepository.findById(123L)).thenReturn(Mono.just(game));
        when(gameMapper.toResponse(game)).thenReturn(response);

        StepVerifier.create(gameService.getGameById(123L))
                .expectNextMatches(r ->
                        r != null &&
                                r.status().equals(GameStatus.IN_PROGRESS))
                .verifyComplete();

        verify(gameRepository).findById(123L);
        verify(gameMapper).toResponse(game);
    }


    @Test
    void getGameById_shouldThrowWhenNotFound() {
        when(gameRepository.findById(123L)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.getGameById(123L))
                .expectErrorMatches(throwable ->
                        throwable instanceof GameNotFoundException &&
                                throwable.getMessage().equals("Game with id '123' not found."))
                .verify();

        verify(gameRepository).findById(123L);
    }

    @Test
    void deleteGame_shouldDeleteExistingGame() {
        Games game = Games.builder().id(456L).playerId("p1").build();

        when(gameRepository.findById(456L)).thenReturn(Mono.just(game));
        when(gameRepository.delete(game)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.deleteGame(456L))
                .verifyComplete();

        verify(gameRepository).findById(456L);
        verify(gameRepository).delete(game);
    }

    @Test
    void deleteGame_shouldThrowIfNotFound() {
        Long gameId = 999L;
        when(gameRepository.findById(gameId)).thenReturn(Mono.empty());

        StepVerifier.create(gameService.deleteGame(gameId))
                .expectErrorMatches(throwable ->
                        throwable instanceof GameNotFoundException &&
                                throwable.getMessage().equals("Game with id '999' not found."))
                .verify();

        verify(gameRepository).findById(gameId);
    }

    @Test
    void getAllGames_shouldReturnAllGames() {
        Games game1 = Games.builder()
                .id(1L)
                .playerId("player1")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(10)
                .dealerScore(8)
                .build();

        Games game2 = Games.builder()
                .id(2L)
                .playerId("player2")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.FINISHED)
                .playerScore(21)
                .dealerScore(19)
                .build();

        GameResponse response1 = new GameResponse(
                game1.getId(), game1.getPlayerId(), game1.getCreatedAt(),
                game1.getStatus(), game1.getPlayerScore(), game1.getDealerScore()
        );

        GameResponse response2 = new GameResponse(
                game2.getId(), game2.getPlayerId(), game2.getCreatedAt(),
                game2.getStatus(), game2.getPlayerScore(), game2.getDealerScore()
        );

        when(gameRepository.findAll()).thenReturn(Flux.just(game1, game2));
        when(gameMapper.toResponse(game1)).thenReturn(response1);
        when(gameMapper.toResponse(game2)).thenReturn(response2);

        StepVerifier.create(gameService.getAllGames())
                .expectNext(response1)
                .expectNext(response2)
                .verifyComplete();

        verify(gameRepository).findAll();
        verify(gameMapper).toResponse(game1);
        verify(gameMapper).toResponse(game2);
    }
    @Test
    void createGame_shouldFailWhenPlayerNameIsNull() {
        StepVerifier.create(gameService.createGame(null))
                .expectErrorMatches(throwable ->
                        throwable instanceof PlayerNotFoundException &&
                                throwable.getMessage().equals("Player name must not be null or empty."))
                .verify();
    }

    @Test
    void createGame_shouldFailWhenPlayerNameIsEmpty() {
        StepVerifier.create(gameService.createGame(""))
                .expectErrorMatches(throwable ->
                        throwable instanceof PlayerNotFoundException &&
                                throwable.getMessage().equals("Player name must not be null or empty."))
                .verify();
    }

    @Test
    void deleteGame_shouldThrowWhenIdIsNull() {
        StepVerifier.create(gameService.deleteGame(null))
                .expectErrorMatches(throwable ->
                        throwable instanceof GameNotFoundException &&
                                throwable.getMessage().equals("Game ID must not be null."))
                .verify();
    }

    @Test
    void createGame_shouldSetCreatedAtNotNull() {
        Player player = Player.builder().id("1").name("John").build();

        Games game = Games.builder()
                .id(123L)
                .playerId(player.getId())
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .build();

        GameResponse response = new GameResponse(
                game.getId(), game.getPlayerId(), game.getCreatedAt(),
                game.getStatus(), game.getPlayerScore(), game.getDealerScore()
        );

        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> {
            Games g = invocation.getArgument(0);
            g.setId(123L);
            g.setCreatedAt(LocalDateTime.now());
            return Mono.just(g);
        });
        when(gameMapper.toResponse(any(Games.class))).thenReturn(response);

        StepVerifier.create(gameService.createGame("John"))
                .assertNext(r -> {
                    assert r.createdAt() != null;
                })
                .verifyComplete();

        verify(playerRepository).findByName("John");
        verify(gameRepository).save(any(Games.class));
        verify(gameMapper).toResponse(any(Games.class));
    }

    @Test
    void createGame_shouldInitializeScoresToZero() {
        Player player = Player.builder().id("1").name("John").build();

        Games game = Games.builder()
                .id(123L)
                .playerId(player.getId())
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .build();

        GameResponse response = new GameResponse(
                game.getId(), game.getPlayerId(), game.getCreatedAt(),
                game.getStatus(), game.getPlayerScore(), game.getDealerScore()
        );

        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(gameRepository.save(any(Games.class))).thenReturn(Mono.just(game));
        when(gameMapper.toResponse(game)).thenReturn(response);

        StepVerifier.create(gameService.createGame("John"))
                .assertNext(r -> {
                    assert r.playerScore() == 0;
                    assert r.dealerScore() == 0;
                })
                .verifyComplete();

        verify(playerRepository).findByName("John");
        verify(gameRepository).save(any(Games.class));
        verify(gameMapper).toResponse(game);
    }


}
