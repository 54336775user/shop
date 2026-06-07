# Shop 电商秒杀系统

Spring Boot + Vue 3 前后端分离电商项目，支持商品浏览、购物车、订单、秒杀、后台管理等。

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+
- MySQL 8.0
- Redis
- RabbitMQ

## 快速开始

### 1. 克隆项目

cd shop

### 2. 初始化数据库

CREATE DATABASE supershop DEFAULT CHARACTER SET utf8mb4;
-- 执行 src/main/resources/db/schema.sql（建表）
-- 可选：执行 demo-seed.sql 或启动后端自动写入演示数据

### 3. 修改配置

编辑 src/main/resources/application.yml，修改 MySQL 用户名和密码。

### 4. 启动后端

mvn spring-boot:run
# 或在 IDE 中运行 ShopApplication.java
# 后端地址：http://localhost:8080

### 5. 启动前端

cd frontend
npm install
npm run dev
# 前端地址：http://localhost:5173

