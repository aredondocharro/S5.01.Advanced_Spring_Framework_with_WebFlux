package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.PlayerRankingResponse;
import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlayerService {
    Mono<PlayerResponse> create(PlayerRequest request);
    Mono<PlayerResponse> findByName(String name);
    Mono<PlayerResponse> findById(String id);
    Mono<Void> deleteById(String id);
    Flux<PlayerResponse> findAll();
    Flux<PlayerRankingResponse> getRanking();
    Mono<PlayerResponse> updatePlayerName(String id, String newName);
}
