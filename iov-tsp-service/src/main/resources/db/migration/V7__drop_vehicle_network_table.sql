-- V7__drop_vehicle_network_table.sql
-- 删除旧表 tb_vehicle_network 和 tb_vehicle_network_log
-- 注意：此脚本应在确认数据迁移完成后执行

DROP TABLE IF EXISTS tb_vehicle_network_log;
DROP TABLE IF EXISTS tb_vehicle_network;
