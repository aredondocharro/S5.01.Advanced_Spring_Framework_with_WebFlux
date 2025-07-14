package cat.itacademy.blackjack.model;

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

    private String playerId;
    private LocalDateTime createdAt;
    private GameStatus status;
    private int playerScore;
    private int dealerScore;
    private String deckJson;

    @Transient
    private List<Card> playerCards;

    @Transient
    private List<Card> dealerCards;
}
