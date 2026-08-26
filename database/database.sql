-- 全量建表（新库执行一次）。
-- 容器首次启动：compose 会自动执行本文件，再执行 seed.sql。
-- 已有旧库补列/补表：执行 migrate.sql。
-- 演示账号：见 seed.sql（密码均为 123456）。
SET NAMES utf8mb4;

/*
用户表
*/
CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(50),
  avatar VARCHAR(255),
  bio TEXT,
  role INT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS videos (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  author_id INT NOT NULL,
  cover_url VARCHAR(255),
  video_url VARCHAR(255),
  status INT DEFAULT 0,
  report_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS live_rooms (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  anchor_id INT NOT NULL,
  stream_key VARCHAR(100),
  is_live BOOLEAN DEFAULT FALSE,
  session_start DATETIME,
  ended_at DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (anchor_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS subscriptions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  follower_id INT NOT NULL,
  target_id INT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (follower_id) REFERENCES users(id),
  FOREIGN KEY (target_id) REFERENCES users(id),
  UNIQUE KEY uk_follow (follower_id, target_id)
);

CREATE TABLE IF NOT EXISTS comments (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  target_id INT NOT NULL,
  target_type INT NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS play_history (
  user_id INT NOT NULL,
  video_id INT NOT NULL,
  progress INT DEFAULT 0,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, video_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (video_id) REFERENCES videos(id)
);

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
  is_read BOOLEAN DEFAULT FALSE,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (actor_id) REFERENCES users(id)
);

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

-- 已有库增量升级（按需执行）：
-- ALTER TABLE users ADD COLUMN bio TEXT AFTER avatar;
-- ALTER TABLE videos ADD COLUMN report_count INT DEFAULT 0 AFTER status;
-- CREATE TABLE video_reports (...见上...);

-- 历史账号：将 role=0 的用户升级为发布者（管理员 role=2 不受影响）
-- UPDATE users SET role = 1 WHERE role IS NULL OR role < 1;

-- INSERT INTO videos (title, description, author_id, cover_url, video_url, status)
-- VALUES ('示例视频', '用于联调播放列表', 1, NULL, 'https://www.w3schools.com/html/mov_bbb.mp4', 1);
