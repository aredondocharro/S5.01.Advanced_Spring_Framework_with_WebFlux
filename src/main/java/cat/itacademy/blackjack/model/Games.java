package cat.itacademy.blackjack.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
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
    @Column("player_id")
    private String playerId;

    @NotNull
    @Column("created_at")
    private LocalDateTime createdAt;

    @NotNull
    @Column("status")
    private GameStatus status;

    @NotNull
    @Column("turn")
    private GameTurn turn;

    @Column("player_score")
    private int playerScore;

    @Column("dealer_score")
    private int dealerScore;

    @NotNull
    @Column("deck_json")
    private String deckJson;

    @NotNull
    @Column("player_cards_json")
    private String playerCardsJson;

    @NotNull
    @Column("dealer_cards_json")
    private String dealerCardsJson;

    @Transient
    private List<Card> playerCards;

    @Transient
    private List<Card> dealerCards;
}


