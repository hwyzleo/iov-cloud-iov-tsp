-- 添加 activate_time 字段到 tb_tbox 表
ALTER TABLE tb_tbox ADD COLUMN activate_time DATETIME NULL COMMENT '首次成功接入（激活）时间，set-once；空 = 尚未激活';

-- 存量回填：所有存量 TBOX 统一 device_status = PRE_ACTIVE (1)、activate_time = NULL
UPDATE tb_tbox SET device_status = 1 WHERE device_status IS NULL;