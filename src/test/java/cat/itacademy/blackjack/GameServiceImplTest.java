package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.CardResponseDTO;
import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.exception.GameNotFoundException;
import cat.itacademy.blackjack.exception.PlayerNotFoundException;
import cat.itacademy.blackjack.mapper.CardMapper;
import cat.itacademy.blackjack.mapper.GameMapper;
import cat.itacademy.blackjack.model.*;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import cat.itacademy.blackjack.repository.sql.GameRepository;
import cat.itacademy.blackjack.service.DeckService;
import cat.itacademy.blackjack.service.GameServiceImpl;
import cat.itacademy.blackjack.service.engine.DeckManager;
import cat.itacademy.blackjack.service.engine.GameFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.List;


import static com.mongodb.internal.connection.tlschannel.util.Util.assertTrue;
import static org.bson.assertions.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Mock
    private DeckService deckService;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private GameFactory gameFactory;

    @Mock
    private DeckManager deckManager;

    @InjectMocks
    private GameServiceImpl gameService;


    private final String playerName = "John";

    @Test
    void createGame_shouldCreateGameForNewPlayer() {
        // Arrange
        Player player = Player.builder()
                .id("1")
                .name("John")
                .build();

        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0);

        // Cartas que forman parte del mazo (orden importa)
        Card playerCard1 = new Card(CardSuit.HEARTS, CardValue.FIVE);
        Card playerCard2 = new Card(CardSuit.CLUBS, CardValue.SIX);
        Card dealerCard1 = new Card(CardSuit.SPADES, CardValue.SEVEN);
        Card dealerCard2 = new Card(CardSuit.DIAMONDS, CardValue.EIGHT);

        List<Card> fullDeck = List.of(playerCard1, playerCard2, dealerCard1, dealerCard2);
        List<Card> playerCards = List.of(playerCard1, playerCard2);
        List<Card> dealerCards = List.of(dealerCard1, dealerCard2);

        // DTOs esperados después del mapeo con MapStruct
        List<CardResponseDTO> playerCardDtos = List.of(
                new CardResponseDTO(CardSuit.HEARTS.name(), CardValue.FIVE.name()),
                new CardResponseDTO(CardSuit.CLUBS.name(), CardValue.SIX.name())
        );

        List<CardResponseDTO> dealerCardDtos = List.of(
                new CardResponseDTO(CardSuit.SPADES.name(), CardValue.SEVEN.name()),
                new CardResponseDTO(CardSuit.DIAMONDS.name(), CardValue.EIGHT.name())
        );

        // Juego que será guardado
        Games savedGame = Games.builder()
                .id(123L)
                .playerId(player.getId())
                .createdAt(now)
                .status(GameStatus.IN_PROGRESS)
                .playerScore(11) // 5 + 6
                .dealerScore(15) // 7 + 8
                .build();

        GameResponse expectedResponse = new GameResponse(
                123L,
                player.getId(),
                now,
                GameStatus.IN_PROGRESS,
                11,
                15,
                playerCardDtos,
                dealerCardDtos
        );

        // Mocks
        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(deckService.getShuffledDeck()).thenReturn(fullDeck);
        when(cardMapper.toDtoList(playerCards)).thenReturn(playerCardDtos);
        when(cardMapper.toDtoList(dealerCards)).thenReturn(dealerCardDtos);
        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> {
            Games game = invocation.getArgument(0);
            game.setId(123L);
            game.setCreatedAt(now);
            return Mono.just(game);
        });

        // Act & Assert
        StepVerifier.create(gameService.createGame("John"))
                .expectNextMatches(response ->
                        response.id().equals(123L) &&
                                response.playerId().equals("1") &&
                                response.status() == GameStatus.IN_PROGRESS &&
                                response.playerScore() == 11 &&
                                response.dealerScore() == 15 &&
                                response.playerCards().equals(playerCardDtos) &&
                                response.dealerCards().equals(dealerCardDtos)
                )
                .verifyComplete();

        // Verify interactions
        verify(playerRepository).findByName("John");
        verify(deckService).getShuffledDeck();
        verify(cardMapper).toDtoList(playerCards);
        verify(cardMapper).toDtoList(dealerCards);
        verify(gameRepository).save(any(Games.class));
    }

    @Test
    void createGame_shouldFailWhenGameSaveFails() {
        // Arrange
        Player player = Player.builder().id("abc123").name(playerName).build();

        // Necesario: al menos 4 cartas para pasar la validación del mazo
        List<Card> fullDeck = List.of(
                new Card(CardSuit.HEARTS, CardValue.FIVE),
                new Card(CardSuit.SPADES, CardValue.SIX),
                new Card(CardSuit.CLUBS, CardValue.SEVEN),
                new Card(CardSuit.DIAMONDS, CardValue.EIGHT)
        );

        when(playerRepository.findByName(playerName)).thenReturn(Mono.just(player));
        when(deckService.getShuffledDeck()).thenReturn(fullDeck);
        when(gameRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB error")));

        // Act
        Mono<GameResponse> result = gameService.createGame(playerName);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("DB error"))
                .verify();

        verify(playerRepository).findByName(playerName);
        verify(deckService).getShuffledDeck();
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
        String deckJson = """
        [
            {"suit":"HEARTS","value":"FIVE"},
            {"suit":"SPADES","value":"TEN"}
        ]
    """;

        Games game = Games.builder()
                .id(123L)
                .playerId("player1")
                .status(GameStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .playerScore(0)
                .dealerScore(0)
                .deckJson(deckJson)
                .build();

        // 🔹 Simulamos las cartas esperadas después de deserializar el deckJson
        List<Card> expectedCards = List.of(
                new Card(CardSuit.HEARTS, CardValue.FIVE),
                new Card(CardSuit.SPADES, CardValue.TEN)
        );

        // 🔹 Response esperado
        GameResponse response = new GameResponse(
                123L,
                "player1",
                game.getCreatedAt(),
                GameStatus.IN_PROGRESS,
                0,
                0,
                expectedCards.stream().map(cardMapper::toDto).toList(), // o directamente List.of()
                expectedCards.stream().map(cardMapper::toDto).toList()
        );

        // 🔹 Mocks
        when(gameRepository.findById(123L)).thenReturn(Mono.just(game));
        when(gameMapper.toResponse(eq(game), anyList(), anyList())).thenReturn(response);

        StepVerifier.create(gameService.getGameById(123L))
                .expectNextMatches(r ->
                        r != null &&
                                r.id() == 123L &&
                                r.playerId().equals("player1") &&
                                r.status() == GameStatus.IN_PROGRESS
                )
                .verifyComplete();

        verify(gameRepository).findById(123L);
        verify(gameMapper).toResponse(eq(game), anyList(), anyList());
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
        // Arrange
        Games game = Games.builder().id(456L).playerId("p1").build();

        when(gameRepository.findById(456L)).thenReturn(Mono.just(game));
        when(gameRepository.delete(game)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(gameService.deleteGame(456L))
                .verifyComplete();

        verify(gameRepository).findById(456L);
        verify(gameRepository).delete(game);

        // No se stubbean métodos innecesarios
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
    void getAllGames_shouldReturnAllGamesSuccessfully() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.now();

        List<Card> deck = List.of(
                new Card(CardSuit.HEARTS, CardValue.TWO),
                new Card(CardSuit.HEARTS, CardValue.THREE),
                new Card(CardSuit.CLUBS, CardValue.FOUR),
                new Card(CardSuit.SPADES, CardValue.FIVE)
        );

        String deckJson = "";
        try {
            deckJson = new ObjectMapper().writeValueAsString(deck);
        } catch (JsonProcessingException e) {
            fail("Error serializing deck JSON: " + e.getMessage());
        }

        Games game1 = Games.builder()
                .id(1L)
                .playerId("player1")
                .createdAt(createdAt)
                .status(GameStatus.IN_PROGRESS)
                .playerScore(10)
                .dealerScore(8)
                .deckJson(deckJson)
                .build();

        Games game2 = Games.builder()
                .id(2L)
                .playerId("player2")
                .createdAt(createdAt)
                .status(GameStatus.FINISHED)
                .playerScore(21)
                .dealerScore(19)
                .deckJson(deckJson)
                .build();


        List<CardResponseDTO> expectedPlayerCards = deck.subList(0, 2).stream()
                .map(cardMapper::toDto)
                .toList();

        List<CardResponseDTO> expectedDealerCards = deck.subList(2, 4).stream()
                .map(cardMapper::toDto)
                .toList();

        when(gameRepository.findAll()).thenReturn(Flux.just(game1, game2));

        when(deckManager.parseDeckReactive(deckJson)).thenReturn(Mono.just(deck));
        when(deckManager.splitDeck(deck)).thenReturn(Tuples.of(deck.subList(0, 2), deck.subList(2, 4)));

        when(gameMapper.toResponse(eq(game1), any(), any()))
                .thenAnswer(invocation -> {
                    Games g = invocation.getArgument(0);
                    List<Card> playerCards = invocation.getArgument(1);
                    List<Card> dealerCards = invocation.getArgument(2);


                    return new GameResponse(
                            g.getId(), g.getPlayerId(), g.getCreatedAt(), g.getStatus(),
                            g.getPlayerScore(), g.getDealerScore(),
                            playerCards.stream().map(cardMapper::toDto).toList(),
                            dealerCards.stream().map(cardMapper::toDto).toList()
                    );
                });

        when(gameMapper.toResponse(eq(game2), any(), any()))
                .thenAnswer(invocation -> {
                    Games g = invocation.getArgument(0);
                    List<Card> playerCards = invocation.getArgument(1);
                    List<Card> dealerCards = invocation.getArgument(2);

                    return new GameResponse(
                            g.getId(), g.getPlayerId(), g.getCreatedAt(), g.getStatus(),
                            g.getPlayerScore(), g.getDealerScore(),
                            playerCards.stream().map(cardMapper::toDto).toList(),
                            dealerCards.stream().map(cardMapper::toDto).toList()
                    );
                });

        // Act & Assert
        StepVerifier.create(gameService.getAllGames())
                .assertNext(response -> {
                    assertEquals(1L, response.id());
                    assertEquals("player1", response.playerId());
                    assertEquals(10, response.playerScore());
                    assertEquals(8, response.dealerScore());
                    assertEquals(GameStatus.IN_PROGRESS, response.status());
                    assertEquals(expectedPlayerCards, response.playerCards());
                    assertEquals(expectedDealerCards, response.dealerCards());
                })
                .assertNext(response -> {
                    assertEquals(2L, response.id());
                    assertEquals("player2", response.playerId());
                    assertEquals(21, response.playerScore());
                    assertEquals(19, response.dealerScore());
                    assertEquals(GameStatus.FINISHED, response.status());
                    assertEquals(expectedPlayerCards, response.playerCards());
                    assertEquals(expectedDealerCards, response.dealerCards());
                })
                .verifyComplete();

        verify(gameRepository).findAll();
        verify(deckManager, times(2)).parseDeckReactive(anyString());
        verify(gameMapper).toResponse(eq(game1), any(), any());
        verify(gameMapper).toResponse(eq(game2), any(), any());
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
        // Arrange
        Player player = Player.builder().id("1").name("John").build();

        List<Card> deck = List.of(
                new Card(CardSuit.HEARTS, CardValue.FIVE),
                new Card(CardSuit.SPADES, CardValue.SIX),
                new Card(CardSuit.CLUBS, CardValue.TWO),
                new Card(CardSuit.DIAMONDS, CardValue.THREE)
        );

        List<Card> playerCards = deck.subList(0, 2);
        List<Card> dealerCards = deck.subList(2, 4);

        List<CardResponseDTO> playerCardDtos = List.of(
                new CardResponseDTO("HEARTS", "FIVE"),
                new CardResponseDTO("SPADES", "SIX")
        );
        List<CardResponseDTO> dealerCardDtos = List.of(
                new CardResponseDTO("CLUBS", "TWO"),
                new CardResponseDTO("DIAMONDS", "THREE")
        );

        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(deckService.getShuffledDeck()).thenReturn(deck);
        when(cardMapper.toDtoList(playerCards)).thenReturn(playerCardDtos);
        when(cardMapper.toDtoList(dealerCards)).thenReturn(dealerCardDtos);

        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> {
            Games g = invocation.getArgument(0);
            g.setId(123L);
            g.setCreatedAt(LocalDateTime.now());
            return Mono.just(g);
        });
        when(gameMapper.toResponse(any(Games.class), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    Games g = invocation.getArgument(0);
                    List<CardResponseDTO> playerDtos = invocation.getArgument(1);
                    List<CardResponseDTO> dealerDtos = invocation.getArgument(2);
                    return new GameResponse(
                            g.getId(),
                            g.getPlayerId(),
                            g.getCreatedAt(),
                            g.getStatus(),
                            g.getPlayerScore(),
                            g.getDealerScore(),
                            playerDtos,
                            dealerDtos
                    );
                });
        // Act & Assert
        StepVerifier.create(gameService.createGame("John"))
                .assertNext(response -> assertNotNull(response.createdAt()))
                .verifyComplete();
    }




    @Test
    void createGame_shouldCalculateCorrectInitialScores() {
        // Arrange
        Player player = Player.builder().id("1").name("John").build();

        List<Card> deck = List.of(
                new Card(CardSuit.HEARTS, CardValue.FIVE),     // Player
                new Card(CardSuit.CLUBS, CardValue.SIX),       // Player
                new Card(CardSuit.SPADES, CardValue.TEN),      // Dealer
                new Card(CardSuit.DIAMONDS, CardValue.ACE)     // Dealer
        );

        List<Card> playerCards = deck.subList(0, 2);  // 5 + 6 = 11
        List<Card> dealerCards = deck.subList(2, 4);  // 10 + 11 = 21

        List<CardResponseDTO> playerCardDtos = List.of(
                new CardResponseDTO("HEARTS", "FIVE"),
                new CardResponseDTO("CLUBS", "SIX")
        );
        List<CardResponseDTO> dealerCardDtos = List.of(
                new CardResponseDTO("SPADES", "TEN"),
                new CardResponseDTO("DIAMONDS", "ACE")
        );

        when(playerRepository.findByName("John")).thenReturn(Mono.just(player));
        when(deckService.getShuffledDeck()).thenReturn(deck);
        when(cardMapper.toDtoList(playerCards)).thenReturn(playerCardDtos);
        when(cardMapper.toDtoList(dealerCards)).thenReturn(dealerCardDtos);

        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> {
            Games g = invocation.getArgument(0);
            g.setId(999L);
            g.setCreatedAt(LocalDateTime.now());
            g.setPlayerScore(11);
            g.setDealerScore(21);
            return Mono.just(g);
        });

        // Act & Assert
        StepVerifier.create(gameService.createGame("John"))
                .assertNext(response -> {
                    assertEquals(11, response.playerScore());
                    assertEquals(21, response.dealerScore());
                })
                .verifyComplete();
    }


    @Test
    void playGame_shouldSimulateTurnAndReturnResponse_whenDealerBusts() {
        // Arrange
        Long gameId = 1L;
        ObjectMapper mapper = new ObjectMapper();

        Card playerCard1 = new Card(CardSuit.HEARTS, CardValue.TEN);
        Card playerCard2 = new Card(CardSuit.SPADES, CardValue.SEVEN);
        Card dealerCard1 = new Card(CardSuit.CLUBS, CardValue.SIX);
        Card dealerCard2 = new Card(CardSuit.DIAMONDS, CardValue.NINE);
        Card dealerCard3 = new Card(CardSuit.HEARTS, CardValue.NINE);

        List<Card> deck = List.of(playerCard1, playerCard2, dealerCard1, dealerCard2, dealerCard3);

        String deckJson;
        try {
            deckJson = mapper.writeValueAsString(deck);
        } catch (JsonProcessingException e) {
            fail("Failed to serialize deck: " + e.getMessage());
            return;
        }

        Games game = Games.builder()
                .id(gameId)
                .playerId("player-123")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .deckJson(deckJson)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Mono.just(game));
        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));


        when(gameMapper.toResponse(any(Games.class), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    Games updatedGame = invocation.getArgument(0);
                    List<Card> playerCards = invocation.getArgument(1);
                    List<Card> dealerCards = invocation.getArgument(2);

                    List<CardResponseDTO> playerDtos = playerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    List<CardResponseDTO> dealerDtos = dealerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    return new GameResponse(
                            updatedGame.getId(),
                            updatedGame.getPlayerId(),
                            updatedGame.getCreatedAt(),
                            updatedGame.getStatus(),
                            updatedGame.getPlayerScore(),
                            updatedGame.getDealerScore(),
                            playerDtos,
                            dealerDtos
                    );
                });

        // Act & Assert
        StepVerifier.create(gameService.playGame(gameId))
                .assertNext(response -> {
                    assertEquals(gameId, response.id());
                    assertEquals(17, response.playerScore());
                    assertEquals(24, response.dealerScore());
                    assertEquals(GameStatus.PLAYER_WON, response.status());
                    assertEquals(2, response.playerCards().size());
                    assertEquals(3, response.dealerCards().size());
                })
                .verifyComplete();
    }



    @Test
    void playGame_shouldReturnDrawWhenScoresAreEqual() throws JsonProcessingException {
        Long gameId = 2L;
        ObjectMapper mapper = new ObjectMapper();

        Card playerCard1 = new Card(CardSuit.HEARTS, CardValue.TEN);
        Card playerCard2 = new Card(CardSuit.SPADES, CardValue.SEVEN);
        Card dealerCard1 = new Card(CardSuit.CLUBS, CardValue.NINE);
        Card dealerCard2 = new Card(CardSuit.DIAMONDS, CardValue.EIGHT);

        List<Card> deck = List.of(playerCard1, playerCard2, dealerCard1, dealerCard2);
        String deckJson = mapper.writeValueAsString(deck);

        Games game = Games.builder()
                .id(gameId)
                .playerId("player-456")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .deckJson(deckJson)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Mono.just(game));
        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(gameMapper.toResponse(any(Games.class), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    Games updatedGame = invocation.getArgument(0);
                    List<Card> playerCards = invocation.getArgument(1);
                    List<Card> dealerCards = invocation.getArgument(2);

                    List<CardResponseDTO> playerDtos = playerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    List<CardResponseDTO> dealerDtos = dealerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    return new GameResponse(
                            updatedGame.getId(),
                            updatedGame.getPlayerId(),
                            updatedGame.getCreatedAt(),
                            updatedGame.getStatus(),
                            updatedGame.getPlayerScore(),
                            updatedGame.getDealerScore(),
                            playerDtos,
                            dealerDtos
                    );
                });
        StepVerifier.create(gameService.playGame(gameId))
                .assertNext(response -> {
                    assertEquals(gameId, response.id());
                    assertEquals(17, response.playerScore());
                    assertEquals(17, response.dealerScore());
                    assertEquals(GameStatus.DRAW, response.status());
                    assertEquals(2, response.playerCards().size());
                    assertEquals(2, response.dealerCards().size());
                })
                .verifyComplete();

        verify(gameRepository).save(any(Games.class));
    }

    @Test
    void playGame_shouldReturnDealerWonWhenPlayerBusts() throws JsonProcessingException {
        Long gameId = 3L;
        ObjectMapper mapper = new ObjectMapper();

        Card playerCard1 = new Card(CardSuit.HEARTS, CardValue.KING);
        Card playerCard2 = new Card(CardSuit.SPADES, CardValue.QUEEN);
        Card playerCard3 = new Card(CardSuit.CLUBS, CardValue.TWO);
        Card dealerCard1 = new Card(CardSuit.DIAMONDS, CardValue.FIVE);
        Card dealerCard2 = new Card(CardSuit.SPADES, CardValue.SIX);

        List<Card> deck = List.of(playerCard1, playerCard3, playerCard2, dealerCard1, dealerCard2);
        String deckJson = mapper.writeValueAsString(deck);

        Games game = Games.builder()
                .id(gameId)
                .playerId("player-789")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .deckJson(deckJson)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Mono.just(game));
        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(gameMapper.toResponse(any(Games.class), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    Games updatedGame = invocation.getArgument(0);
                    List<Card> playerCards = invocation.getArgument(1);
                    List<Card> dealerCards = invocation.getArgument(2);

                    List<CardResponseDTO> playerDtos = playerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    List<CardResponseDTO> dealerDtos = dealerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    return new GameResponse(
                            updatedGame.getId(),
                            updatedGame.getPlayerId(),
                            updatedGame.getCreatedAt(),
                            updatedGame.getStatus(),
                            updatedGame.getPlayerScore(),
                            updatedGame.getDealerScore(),
                            playerDtos,
                            dealerDtos
                    );
                });
        StepVerifier.create(gameService.playGame(gameId))
                .assertNext(response -> {
                    assertEquals(gameId, response.id());
                    assertEquals(22, response.playerScore());
                    assertEquals(GameStatus.DEALER_WON, response.status());
                    assertEquals(3, response.playerCards().size());
                })
                .verifyComplete();

        verify(gameRepository).save(any(Games.class));
    }

    @Test
    void playGame_shouldReturnDealerWonWhenDealerHasHigherScore() throws JsonProcessingException {
        Long gameId = 4L;
        ObjectMapper mapper = new ObjectMapper();

        Card playerCard1 = new Card(CardSuit.HEARTS, CardValue.TEN);
        Card playerCard2 = new Card(CardSuit.SPADES, CardValue.SEVEN);
        Card dealerCard1 = new Card(CardSuit.CLUBS, CardValue.NINE);
        Card dealerCard2 = new Card(CardSuit.DIAMONDS, CardValue.NINE);

        List<Card> deck = List.of(playerCard1, playerCard2, dealerCard1, dealerCard2);
        String deckJson = mapper.writeValueAsString(deck);

        Games game = Games.builder()
                .id(gameId)
                .playerId("player-999")
                .createdAt(LocalDateTime.now())
                .status(GameStatus.IN_PROGRESS)
                .playerScore(0)
                .dealerScore(0)
                .deckJson(deckJson)
                .build();

        when(gameRepository.findById(gameId)).thenReturn(Mono.just(game));
        when(gameRepository.save(any(Games.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(gameMapper.toResponse(any(Games.class), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    Games updatedGame = invocation.getArgument(0);
                    List<Card> playerCards = invocation.getArgument(1);
                    List<Card> dealerCards = invocation.getArgument(2);

                    List<CardResponseDTO> playerDtos = playerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    List<CardResponseDTO> dealerDtos = dealerCards.stream()
                            .map(c -> new CardResponseDTO(c.getSuit().name(), c.getValue().name()))
                            .toList();

                    return new GameResponse(
                            updatedGame.getId(),
                            updatedGame.getPlayerId(),
                            updatedGame.getCreatedAt(),
                            updatedGame.getStatus(),
                            updatedGame.getPlayerScore(),
                            updatedGame.getDealerScore(),
                            playerDtos,
                            dealerDtos
                    );
                });
        StepVerifier.create(gameService.playGame(gameId))
                .assertNext(response -> {
                    assertEquals(gameId, response.id());
                    assertEquals(17, response.playerScore());
                    assertEquals(18, response.dealerScore());
                    assertEquals(GameStatus.DEALER_WON, response.status());
                    assertEquals(2, response.playerCards().size());
                    assertEquals(2, response.dealerCards().size());
                })
                .verifyComplete();

        verify(gameRepository).save(any(Games.class));
    }

}
