package cat.itacademy.blackjack.mapper;

import cat.itacademy.blackjack.dto.PlayerRankingResponse;
import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.model.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface PlayerMapper {

    PlayerMapper INSTANCE = Mappers.getMapper(PlayerMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gamesPlayed", constant = "0")
    @Mapping(target = "gamesWon", constant = "0")
    @Mapping(target = "totalScore", constant = "0")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    Player toEntity(PlayerRequest request);


    PlayerResponse toResponse(Player player);

    default PlayerRankingResponse toRankingResponse(Player player) {
        double winRate = player.getGamesPlayed() == 0
                ? 0.0
                : (double) player.getGamesWon() / player.getGamesPlayed();

        return new PlayerRankingResponse(
                player.getName(),
                player.getGamesPlayed(),
                player.getGamesWon(),
                winRate
        );
    }
}
