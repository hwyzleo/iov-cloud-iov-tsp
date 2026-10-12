-- V9__add_tbox_login_time.sql
-- TSP-DSN-CR-008: TBOX 两阶段注册激活——tb_tbox 新增首次/最近登录时间
-- 1) first_login_time：首次 mTLS + US-017 ALLOW 成功建链时间，set-once
-- 2) last_login_time：最近一次成功建链时间，按 max(existing, event.ts) 单调更新
-- 3) 兼容规则：历史 ACTIVE + activate_time 记录保持 ACTIVE 不回退，以旧 activate_time 回填 first/last login
-- MySQL 8.0 不支持 ADD COLUMN / CREATE INDEX IF NOT EXISTS，使用存储过程守卫幂等

DROP PROCEDURE IF EXISTS tsp_add_tbox_login_time;
DELIMITER $$
CREATE PROCEDURE tsp_add_tbox_login_time()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND column_name = 'first_login_time') THEN
        ALTER TABLE tb_tbox ADD COLUMN first_login_time DATETIME NULL COMMENT '首次 mTLS + US-017 ALLOW 成功建链时间，set-once';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND column_name = 'last_login_time') THEN
        ALTER TABLE tb_tbox ADD COLUMN last_login_time DATETIME NULL COMMENT '最近一次成功建链时间，按 max(existing, event.ts) 单调更新';
    END IF;

    -- 长期未登录识别支撑索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'idx_tbox_last_login_time') THEN
        CREATE INDEX idx_tbox_last_login_time ON tb_tbox(last_login_time);
    END IF;
END$$
DELIMITER ;

CALL tsp_add_tbox_login_time();
DROP PROCEDURE tsp_add_tbox_login_time;

-- 存量兼容回填：以旧 activate_time 回填 first_login_time / last_login_time，仅回填尚未写入的记录
UPDATE tb_tbox
SET first_login_time = activate_time,
    last_login_time  = activate_time
WHERE activate_time IS NOT NULL
  AND first_login_time IS NULL
  AND last_login_time IS NULL;
