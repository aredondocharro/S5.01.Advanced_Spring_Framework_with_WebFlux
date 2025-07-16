package cat.itacademy.blackjack;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.mapper.PlayerMapper;
import cat.itacademy.blackjack.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMapperTest {

    private PlayerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(PlayerMapper.class); // usa implementación generada por MapStruct
    }

    @Test
    void toEntity_shouldMapPlayerRequestToPlayerEntity() {
        PlayerRequest request = new PlayerRequest("John");

        Player result = mapper.toEntity(request);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(0, result.getGamesPlayed());
        assertEquals(0, result.getGamesWon());
        assertEquals(0, result.getTotalScore());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void toResponse_shouldMapPlayerEntityToPlayerResponse() {
        LocalDateTime now = LocalDateTime.now();

        Player player = Player.builder()
                .id("abc123")
                .name("Jane")
                .gamesPlayed(10)
                .gamesWon(5)
                .totalScore(250)
                .createdAt(now)
                .build();

        PlayerResponse response = mapper.toResponse(player);

        assertNotNull(response);
        assertEquals("abc123", response.id());
        assertEquals("Jane", response.name());
        assertEquals(250, response.totalScore());
        assertEquals(now, response.createdAt());
    }
}
