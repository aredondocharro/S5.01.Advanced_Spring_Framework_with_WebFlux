package cat.itacademy.blackjack.mapper;

import cat.itacademy.blackjack.dto.CardResponseDTO;
import cat.itacademy.blackjack.model.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CardMapper {

    // Para una sola carta (usado en tests, si haces mocking con when(...toDto(...)))
    default CardResponseDTO toDto(Card card) {
        return new CardResponseDTO(
                card.getSuit().name(),
                card.getValue().name()
        );
    }

    // Para listas de cartas (usado normalmente en lógica real)
    @Named("toDtoList")
    default List<CardResponseDTO> toDtoList(List<Card> cards) {
        return cards.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
