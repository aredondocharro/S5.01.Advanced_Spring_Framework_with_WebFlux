package cat.itacademy.blackjack.mapper;

import cat.itacademy.blackjack.dto.CardResponseDTO;
import cat.itacademy.blackjack.model.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CardMapper {

    default CardResponseDTO toDto(Card card) {
        return new CardResponseDTO(card.getSuit().name(), card.getValue().name());
    }

    @Named("toDtoList")
    default List<CardResponseDTO> toDtoList(List<Card> cards) {
        return cards.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
