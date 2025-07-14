package cat.itacademy.blackjack.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {
    @Id
    private String id;

    private String name;
    private int totalScore;
    private int gamesPlayed;
    private int gamesWon;

    private LocalDateTime createdAt;
}