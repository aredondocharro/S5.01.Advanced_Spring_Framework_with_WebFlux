package cat.itacademy.blackjack.mapper;

import cat.itacademy.blackjack.dto.CardResponseDTO;
import cat.itacademy.blackjack.model.Card;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    CardResponseDTO toDto(Card card);

    List<CardResponseDTO> toDtoList(List<Card> cards);
}