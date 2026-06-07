package com.eample.shop.runer;

import com.eample.shop.Service.SeckillService;
import com.eample.shop.cache.ProductCacheService;
import com.eample.shop.entity.Category;
import com.eample.shop.entity.Product;
import com.eample.shop.mapper.CategoryMapper;
import com.eample.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动且分类表为空时，自动写入演示分类与商品数据。
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeedRunner implements ApplicationRunner {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductCacheService productCacheService;
    private final SeckillService seckillService;

    @Value("${shop.demo.seed-enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        if (!categoryMapper.findAll().isEmpty()) {
            return;
        }

        try {
            seedDemoData();
            log.info("演示分类与商品数据初始化完成");
        } catch (Exception e) {
            log.error("演示数据初始化失败", e);
        }
    }

    private void seedDemoData() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> categoryIds = insertCategories(now);
        List<Product> products = buildProducts(categoryIds, now);
        for (Product product : products) {
            productMapper.insert(product);
            if (product.getIsFlashSale() != null && product.getIsFlashSale() == 1) {
                try {
                    seckillService.warmupStockByProductId(product.getId());
                } catch (Exception e) {
                    log.warn("秒杀商品 {} 库存预热失败: {}", product.getName(), e.getMessage());
                }
            }
        }
        productCacheService.clearAllProductListCache();
        seckillService.refreshProductListCache();
    }

    private Map<String, Long> insertCategories(LocalDateTime now) {
        String[] names = {
                "数码电器", "家居生活", "美妆护肤", "服饰鞋包",
                "食品生鲜", "运动户外", "图书文具", "母婴玩具"
        };
        Map<String, Long> ids = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            Category category = new Category();
            category.setName(names[i]);
            category.setSort((long) (i + 1));
            category.setStatus(1);
            category.setCreateTime(now);
            category.setUpdateTime(now);
            categoryMapper.insert(category);
            ids.put(names[i], category.getId());
        }
        return ids;
    }

    private List<Product> buildProducts(Map<String, Long> categoryIds, LocalDateTime now) {
        List<Product> products = new ArrayList<>();
        products.add(normalProduct(categoryIds.get("数码电器"), "高性能游戏本", 6999.0, 120,
                "轻薄高性能，适合办公与游戏", "digital-laptop", now));
        products.add(flashProduct(categoryIds.get("数码电器"), "降噪蓝牙耳机", 899.0, 499.0, 350, 80,
                "主动降噪，长续航，通勤必备", "digital-earphone",
                now.minusHours(1), now.plusHours(8), now));
        products.add(normalProduct(categoryIds.get("数码电器"), "智能运动手表", 1099.0, 95,
                "心率监测、睡眠分析、消息提醒", "digital-watch", now));
        products.add(normalProduct(categoryIds.get("数码电器"), "27英寸4K显示器", 2399.0, 60,
                "IPS 面板，低蓝光护眼", "digital-monitor", now));

        products.add(normalProduct(categoryIds.get("家居生活"), "人体工学办公椅", 888.0, 45,
                "腰托可调，久坐更舒适", "home-chair", now));
        products.add(flashProduct(categoryIds.get("家居生活"), "北欧风护眼台灯", 159.0, 99.0, 200, 50,
                "三档色温，无频闪", "home-lamp",
                now.minusMinutes(30), now.plusHours(6), now));
        products.add(normalProduct(categoryIds.get("家居生活"), "记忆棉护颈枕", 199.0, 180,
                "慢回弹，贴合颈椎曲线", "home-pillow", now));
        products.add(normalProduct(categoryIds.get("家居生活"), "免手洗旋转拖把", 89.0, 260,
                "脱水省力，适合家庭清洁", "home-mop", now));

        products.add(normalProduct(categoryIds.get("美妆护肤"), "玻尿酸保湿面霜", 168.0, 300,
                "深层补水，改善干燥", "beauty-cream", now));
        products.add(normalProduct(categoryIds.get("美妆护肤"), "清爽防晒喷雾", 79.0, 420,
                "SPF50+，轻薄不闷", "beauty-sunscreen", now));
        products.add(flashProduct(categoryIds.get("美妆护肤"), "丝绒哑光口红套装", 299.0, 199.0, 150, 40,
                "三支热门色号，礼盒装", "beauty-lipstick",
                now.plusMinutes(8), now.plusHours(10), now));
        products.add(normalProduct(categoryIds.get("美妆护肤"), "氨基酸洁面乳", 59.0, 500,
                "温和清洁，洗后不紧绷", "beauty-cleanser", now));

        products.add(normalProduct(categoryIds.get("服饰鞋包"), "轻薄羽绒服", 599.0, 110,
                "90% 白鸭绒，便携收纳", "fashion-coat", now));
        products.add(normalProduct(categoryIds.get("服饰鞋包"), "经典休闲帆布鞋", 259.0, 220,
                "百搭款式，透气舒适", "fashion-shoes", now));
        products.add(normalProduct(categoryIds.get("服饰鞋包"), "真皮双肩背包", 399.0, 85,
                "大容量，适合通勤与短途出行", "fashion-bag", now));
        products.add(normalProduct(categoryIds.get("服饰鞋包"), "纯棉基础款T恤", 89.0, 600,
                "多色可选，亲肤透气", "fashion-tshirt", now));

        products.add(normalProduct(categoryIds.get("食品生鲜"), "每日坚果礼盒", 128.0, 240,
                "7 种坚果搭配，独立小包装", "food-nuts", now));
        products.add(flashProduct(categoryIds.get("食品生鲜"), "原切牛排套餐", 198.0, 128.0, 120, 35,
                "西冷+眼肉组合，冷链配送", "food-steak",
                now.minusHours(2), now.plusHours(5), now));
        products.add(normalProduct(categoryIds.get("食品生鲜"), "有机红富士苹果", 49.9, 800,
                "脆甜多汁，产地直供", "food-apple", now));
        products.add(normalProduct(categoryIds.get("食品生鲜"), "精品挂耳咖啡", 68.0, 360,
                "中深烘焙，香气浓郁", "food-coffee", now));

        products.add(normalProduct(categoryIds.get("运动户外"), "加厚防滑瑜伽垫", 79.0, 320,
                "10mm 厚度，回弹好", "sport-yoga", now));
        products.add(flashProduct(categoryIds.get("运动户外"), "轻量缓震跑鞋", 459.0, 299.0, 90, 25,
                "透气网面，日常训练适用", "sport-running",
                now.minusHours(1), now.plusHours(12), now));
        products.add(normalProduct(categoryIds.get("运动户外"), "双人露营帐篷", 699.0, 55,
                "防风防雨，快速搭建", "sport-tent", now));
        products.add(normalProduct(categoryIds.get("运动户外"), "运动保温水杯", 39.0, 480,
                "500ml，保冷保热", "sport-bottle", now));

        products.add(normalProduct(categoryIds.get("图书文具"), "Java 编程思想", 128.0, 150,
                "经典 Java 入门与进阶读物", "book-java", now));
        products.add(normalProduct(categoryIds.get("图书文具"), "中性签字笔套装", 29.0, 900,
                "12 支装，书写顺滑", "book-pen", now));
        products.add(normalProduct(categoryIds.get("图书文具"), "A5 活页笔记本", 35.0, 700,
                "可替换内芯，学习办公皆宜", "book-notebook", now));
        products.add(normalProduct(categoryIds.get("图书文具"), "儿童启蒙绘本", 45.0, 280,
                "亲子共读，色彩丰富", "book-picture", now));

        products.add(normalProduct(categoryIds.get("母婴玩具"), "轻便可折叠婴儿推车", 899.0, 40,
                "一键收车，出行方便", "baby-stroller", now));
        products.add(normalProduct(categoryIds.get("母婴玩具"), "大颗粒积木玩具", 129.0, 210,
                "安全材质，开发创造力", "baby-blocks", now));
        products.add(flashProduct(categoryIds.get("母婴玩具"), "儿童三轮滑板车", 199.0, 129.0, 100, 30,
                "可调节高度，稳定安全", "baby-scooter",
                now.plusMinutes(5), now.plusHours(9), now));
        products.add(normalProduct(categoryIds.get("母婴玩具"), "宽口径安抚奶瓶", 69.0, 330,
                "防胀气设计，易清洗", "baby-bottle", now));
        return products;
    }

    private Product normalProduct(Long categoryId, String name, double price, int stock,
                                  String description, String imageSeed, LocalDateTime now) {
        Product product = baseProduct(categoryId, name, price, stock, description, imageSeed, now);
        product.setIsFlashSale(0);
        product.setFlashPrice(null);
        product.setFlashStock(null);
        product.setFlashStartTime(null);
        product.setFlashEndTime(null);
        return product;
    }

    private Product flashProduct(Long categoryId, String name, double price, double flashPrice,
                                 int stock, int flashStock, String description, String imageSeed,
                                 LocalDateTime flashStart, LocalDateTime flashEnd, LocalDateTime now) {
        Product product = baseProduct(categoryId, name, price, stock, description, imageSeed, now);
        product.setIsFlashSale(1);
        product.setFlashPrice(flashPrice);
        product.setFlashStock(flashStock);
        product.setFlashStartTime(flashStart);
        product.setFlashEndTime(flashEnd);
        return product;
    }

    private Product baseProduct(Long categoryId, String name, double price, int stock,
                                String description, String imageSeed, LocalDateTime now) {
        Product product = new Product();
        product.setName(name);
        product.setCategoryId(categoryId.intValue());
        product.setPrice(price);
        product.setStock(stock);
        product.setImage("https://picsum.photos/seed/" + imageSeed + "/400/400");
        product.setDescription(description);
        product.setStatus(1);
        product.setCreateTime(now);
        product.setUpdateTime(now);
        return product;
    }
}
