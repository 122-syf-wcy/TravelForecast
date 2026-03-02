-- 为门票订单表添加条形码字段
ALTER TABLE ticket_orders ADD COLUMN barcode VARCHAR(20) COMMENT '条形码（用于景区扫码检票）' AFTER remark;

-- 为已有订单生成条形码
UPDATE ticket_orders SET barcode = CONCAT('69', SUBSTRING(UNIX_TIMESTAMP() * 1000, -8), LPAD(FLOOR(RAND() * 1000), 3, '0')) WHERE barcode IS NULL;

-- 添加索引以便快速查询
CREATE INDEX idx_ticket_orders_barcode ON ticket_orders(barcode);
