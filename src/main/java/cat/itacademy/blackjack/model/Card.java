package cat.itacademy.blackjack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {

    private CardSuit suit;
    private CardValue value;

    public int getPoints() {
        return value.getPoints();
    }

    public String getLabel() {
        return value.getLabel() + " of " + suit.name().charAt(0) + suit.name().substring(1).toLowerCase();
    }
}