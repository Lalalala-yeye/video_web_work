-- 清掉本机/演示库里 Selenium E2E 留下的账号和稿件。
-- 不会动 demo_* 演示账号，也不会动标题不含 E2E、用户名不以 e2e 开头的真实视频。
-- 用法（容器在跑时）：
--   docker exec -i doinb-mysql mysql -uroot -ptest --default-character-set=utf8mb4 doinb < database/clean-e2e.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM comment_reactions
 WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR comment_id IN (SELECT id FROM comments WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'e2e%'));

DELETE FROM video_reactions
 WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR video_id IN (SELECT id FROM videos WHERE title LIKE 'E2E%' OR author_id IN (SELECT id FROM users WHERE username LIKE 'e2e%'));

DELETE FROM play_history
 WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR video_id IN (SELECT id FROM videos WHERE title LIKE 'E2E%' OR author_id IN (SELECT id FROM users WHERE username LIKE 'e2e%'));

DELETE FROM video_reports
 WHERE reporter_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR video_id IN (SELECT id FROM videos WHERE title LIKE 'E2E%' OR author_id IN (SELECT id FROM users WHERE username LIKE 'e2e%'));

DELETE FROM notifications
 WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR actor_id IN (SELECT id FROM users WHERE username LIKE 'e2e%');

DELETE FROM dm_messages
 WHERE sender_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR room_id IN (
        SELECT id FROM dm_rooms
         WHERE user_a IN (SELECT id FROM users WHERE username LIKE 'e2e%')
            OR user_b IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    );

DELETE FROM dm_rooms
 WHERE user_a IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR user_b IN (SELECT id FROM users WHERE username LIKE 'e2e%');

DELETE FROM comments
 WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR (target_type = 1 AND target_id IN (SELECT id FROM videos WHERE title LIKE 'E2E%' OR author_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')));

DELETE FROM subscriptions
 WHERE follower_id IN (SELECT id FROM users WHERE username LIKE 'e2e%')
    OR target_id IN (SELECT id FROM users WHERE username LIKE 'e2e%');

DELETE FROM live_rooms
 WHERE anchor_id IN (SELECT id FROM users WHERE username LIKE 'e2e%');

DELETE FROM videos
 WHERE title LIKE 'E2E%'
    OR author_id IN (SELECT id FROM users WHERE username LIKE 'e2e%');

DELETE FROM users WHERE username LIKE 'e2e%';

SET FOREIGN_KEY_CHECKS = 1;
