package cat.itacademy.blackjack.service;

import cat.itacademy.blackjack.dto.PlayerRequest;
import cat.itacademy.blackjack.dto.PlayerResponse;
import cat.itacademy.blackjack.mapper.PlayerMapper;
import cat.itacademy.blackjack.model.Player;
import cat.itacademy.blackjack.repository.mongo.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    @Override
    public Mono<PlayerResponse> create(PlayerRequest request) {
        Player player = playerMapper.toEntity(request);
        return playerRepository.save(player)
                .map(playerMapper::toResponse);
    }

    @Override
    public Mono<PlayerResponse> findById(String id) {
        return playerRepository.findById(id)
                .map(playerMapper::toResponse);
    }

    @Override
    public Flux<PlayerResponse> findAll() {
        return playerRepository.findAll()
                .map(playerMapper::toResponse);
    }

    @Override
    public Flux<PlayerResponse> getRanking() {
        return playerRepository.findAll()
                .sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()))
                .map(playerMapper::toResponse);
    }
}

