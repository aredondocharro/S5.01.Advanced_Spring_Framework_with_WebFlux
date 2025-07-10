package cat.itacademy.blackjack.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    private String suit;
    private String value;
}