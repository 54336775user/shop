package com.eample.shop.Service.impl;

import com.eample.shop.Service.CategoryService;
import com.eample.shop.Service.ProductService;
import com.eample.shop.Service.SeckillService;
import com.eample.shop.cache.ProductCacheService;
import com.eample.shop.common.PageResult;
import com.eample.shop.dto.ProductRequestDTO;
import com.eample.shop.entity.CartItem;
import com.eample.shop.entity.Product;
import com.eample.shop.entity.ProductByQuantity;
import com.eample.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final ProductCacheService productCacheService;
    private final SeckillService seckillService;

    /**
     * 前台搜索框
     * @param keyword
     * @param categoryId
     * @param flashSale
     * @param page
     * @param size
     * @return
     */
    @Override
    public List<Product> search(String keyword, Long categoryId, Boolean flashSale, Integer page, Integer size) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 8 : size;
        int offset = (currentPage - 1) * pageSize;

        String realKeyword = null;
        if (StringUtils.hasText(keyword)) {
            realKeyword = keyword.trim();
        }
        String key= productCacheService.buildKey(realKeyword, categoryId, flashSale, currentPage, pageSize);
        List<Product> res=productCacheService.getCachedList(key);
        if(res != null && !res.isEmpty()){
            return res;
        }
        List<Product> search = productMapper.search(realKeyword, categoryId, flashSale, offset, pageSize);
        productCacheService.setCachedList(key,search);
        return search;
    }

    @Override
    public PageResult<Product> adminSearch(String keyword, Long categoryId, Integer status, Boolean flashSale, Integer page, Integer size) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 8 : size;
        int offset = (currentPage - 1) * pageSize;

        String realKeyword = null;
        if (StringUtils.hasText(keyword)) {
            realKeyword = keyword.trim();
        }
        long total = productMapper.countAdminSearch(realKeyword, categoryId, status, flashSale);
        List<Product> search = productMapper.adminsearch(realKeyword, categoryId, status, flashSale, offset, pageSize);
        return PageResult.of(search, total, currentPage, pageSize);
    }

    /**
     * 查询所有商品
     * @return
     */
    @Override
    public List<Product> listAll() {
        return productMapper.findAll();
    }

    /**
     * 根据商品id查询
     * @param categoryId
     * @return
     */
    @Override
    public List<Product> listByCategory(Long categoryId) {
        categoryService.getById(categoryId);
        return productMapper.findByCategoryId(categoryId);
    }

    /**
     * 查询秒杀商品
     * @return
     */
    @Override
    public List<Product> listFlashSale() {
        return productMapper.findVisibleFlashSale();
    }

    /**
     * 查询上架商品
     * @return
     */
    @Override
    public List<Product> listOnShelf() {
        return productMapper.findOnShelf();
    }

    /**
     * 上架该id商品
     * @param id
     */
    @Override
    public void onShelf(Long id) {
        Product product = getById(id);
        productMapper.updateStatus(product.getId(), 1);
        productCacheService.clearAllProductListCache();
    }

    /**
     * 下架该id商品
     * @param id
     */
    @Override
    public void offShelf(Long id) {
        Product product = getById(id);
        productMapper.updateStatus(product.getId(), 0);
        productCacheService.clearAllProductListCache();
    }

    /**
     * 删除商品
     * @param id
     */
    @Override
    public void delete(Long id) {
        Product product = getById(id);
        productMapper.deleteById(product.getId());
        productCacheService.clearAllProductListCache();
    }


    /**
     * 修改商品
     * @param request
     */
    @Override
    public void update(ProductRequestDTO request) {
        if (request.getId() == null) {
            throw new RuntimeException("商品id不能为空");
        }
        Product product = getById(request.getId());
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (request.getCategoryId() == null) {
            throw new RuntimeException("请选择分类");
        }
        categoryService.getById(request.getCategoryId());

        product.setName(request.getName().trim());
        product.setCategoryId(Math.toIntExact(request.getCategoryId()));
        product.setPrice(request.getPrice());
        if (request.getStock() != null && request.getStock() > 0) {
            product.setStock(request.getStock());
        }
        else{
            throw new RuntimeException("商品库存必须大于 0");
        }
        if (request.getImage() != null) {
            product.setImage(request.getImage());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getIsFlashSale() != null) {
            product.setIsFlashSale(request.getIsFlashSale());
        }
        if (request.getIsFlashSale() != null && request.getIsFlashSale() == 0) {
            product.setFlashPrice(null);
            product.setFlashStock(null);
        } else {
            if (request.getFlashPrice() != null) {
                product.setFlashPrice(request.getFlashPrice());
            }
            if (request.getFlashStock() != null) {
                product.setFlashStock(request.getFlashStock());
            }
        }
        if(request.getFlashStartTime() != null){
            product.setFlashStartTime(request.getFlashStartTime());
        }
        if(request.getFlashEndTime() != null){
            product.setFlashEndTime(request.getFlashEndTime());
        }
        product.setUpdateTime(LocalDateTime.now());
        productMapper.update(product);
        if (product.getIsFlashSale() != null && product.getIsFlashSale() == 1) {
            try {
                seckillService.warmupStockByProductId(product.getId());
            } catch (Exception ignored) {
                // 预热失败不影响主流程，可打日志
                log.info("预热失败");
            }
        }
        productCacheService.clearAllProductListCache();
    }

    /**
     * 根据id查询商品
     * @param id
     * @return
     */
    @Override
    public Product getById(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return product;
    }

    /**
     * 添加商品
     * @param request
     */
    @Override
    public void add(ProductRequestDTO request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (request.getCategoryId() == null) {
            throw new RuntimeException("请选择分类");
        }
        categoryService.getById(request.getCategoryId()); // 分类不存在会抛异常

        if (request.getPrice() == null) {
            throw new RuntimeException("商品价格不能为空");
        }

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setCategoryId(Math.toIntExact(request.getCategoryId()));
        product.setPrice(request.getPrice());
        if (request.getStock() != null && request.getStock() > 0) {
            product.setStock(request.getStock());
        }
        else{
            throw new RuntimeException("商品库存必须大于 0");
        }
        product.setImage(request.getImage());
        product.setDescription(request.getDescription());
        Integer status = request.getStatus();
        Integer flashSaleFlag = request.getIsFlashSale();
        product.setStatus(status != null ? status : 1);
        product.setIsFlashSale(flashSaleFlag != null ? flashSaleFlag : 0);
        product.setFlashPrice(request.getFlashPrice());
        product.setFlashStock(request.getFlashStock());
        if (product.getIsFlashSale() != null && product.getIsFlashSale() == 0) {
            product.setFlashPrice(null);
            product.setFlashStock(null);
        }
        if(request.getFlashStartTime() != null){
            product.setFlashStartTime(request.getFlashStartTime());
        }
        if(request.getFlashEndTime() != null){
            product.setFlashEndTime(request.getFlashEndTime());
        }
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        productCacheService.clearAllProductListCache();
    }

    /**
     * 优化部分，获取product与quantity列表
     * @param cartItems
     * @return
     */
    @Override
    public List<ProductByQuantity> getByIds(List<CartItem> cartItems) {
        List<Long>productIds=cartItems.stream().map(CartItem::getProductId).collect(Collectors.toList());
        //
        List<Product>res=productMapper.getByIds(productIds);
        //
        Map<Long, Product> productMap = res.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        //
        return cartItems.stream().map(cartItem -> {
            ProductByQuantity productByQuantity=new ProductByQuantity();
            productByQuantity.setProduct(productMap.get(cartItem.getProductId()));
            productByQuantity.setQuantity(cartItem.getQuantity());
            return productByQuantity;
        }).collect(Collectors.toList());
    }
}
