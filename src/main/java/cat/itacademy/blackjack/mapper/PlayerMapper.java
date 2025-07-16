package cat.itacademy.blackjack.mapper;


import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.model.Player;
import org.mapstruct.Mapper;


import java.time.LocalDateTime;


@Mapper(componentModel = "spring")
public interface PlayerMapper {

    default Player toEntity(PlayerRequest request) {
        return Player.builder()
                .name(request.name())
                .gamesPlayed(0)
                .gamesWon(0)
                .totalScore(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    PlayerResponse toResponse(Player player);

}
