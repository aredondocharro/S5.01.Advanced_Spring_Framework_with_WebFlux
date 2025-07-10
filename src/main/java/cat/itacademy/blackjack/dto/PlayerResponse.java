package cat.itacademy.blackjack.dto;

import java.time.LocalDateTime;

public record PlayerResponse(
        String id,
        String name,
        int totalScore,
        LocalDateTime createdAt
) {}
