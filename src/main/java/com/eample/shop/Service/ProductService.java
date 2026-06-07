package com.eample.shop.Service;
import com.eample.shop.dto.ProductRequestDTO;
import com.eample.shop.common.PageResult;
import com.eample.shop.entity.CartItem;
import com.eample.shop.entity.Product;
import com.eample.shop.entity.ProductByQuantity;

import java.util.List;
public interface ProductService {
    List<Product> search(String keyword, Long categoryId, Boolean flashSale, Integer page, Integer size); //前台首页搜索
    List<Product> listAll();           // 后台
    List<Product> listOnShelf();       // 前台首页
    List<Product> listFlashSale();     // 前台秒杀区
    List<Product> listByCategory(Long categoryId);
    Product getById(Long id);
    void add(ProductRequestDTO request);
    void update(ProductRequestDTO request);
    void onShelf(Long id);
    void offShelf(Long id);
    void delete(Long id);

    PageResult<Product> adminSearch(String keyword, Long categoryId, Integer status, Boolean flashSale, Integer page, Integer size);

    List<ProductByQuantity> getByIds(List<CartItem> cartItems);
}
