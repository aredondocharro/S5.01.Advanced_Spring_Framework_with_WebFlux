package cat.itacademy.blackjack.dto;

public record PlayerRankingResponse(
        String name,
        int gamesPlayed,
        int gamesWon,
        double winRate,
        int totalScore
) {}