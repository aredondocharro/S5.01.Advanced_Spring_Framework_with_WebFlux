CREATE TABLE IF NOT EXISTS games (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    turn VARCHAR(50) NOT NULL,
    player_score INT NOT NULL,
    dealer_score INT NOT NULL,
    deck_json TEXT NOT NULL
);