CREATE DATABASE IF NOT EXISTS doinb_msg
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE doinb_msg;

CREATE TABLE IF NOT EXISTS notifications (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  type INT NOT NULL,
  actor_id INT NOT NULL,
  ref_id INT,
  preview VARCHAR(255),
  link_path VARCHAR(255),
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notifications_user_time (user_id, create_time),
  INDEX idx_notifications_user_read (user_id, is_read)
);

CREATE TABLE IF NOT EXISTS dm_rooms (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_a INT NOT NULL,
  user_b INT NOT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dm_rooms_users (user_a, user_b)
);

CREATE TABLE IF NOT EXISTS dm_messages (
  id INT PRIMARY KEY AUTO_INCREMENT,
  room_id INT NOT NULL,
  sender_id INT NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dm_messages_room_time (room_id, create_time),
  CONSTRAINT fk_dm_messages_room FOREIGN KEY (room_id) REFERENCES dm_rooms(id)
);
