-- Destroy
DROP TABLE IF EXISTS items CASCADE;

CREATE TABLE IF NOT EXISTS items (
  id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  project VARCHAR(255) NOT NULL,
  repository VARCHAR(255) NOT NULL,
  username VARCHAR(512) NOT NULL,
  created TIMESTAMP DEFAULT NOW()
);