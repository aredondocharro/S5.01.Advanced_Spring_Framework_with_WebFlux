package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.model.Game;
import reactor.core.publisher.Mono;

public interface GameService {

    Mono<Game> createGame(String playerName);
}