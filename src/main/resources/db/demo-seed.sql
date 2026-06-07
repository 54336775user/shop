-- 演示数据：8 个分类 + 32 个商品（含 6 个秒杀商品）
-- 适用空库初始化；若已有分类请先备份，或仅在新库执行。
-- 也可直接重启后端，由 DemoDataSeedRunner 在分类表为空时自动写入。

USE supershop;

SET NAMES utf8mb4;

INSERT INTO category (name, sort, status, create_time, update_time) VALUES
('数码电器', 1, 1, NOW(), NOW()),
('家居生活', 2, 1, NOW(), NOW()),
('美妆护肤', 3, 1, NOW(), NOW()),
('服饰鞋包', 4, 1, NOW(), NOW()),
('食品生鲜', 5, 1, NOW(), NOW()),
('运动户外', 6, 1, NOW(), NOW()),
('图书文具', 7, 1, NOW(), NOW()),
('母婴玩具', 8, 1, NOW(), NOW());

INSERT INTO product (
    name, category_id, price, stock, image, description,
    status, is_flash_sale, flash_price, flash_stock, flash_start_time, flash_end_time,
    create_time, update_time
) VALUES
('高性能游戏本', 1, 6999, 120, 'https://picsum.photos/seed/digital-laptop/400/400', '轻薄高性能，适合办公与游戏', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('降噪蓝牙耳机', 1, 899, 350, 'https://picsum.photos/seed/digital-earphone/400/400', '主动降噪，长续航，通勤必备', 1, 1, 499, 80, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 8 HOUR), NOW(), NOW()),
('智能运动手表', 1, 1099, 95, 'https://picsum.photos/seed/digital-watch/400/400', '心率监测、睡眠分析、消息提醒', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('27英寸4K显示器', 1, 2399, 60, 'https://picsum.photos/seed/digital-monitor/400/400', 'IPS 面板，低蓝光护眼', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),

('人体工学办公椅', 2, 888, 45, 'https://picsum.photos/seed/home-chair/400/400', '腰托可调，久坐更舒适', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('北欧风护眼台灯', 2, 159, 200, 'https://picsum.photos/seed/home-lamp/400/400', '三档色温，无频闪', 1, 1, 99, 50, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 6 HOUR), NOW(), NOW()),
('记忆棉护颈枕', 2, 199, 180, 'https://picsum.photos/seed/home-pillow/400/400', '慢回弹，贴合颈椎曲线', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('免手洗旋转拖把', 2, 89, 260, 'https://picsum.photos/seed/home-mop/400/400', '脱水省力，适合家庭清洁', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),

('玻尿酸保湿面霜', 3, 168, 300, 'https://picsum.photos/seed/beauty-cream/400/400', '深层补水，改善干燥', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('清爽防晒喷雾', 3, 79, 420, 'https://picsum.photos/seed/beauty-sunscreen/400/400', 'SPF50+，轻薄不闷', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('丝绒哑光口红套装', 3, 299, 150, 'https://picsum.photos/seed/beauty-lipstick/400/400', '三支热门色号，礼盒装', 1, 1, 199, 40, DATE_ADD(NOW(), INTERVAL 8 MINUTE), DATE_ADD(NOW(), INTERVAL 10 HOUR), NOW(), NOW()),
('氨基酸洁面乳', 3, 59, 500, 'https://picsum.photos/seed/beauty-cleanser/400/400', '温和清洁，洗后不紧绷', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),

('轻薄羽绒服', 4, 599, 110, 'https://picsum.photos/seed/fashion-coat/400/400', '90% 白鸭绒，便携收纳', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('经典休闲帆布鞋', 4, 259, 220, 'https://picsum.photos/seed/fashion-shoes/400/400', '百搭款式，透气舒适', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('真皮双肩背包', 4, 399, 85, 'https://picsum.photos/seed/fashion-bag/400/400', '大容量，适合通勤与短途出行', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('纯棉基础款T恤', 4, 89, 600, 'https://picsum.photos/seed/fashion-tshirt/400/400', '多色可选，亲肤透气', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),

('每日坚果礼盒', 5, 128, 240, 'https://picsum.photos/seed/food-nuts/400/400', '7 种坚果搭配，独立小包装', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('原切牛排套餐', 5, 198, 120, 'https://picsum.photos/seed/food-steak/400/400', '西冷+眼肉组合，冷链配送', 1, 1, 128, 35, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR), NOW(), NOW()),
('有机红富士苹果', 5, 49.9, 800, 'https://picsum.photos/seed/food-apple/400/400', '脆甜多汁，产地直供', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('精品挂耳咖啡', 5, 68, 360, 'https://picsum.photos/seed/food-coffee/400/400', '中深烘焙，香气浓郁', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),

('加厚防滑瑜伽垫', 6, 79, 320, 'https://picsum.photos/seed/sport-yoga/400/400', '10mm 厚度，回弹好', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('轻量缓震跑鞋', 6, 459, 90, 'https://picsum.photos/seed/sport-running/400/400', '透气网面，日常训练适用', 1, 1, 299, 25, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 12 HOUR), NOW(), NOW()),
('双人露营帐篷', 6, 699, 55, 'https://picsum.photos/seed/sport-tent/400/400', '防风防雨，快速搭建', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('运动保温水杯', 6, 39, 480, 'https://picsum.photos/seed/sport-bottle/400/400', '500ml，保冷保热', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),

('Java 编程思想', 7, 128, 150, 'https://picsum.photos/seed/book-java/400/400', '经典 Java 入门与进阶读物', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('中性签字笔套装', 7, 29, 900, 'https://picsum.photos/seed/book-pen/400/400', '12 支装，书写顺滑', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('A5 活页笔记本', 7, 35, 700, 'https://picsum.photos/seed/book-notebook/400/400', '可替换内芯，学习办公皆宜', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('儿童启蒙绘本', 7, 45, 280, 'https://picsum.photos/seed/book-picture/400/400', '亲子共读，色彩丰富', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),

('轻便可折叠婴儿推车', 8, 899, 40, 'https://picsum.photos/seed/baby-stroller/400/400', '一键收车，出行方便', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('大颗粒积木玩具', 8, 129, 210, 'https://picsum.photos/seed/baby-blocks/400/400', '安全材质，开发创造力', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW()),
('儿童三轮滑板车', 8, 199, 100, 'https://picsum.photos/seed/baby-scooter/400/400', '可调节高度，稳定安全', 1, 1, 129, 30, DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_ADD(NOW(), INTERVAL 9 HOUR), NOW(), NOW()),
('宽口径安抚奶瓶', 8, 69, 330, 'https://picsum.photos/seed/baby-bottle/400/400', '防胀气设计，易清洗', 1, 0, NULL, NULL, NULL, NULL, NOW(), NOW());
