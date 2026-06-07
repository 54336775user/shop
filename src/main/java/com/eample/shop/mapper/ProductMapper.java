package com.eample.shop.mapper;

import com.eample.shop.entity.Product;
import com.eample.shop.entity.ProductByQuantity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    /**
     * 查询所有product
     * @return
     */
    List<Product> findAll();

    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    List<Product> findFlashSale();  // is_flash_sale=1 且 status=1

    List<Product> findVisibleFlashSale();  // 进行中 + 10分钟内即将开始

    List<Product> findOnShelf();    // 前台：status=1

    Product findById(@Param("id") Long id);

    void insert(Product product);

    void update(Product product);

    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    void deleteById(@Param("id") Long id);

    int countByCategoryId(@Param("categoryId") Long categoryId); // 删分类前校验用

    long countSearch(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("flashSale") Boolean flashSale);

    long countAdminSearch(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("flashSale") Boolean flashSale);

    List<Product> search(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("flashSale") Boolean flashSale,
            @Param("offset") Integer offset,
            @Param("size") Integer size);

    List<Product> adminsearch(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("flashSale") Boolean flashSale,
            @Param("offset") Integer offset,
            @Param("size") Integer size);

    /**
     * 扣减商品库存
     * @param productId
     * @param quantity
     * @return
     */
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 回调增加商品库存
     * @param productId
     * @param quantity
     * @return
     */
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 优化部分，返回product和quantity列表
     * @param productIds
     * @return
     */
    List<Product> getByIds(List<Long> productIds);


    /**
     * 扣减秒杀库存
     * @param id
     * @param quantity
     * @return
     */
    int decreaseFlashStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 回滚秒杀库存
     */
    int increaseFlashStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
