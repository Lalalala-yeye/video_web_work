-- 给「已经存在的旧库」补列、补表。可重复执行。
-- 新库请只跑 database.sql（再按需跑 seed.sql），不必先跑本文件。
-- 用法：mysql -uroot -p doinb < database/migrate.sql

ALTER TABLE users ADD COLUMN IF NOT EXISTS bio TEXT;

ALTER TABLE videos ADD COLUMN IF NOT EXISTS report_count INT DEFAULT 0;
ALTER TABLE videos ADD COLUMN IF NOT EXISTS create_time DATETIME DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE live_rooms ADD COLUMN IF NOT EXISTS session_start DATETIME;
ALTER TABLE live_rooms ADD COLUMN IF NOT EXISTS ended_at DATETIME;
ALTER TABLE live_rooms ADD COLUMN IF NOT EXISTS create_time DATETIME DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS video_reactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  video_id INT NOT NULL,
  reaction TINYINT NOT NULL,
  UNIQUE KEY uk_user_video (user_id, video_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (video_id) REFERENCES videos(id)
);

CREATE TABLE IF NOT EXISTS comment_reactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  comment_id INT NOT NULL,
  reaction TINYINT NOT NULL,
  UNIQUE KEY uk_user_comment (user_id, comment_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (comment_id) REFERENCES comments(id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  type INT NOT NULL,
  actor_id INT NOT NULL,
  ref_id INT,
  preview VARCHAR(255),
  link_path VARCHAR(255),
  is_read BOOLEAN DEFAULT FALSE,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (actor_id) REFERENCES users(id)
);

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS link_path VARCHAR(255);

CREATE TABLE IF NOT EXISTS dm_rooms (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_a INT NOT NULL,
  user_b INT NOT NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_users (user_a, user_b),
  FOREIGN KEY (user_a) REFERENCES users(id),
  FOREIGN KEY (user_b) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS dm_messages (
  id INT PRIMARY KEY AUTO_INCREMENT,
  room_id INT NOT NULL,
  sender_id INT NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (room_id) REFERENCES dm_rooms(id),
  FOREIGN KEY (sender_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS video_reports (
  id INT PRIMARY KEY AUTO_INCREMENT,
  video_id INT NOT NULL,
  reporter_id INT NOT NULL,
  reason VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (video_id) REFERENCES videos(id),
  FOREIGN KEY (reporter_id) REFERENCES users(id),
  UNIQUE KEY uk_video_reporter (video_id, reporter_id)
);

UPDATE users SET role = 1 WHERE role IS NULL OR role < 1;
