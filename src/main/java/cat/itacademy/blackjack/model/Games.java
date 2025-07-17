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

    @NotNull
    private String playerId;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private GameStatus status;

    @NotNull
    private GameTurn turn;

    private int playerScore;
    private int dealerScore;

    @NotNull
    private String deckJson;

    @NotNull
    private String playerCardsJson;

    @NotNull
    private String dealerCardsJson;

    @Transient
    private List<Card> playerCards;

    @Transient
    private List<Card> dealerCards;
}


