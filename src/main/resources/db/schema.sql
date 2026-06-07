CREATE DATABASE  IF NOT EXISTS `supershop` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;
USE `supershop`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: supershop
-- ------------------------------------------------------
-- Server version	5.7.35-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cart_item`
--

DROP TABLE IF EXISTS `cart_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_item`
--

LOCK TABLES `cart_item` WRITE;
/*!40000 ALTER TABLE `cart_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (3,'数码电器',1,1,'2026-06-07 15:07:54','2026-06-07 15:07:54'),(4,'家居生活',2,1,'2026-06-07 15:07:54','2026-06-07 15:07:54'),(5,'美妆护肤',3,1,'2026-06-07 15:07:54','2026-06-07 15:07:54'),(6,'服饰鞋包',4,1,'2026-06-07 15:07:54','2026-06-07 15:07:54'),(7,'食品生鲜',5,1,'2026-06-07 15:07:54','2026-06-07 15:07:54'),(8,'运动户外',6,1,'2026-06-07 15:07:54','2026-06-07 15:07:54'),(9,'图书文具',7,1,'2026-06-07 15:07:54','2026-06-07 15:07:54'),(10,'母婴玩具',8,1,'2026-06-07 15:07:54','2026-06-07 15:07:54');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mq_dead_letter`
--

DROP TABLE IF EXISTS `mq_dead_letter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mq_dead_letter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型，如 SECKILL_ORDER',
  `request_id` varchar(64) DEFAULT NULL COMMENT '秒杀 requestId',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `product_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  `message_body` text NOT NULL COMMENT '原始消息 JSON',
  `fail_reason` varchar(512) DEFAULT NULL COMMENT '失败原因摘要',
  `retry_count` int(11) NOT NULL DEFAULT '0' COMMENT '重试次数',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0待处理 1处理中 2已处理',
  `handler_admin_id` bigint(20) DEFAULT NULL COMMENT '处理人管理员ID',
  `handle_remark` varchar(512) DEFAULT NULL COMMENT '处理备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '进入死信时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_create_time` (`status`,`create_time`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_user_product` (`user_id`,`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='MQ死信消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mq_dead_letter`
--

LOCK TABLES `mq_dead_letter` WRITE;
/*!40000 ALTER TABLE `mq_dead_letter` DISABLE KEYS */;
INSERT INTO `mq_dead_letter` VALUES (1,'SECKILL_ORDER',NULL,NULL,NULL,'{\r\n  \"userId\": 1,\r\n  \"productId\": 1001,\r\n  \"quantity\": 1,\r\n  \"requestId\": \"testdlq001\"\r\n}','消息体解析失败',0,2,3,'已人工核对','2026-06-06 00:23:02','2026-06-06 00:33:06','2026-06-06 00:33:06');
/*!40000 ALTER TABLE `mq_dead_letter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order`
--

DROP TABLE IF EXISTS `order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '0待支付 1已支付 2已发货 3已完成 4已取消',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order`
--

LOCK TABLES `order` WRITE;
/*!40000 ALTER TABLE `order` DISABLE KEYS */;
INSERT INTO `order` VALUES (2,'2026052822445514945',1,245.00,2,'2026-05-28 22:44:55','2026-05-29 11:38:57'),(3,'2026053011420215008',1,12.00,4,'2026-05-30 11:42:02','2026-05-30 11:42:31'),(4,'2026053011425412243',1,12.00,4,'2026-05-30 11:42:55','2026-05-30 11:43:10'),(5,'2026053016560016024',1,12.00,4,'2026-05-30 16:56:01','2026-05-30 17:10:27'),(6,'2026053019514817251',1,12.00,4,'2026-05-30 19:51:49','2026-05-30 20:07:16'),(7,'2026053019595115918',1,5.00,4,'2026-05-30 19:59:51','2026-05-30 20:15:16'),(8,'2026053022261418584',1,12.00,1,'2026-05-30 22:26:14','2026-05-30 22:26:45');
/*!40000 ALTER TABLE `order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_item`
--

DROP TABLE IF EXISTS `order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(20) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `product_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `quantity` int(11) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_item`
--

LOCK TABLES `order_item` WRITE;
/*!40000 ALTER TABLE `order_item` DISABLE KEYS */;
INSERT INTO `order_item` VALUES (3,2,3,'manbo','',122.00,2,244.00),(4,2,5,'鸭腿','',1.00,1,1.00),(5,3,2,'手机','',12.00,1,12.00),(6,4,2,'手机','',12.00,1,12.00),(7,5,2,'手机','',12.00,1,12.00),(8,6,2,'手机','',12.00,1,12.00),(9,7,4,'奥利奥','',5.00,1,5.00),(10,8,2,'手机','',12.00,1,12.00);
/*!40000 ALTER TABLE `order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `category_id` bigint(20) NOT NULL COMMENT '分类ID',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `stock` int(11) NOT NULL DEFAULT '0' COMMENT '库存',
  `image` varchar(255) DEFAULT NULL COMMENT '商品主图',
  `description` varchar(500) DEFAULT NULL COMMENT '商品描述',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 1上架 0下架',
  `is_flash_sale` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否秒杀 1是 0否',
  `flash_price` decimal(10,2) DEFAULT NULL COMMENT '秒杀价',
  `flash_stock` int(11) DEFAULT NULL COMMENT '秒杀库存',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `flash_start_time` datetime DEFAULT NULL COMMENT '秒杀开始时间',
  `flash_end_time` datetime DEFAULT NULL COMMENT '秒杀结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_flash_sale` (`is_flash_sale`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (9,'高性能游戏本',3,6999.00,120,'https://picsum.photos/seed/digital-laptop/400/400','轻薄高性能，适合办公与游戏',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(10,'降噪蓝牙耳机',3,899.00,350,'https://picsum.photos/seed/digital-earphone/400/400','主动降噪，长续航，通勤必备',1,1,499.00,80,'2026-06-07 15:07:54','2026-06-07 15:07:54','2026-06-07 14:07:54','2026-06-07 23:07:54'),(11,'智能运动手表',3,1099.00,95,'https://picsum.photos/seed/digital-watch/400/400','心率监测、睡眠分析、消息提醒',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(12,'27英寸4K显示器',3,2399.00,60,'https://picsum.photos/seed/digital-monitor/400/400','IPS 面板，低蓝光护眼',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(13,'人体工学办公椅',4,888.00,45,'https://picsum.photos/seed/home-chair/400/400','腰托可调，久坐更舒适',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(14,'北欧风护眼台灯',4,159.00,200,'https://picsum.photos/seed/home-lamp/400/400','三档色温，无频闪',1,1,99.00,50,'2026-06-07 15:07:54','2026-06-07 15:07:54','2026-06-07 14:37:54','2026-06-07 21:07:54'),(15,'记忆棉护颈枕',4,199.00,180,'https://picsum.photos/seed/home-pillow/400/400','慢回弹，贴合颈椎曲线',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(16,'免手洗旋转拖把',4,89.00,260,'https://picsum.photos/seed/home-mop/400/400','脱水省力，适合家庭清洁',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(17,'玻尿酸保湿面霜',5,168.00,300,'https://picsum.photos/seed/beauty-cream/400/400','深层补水，改善干燥',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(18,'清爽防晒喷雾',5,79.00,420,'https://picsum.photos/seed/beauty-sunscreen/400/400','SPF50+，轻薄不闷',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(19,'丝绒哑光口红套装',5,299.00,150,'https://picsum.photos/seed/beauty-lipstick/400/400','三支热门色号，礼盒装',1,1,199.00,40,'2026-06-07 15:07:54','2026-06-07 15:07:54','2026-06-07 15:15:54','2026-06-08 01:07:54'),(20,'氨基酸洁面乳',5,59.00,500,'https://picsum.photos/seed/beauty-cleanser/400/400','温和清洁，洗后不紧绷',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(21,'轻薄羽绒服',6,599.00,110,'https://picsum.photos/seed/fashion-coat/400/400','90% 白鸭绒，便携收纳',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(22,'经典休闲帆布鞋',6,259.00,220,'https://picsum.photos/seed/fashion-shoes/400/400','百搭款式，透气舒适',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(23,'真皮双肩背包',6,399.00,85,'https://picsum.photos/seed/fashion-bag/400/400','大容量，适合通勤与短途出行',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(24,'纯棉基础款T恤',6,89.00,600,'https://picsum.photos/seed/fashion-tshirt/400/400','多色可选，亲肤透气',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(25,'每日坚果礼盒',7,128.00,240,'https://picsum.photos/seed/food-nuts/400/400','7 种坚果搭配，独立小包装',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(26,'原切牛排套餐',7,198.00,120,'https://picsum.photos/seed/food-steak/400/400','西冷+眼肉组合，冷链配送',1,1,128.00,35,'2026-06-07 15:07:54','2026-06-07 15:07:54','2026-06-07 13:07:54','2026-06-07 20:07:54'),(27,'有机红富士苹果',7,49.90,800,'https://picsum.photos/seed/food-apple/400/400','脆甜多汁，产地直供',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(28,'精品挂耳咖啡',7,68.00,360,'https://picsum.photos/seed/food-coffee/400/400','中深烘焙，香气浓郁',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(29,'加厚防滑瑜伽垫',8,79.00,320,'https://picsum.photos/seed/sport-yoga/400/400','10mm 厚度，回弹好',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(30,'轻量缓震跑鞋',8,459.00,90,'https://picsum.photos/seed/sport-running/400/400','透气网面，日常训练适用',1,1,299.00,25,'2026-06-07 15:07:54','2026-06-07 15:07:54','2026-06-07 14:07:54','2026-06-08 03:07:54'),(31,'双人露营帐篷',8,699.00,55,'https://picsum.photos/seed/sport-tent/400/400','防风防雨，快速搭建',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(32,'运动保温水杯',8,39.00,480,'https://picsum.photos/seed/sport-bottle/400/400','500ml，保冷保热',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(33,'Java 编程思想',9,128.00,150,'https://picsum.photos/seed/book-java/400/400','经典 Java 入门与进阶读物',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(34,'中性签字笔套装',9,29.00,900,'https://picsum.photos/seed/book-pen/400/400','12 支装，书写顺滑',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(35,'A5 活页笔记本',9,35.00,700,'https://picsum.photos/seed/book-notebook/400/400','可替换内芯，学习办公皆宜',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(36,'儿童启蒙绘本',9,45.00,280,'https://picsum.photos/seed/book-picture/400/400','亲子共读，色彩丰富',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(37,'轻便可折叠婴儿推车',10,899.00,40,'https://picsum.photos/seed/baby-stroller/400/400','一键收车，出行方便',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(38,'大颗粒积木玩具',10,129.00,210,'https://picsum.photos/seed/baby-blocks/400/400','安全材质，开发创造力',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL),(39,'儿童三轮滑板车',10,199.00,100,'https://picsum.photos/seed/baby-scooter/400/400','可调节高度，稳定安全',1,1,129.00,30,'2026-06-07 15:07:54','2026-06-07 15:07:54','2026-06-07 15:12:54','2026-06-08 00:07:54'),(40,'宽口径安抚奶瓶',10,69.00,330,'https://picsum.photos/seed/baby-bottle/400/400','防胀气设计，易清洗',1,0,NULL,NULL,'2026-06-07 15:07:54','2026-06-07 15:07:54',NULL,NULL);
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `seckill_order`
--

DROP TABLE IF EXISTS `seckill_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seckill_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `product_name` varchar(255) NOT NULL,
  `product_image` varchar(500) DEFAULT NULL,
  `seckill_price` decimal(10,2) NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT '1',
  `total_amount` decimal(10,2) NOT NULL,
  `status` int(11) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seckill_order`
--

LOCK TABLES `seckill_order` WRITE;
/*!40000 ALTER TABLE `seckill_order` DISABLE KEYS */;
INSERT INTO `seckill_order` VALUES (1,'SK202606011855241283c85a8',1,6,'ces','',21.00,1,21.00,2,'2026-06-01 18:55:25','2026-06-01 22:18:11'),(2,'SK202606011856401eb0b88d6',1,6,'ces','',21.00,1,21.00,2,'2026-06-01 18:56:41','2026-06-01 22:18:11'),(3,'SK202606011857221a96174a5',1,6,'ces','',21.00,1,21.00,2,'2026-06-01 18:57:23','2026-06-01 22:18:11'),(4,'SK20260601210313159146eb2',1,6,'ces','',21.00,1,21.00,1,'2026-06-01 21:03:14','2026-06-01 21:03:24'),(5,'SK2026060411484218dea8f52',1,5,'鸭腿','',2.00,1,2.00,2,'2026-06-04 11:48:43','2026-06-04 12:03:50'),(6,'SK2026060412031817bb1933e',1,7,'蛋dand','',12.00,1,12.00,2,'2026-06-04 12:03:19','2026-06-04 12:18:54'),(7,'SK2026060712002815a35e573',1,8,'ts','',2.00,1,2.00,2,'2026-06-07 12:00:29','2026-06-07 12:15:58');
/*!40000 ALTER TABLE `seckill_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（加密后）',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `role` varchar(20) DEFAULT 'USER' COMMENT '角色：USER-普通用户 ADMIN-管理员',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-禁用 1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'lao55','$2a$10$cci.ZseL67Q98DdW0rTKBeOT84TQTnviby0uMWxJaIbKTgBW.47rW','3318237611@qq.com','lao55',NULL,'USER',1,'2026-05-19 23:27:42','2026-05-19 23:27:42'),(2,'admintest','$2a$10$qFWpP8vziqrAQXGlf0CBZ.pbWEleueYXi7.5I4GizDMLzCDGwtXMG',NULL,'admintest',NULL,'ADMIN',1,'2026-05-20 23:57:43','2026-05-20 23:57:43'),(3,'admin','$2a$10$sTeoyFT8EpB7lpYGufjFDeD.tgz5E9/Yl7MgxSCY0Rhi8xAuyNB.e',NULL,'admin',NULL,'ADMIN',1,'2026-05-20 23:59:17','2026-05-20 23:59:17'),(4,'ade','$2a$10$2dDi5ISSg23REDJGFBrl1OZoWCWdnM2kWyh2ug4gEbXvr0Tc8sQBW',NULL,'ade',NULL,'ADMIN',1,'2026-05-21 10:05:39','2026-05-21 10:05:39');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-07 16:24:50
