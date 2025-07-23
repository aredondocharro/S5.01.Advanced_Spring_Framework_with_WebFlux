package cat.itacademy.blackjack.mapper;


import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.model.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface PlayerMapper {

    @Mapping(target = "gamesPlayed", constant = "0")
    @Mapping(target = "gamesWon", constant = "0")
    @Mapping(target = "totalScore", constant = "0")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Player toEntity(PlayerRequest request);

    PlayerResponse toResponse(Player player);
}
