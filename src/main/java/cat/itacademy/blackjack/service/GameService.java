package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.model.Games;
import reactor.core.publisher.Mono;

public interface GameService {

    Mono<Games> createGame(String playerName);
}