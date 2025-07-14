package cat.itacademy.blackjack.mapper;

import cat.itacademy.blackjack.dto.GameResponse;
import cat.itacademy.blackjack.model.Card;
import cat.itacademy.blackjack.model.Games;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = CardMapper.class)
public interface GameMapper {

    @Mapping(target = "playerCards", source = "playerCards", qualifiedByName = "toDtoList")
    @Mapping(target = "dealerCards", source = "dealerCards", qualifiedByName = "toDtoList")
    GameResponse toResponse(Games game, List<Card> playerCards, List<Card> dealerCards);
}