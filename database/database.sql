/*
用户表
*/
CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(50),
  avatar VARCHAR(255),
  role INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS videos (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  author_id INT NOT NULL,
  cover_url VARCHAR(255),
  video_url VARCHAR(255),
  status INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS live_rooms (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  anchor_id INT NOT NULL,
  stream_key VARCHAR(100),
  is_live BOOLEAN DEFAULT FALSE,
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

-- 可选：测试用示例数据（需先有 users 表中的 author，例如 id=1）
-- INSERT INTO videos (title, description, author_id, cover_url, video_url, status)
-- VALUES ('示例视频', '用于联调播放列表', 1, NULL, 'https://www.w3schools.com/html/mov_bbb.mp4', 1);
