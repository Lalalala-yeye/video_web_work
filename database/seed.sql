-- 课设演示 / 换机器联调用测试数据。密码均为 123456（BCrypt）。
-- 容器首次启动会自动执行；本机已有库可：mysql -uroot -p doinb < database/seed.sql
-- 账号：demo_admin（管理员） / demo_author（作者） / demo_user（观众）
SET NAMES utf8mb4;

INSERT INTO users (id, username, password, nickname, bio, role) VALUES
(1, 'demo_admin', '$2a$10$LIL3kj3pxnYdaHDJ47W9BuK1yw88SHya9mC57CF4cjRE0asldiSCW', '演示管理员', '课设演示管理员，密码 123456', 2),
(2, 'demo_author', '$2a$10$LIL3kj3pxnYdaHDJ47W9BuK1yw88SHya9mC57CF4cjRE0asldiSCW', '演示作者', '发布示例视频的作者账号', 1),
(3, 'demo_user', '$2a$10$LIL3kj3pxnYdaHDJ47W9BuK1yw88SHya9mC57CF4cjRE0asldiSCW', '演示观众', '用于关注、评论、赞踩、私信', 1)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  bio = VALUES(bio),
  role = VALUES(role);

INSERT INTO videos (id, title, description, author_id, cover_url, video_url, status, report_count) VALUES
(1, '示例视频（已发布）', 'Big Buck Bunny，用于首页列表和播放页联调', 2, NULL, 'https://www.w3schools.com/html/mov_bbb.mp4', 1, 0),
(2, '待审核稿件', '管理员可在后台待审列表看到本条', 2, NULL, 'https://www.w3schools.com/html/mov_bbb.mp4', 0, 0)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  status = VALUES(status);

INSERT INTO live_rooms (id, title, anchor_id, stream_key, is_live) VALUES
(1, '演示直播间', 2, 'demo-stream', FALSE)
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO subscriptions (follower_id, target_id)
SELECT 3, 2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM subscriptions WHERE follower_id = 3 AND target_id = 2);

INSERT INTO comments (id, user_id, target_id, target_type, content) VALUES
(1, 3, 1, 1, '测试评论：这条会出现在示例视频下')
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO video_reactions (user_id, video_id, reaction)
SELECT 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM video_reactions WHERE user_id = 3 AND video_id = 1);

INSERT INTO dm_rooms (id, user_a, user_b) VALUES
(1, 2, 3)
ON DUPLICATE KEY UPDATE user_a = VALUES(user_a);

INSERT INTO dm_messages (room_id, sender_id, content)
SELECT 1, 2, '你好，这是一条演示私信'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dm_messages WHERE room_id = 1);

INSERT INTO notifications (user_id, type, actor_id, ref_id, preview, is_read)
SELECT 2, 1, 3, 1, '演示观众赞了你的视频', FALSE
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM notifications WHERE user_id = 2 AND type = 1 AND actor_id = 3 AND ref_id = 1
);
