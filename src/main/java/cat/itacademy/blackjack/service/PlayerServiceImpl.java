package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    @Override
    public Mono<PlayerResponse> create(PlayerRequest request) {
        Player player = new Player();
        player.setName(request.name());
        player.setTotalScore(0);
        player.setCreatedAt(LocalDateTime.now());
        return playerRepository.save(player)
                .map(this::toResponse);
    }

    @Override
    public Mono<PlayerResponse> findById(String id) {
        return playerRepository.findById(id)
                .map(this::toResponse);
    }

    @Override
    public Flux<PlayerResponse> findAll() {
        return playerRepository.findAll()
                .map(this::toResponse);
    }

    @Override
    public Flux<PlayerResponse> getRanking() {
        return playerRepository.findAll()
                .sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()))
                .map(this::toResponse);
    }

    private PlayerResponse toResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getName(),
                player.getTotalScore(),
                player.getCreatedAt()
        );
    }
}
