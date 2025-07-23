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


        @Mapping(target = "suit", expression = "java(card.getSuit().name())")
        @Mapping(target = "value", expression = "java(card.getValue().name())")
        CardResponseDTO toDto(Card card);

        @Named("toDtoList")
        List<CardResponseDTO> toDtoList(List<Card> cards);
    }
