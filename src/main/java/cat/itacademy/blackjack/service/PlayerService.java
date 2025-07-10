package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlayerService {
    Mono<PlayerResponse> create(PlayerRequest request);
    Mono<PlayerResponse> findById(String id);
    Flux<PlayerResponse> findAll();
    Flux<PlayerResponse> getRanking();
}
