CREATE TABLE IF NOT EXISTS games (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  player_id VARCHAR(255),
  created_at DATETIME,
  status VARCHAR(255),
  player_score INT,
  dealer_score INT,
  deck_json TEXT
);