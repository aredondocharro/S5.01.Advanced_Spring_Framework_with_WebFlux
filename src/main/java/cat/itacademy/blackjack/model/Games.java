package cat.itacademy.blackjack.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Table("games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Games {

    @Id
    private Long id;

    @NotNull(message = "Player ID must not be null")
    private String playerId;

    @NotNull(message = "Creation date must not be null")
    private LocalDateTime createdAt;

    @NotNull(message = "Status must not be null")
    private GameStatus status;

    private int playerScore;

    private int dealerScore;

    @NotNull(message = "Deck JSON must not be null")
    private String deckJson;

    @Transient
    private List<Card> playerCards;

    @Transient
    private List<Card> dealerCards;
}

