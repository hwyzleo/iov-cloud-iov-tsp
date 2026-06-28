-- V20260628__add_binding_projection_support.sql

-- 1. 修改 tb_vehicle_tbox 表，新增投影治理字段
ALTER TABLE tb_vehicle_tbox ADD COLUMN binding_id BIGINT COMMENT 'VMD binding_id，投影幂等主键';
ALTER TABLE tb_vehicle_tbox ADD COLUMN part_code VARCHAR(64) COMMENT 'VMD零件编码';
ALTER TABLE tb_vehicle_tbox ADD COLUMN vehicle_node_code VARCHAR(64) COMMENT '车载节点';
ALTER TABLE tb_vehicle_tbox ADD COLUMN device_category VARCHAR(16) DEFAULT 'TBOX' COMMENT '设备类别: TBOX/CCP';
ALTER TABLE tb_vehicle_tbox ADD COLUMN bind_state VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '绑定状态: ACTIVE/INACTIVE';
ALTER TABLE tb_vehicle_tbox ADD COLUMN binding_version BIGINT DEFAULT 0 COMMENT '绑定版本，映射VMD seq';
ALTER TABLE tb_vehicle_tbox ADD COLUMN last_event_time DATETIME COMMENT '最近事件时间';
ALTER TABLE tb_vehicle_tbox ADD COLUMN source VARCHAR(16) DEFAULT 'VMD' COMMENT '投影来源';

-- 添加唯一约束
ALTER TABLE tb_vehicle_tbox ADD UNIQUE INDEX uk_binding_id (binding_id);

-- 2. 修改 tb_vehicle_ccp 表，新增投影治理字段
ALTER TABLE tb_vehicle_ccp ADD COLUMN binding_id BIGINT COMMENT 'VMD binding_id，投影幂等主键';
ALTER TABLE tb_vehicle_ccp ADD COLUMN part_code VARCHAR(64) COMMENT 'VMD零件编码';
ALTER TABLE tb_vehicle_ccp ADD COLUMN vehicle_node_code VARCHAR(64) COMMENT '车载节点';
ALTER TABLE tb_vehicle_ccp ADD COLUMN device_category VARCHAR(16) DEFAULT 'CCP' COMMENT '设备类别: TBOX/CCP';
ALTER TABLE tb_vehicle_ccp ADD COLUMN bind_state VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '绑定状态: ACTIVE/INACTIVE';
ALTER TABLE tb_vehicle_ccp ADD COLUMN binding_version BIGINT DEFAULT 0 COMMENT '绑定版本，映射VMD seq';
ALTER TABLE tb_vehicle_ccp ADD COLUMN last_event_time DATETIME COMMENT '最近事件时间';
ALTER TABLE tb_vehicle_ccp ADD COLUMN source VARCHAR(16) DEFAULT 'VMD' COMMENT '投影来源';

-- 添加唯一约束
ALTER TABLE tb_vehicle_ccp ADD UNIQUE INDEX uk_binding_id (binding_id);

-- 3. 一次性 bootstrap 对账回填 binding_id（示例，实际需要根据VMD数据调整）
-- UPDATE tb_vehicle_tbox SET binding_id = <vmd_binding_id> WHERE binding_id IS NULL;
-- UPDATE tb_vehicle_ccp SET binding_id = <vmd_binding_id> WHERE binding_id IS NULL;
