CREATE TABLE IF NOT EXISTS games (
    id BIGSERIAL PRIMARY KEY,
    player_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    turn VARCHAR(50) NOT NULL,
    player_score INT NOT NULL,
    dealer_score INT NOT NULL,
    deck_json TEXT NOT NULL,
    player_cards_json TEXT NOT NULL,
    dealer_cards_json TEXT NOT NULL
);

INSERT INTO games (player_id, created_at, status, turn, player_score, dealer_score, deck_json, player_cards_json, dealer_cards_json)
VALUES ('testPlayer', CURRENT_TIMESTAMP, 'IN_PROGRESS', 'PLAYER_TURN', 15, 10, '[]', '[]', '[]');