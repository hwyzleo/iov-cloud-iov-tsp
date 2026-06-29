-- 添加 activate_time 字段到 tb_tbox 表（如果不存在）
-- 使用存储过程来检查列是否存在
DELIMITER //

CREATE PROCEDURE add_column_if_not_exists()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO column_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_tbox'
    AND COLUMN_NAME = 'activate_time';
    
    IF column_exists = 0 THEN
        ALTER TABLE tb_tbox ADD COLUMN activate_time DATETIME NULL COMMENT '首次成功接入（激活）时间，set-once；空 = 尚未激活';
    END IF;
END //

DELIMITER ;

CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- 存量回填：所有存量 TBOX 统一 device_status = PRE_ACTIVE (1)、activate_time = NULL
UPDATE tb_tbox SET device_status = 1 WHERE device_status IS NULL;
