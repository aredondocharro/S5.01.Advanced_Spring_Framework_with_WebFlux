package cat.itacademy.blackjack.mapper;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.model.Games;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface GameMapper {

    GameMapper INSTANCE = Mappers.getMapper(GameMapper.class);

    GameResponse toResponse(Games game);
}

