-- TSP-DSN-CR-002: 删除 IDCM 设备表/绑定表/接口/异常，TSP 退出 IDCM 设计层
-- 落地 TSP-REQ-CR-002

-- 删除车辆-IDCM绑定日志表
DROP TABLE IF EXISTS tb_vehicle_idcm_log;

-- 删除车辆-IDCM绑定表
DROP TABLE IF EXISTS tb_vehicle_idcm;

-- 删除IDCM设备日志表
DROP TABLE IF EXISTS tb_idcm_log;

-- 删除IDCM设备表
DROP TABLE IF EXISTS tb_idcm;
