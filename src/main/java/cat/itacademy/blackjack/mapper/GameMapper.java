package cat.itacademy.blackjack.mapper;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.model.Games;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CardMapper.class)
public interface GameMapper {

    GameResponse toResponse(Games game);
}

