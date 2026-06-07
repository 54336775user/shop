package com.eample.shop.Service.impl;

import com.eample.shop.Service.CategoryService;
import com.eample.shop.dto.CategoryRequestDTO;
import com.eample.shop.entity.Category;
import com.eample.shop.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 查看所有分类商品
     * @return
     */
    @Override
    public List<Category> listAll() {
        return categoryMapper.findAll();
    }

    /**
     * 查看开始售卖商品
     * @return
     */
    @Override
    public List<Category> listEnabled() {
        return categoryMapper.findEnabled();
    }

    /**
     * 根据id查询商品
     * @param id
     * @return
     */
    @Override
    public Category getById(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        return category;
    }

    /**
     * 添加商品
     * @param request
     */
    @Override
    public void add(CategoryRequestDTO request) {
        if(request.getName() == null){
            throw new RuntimeException("未填写分类名");
        }
        if(StringUtils.isEmpty(request.getName())){
            throw new RuntimeException("分类名称为空");
        }
        String name = request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new RuntimeException("分类名称不能为空");
        }
        if (categoryMapper.countByName(name) > 0) {
            throw new RuntimeException("分类名称已存在");
        }

        Category category = new Category();
        category.setName(name);
        category.setSort((long) (request.getSort() != null ? request.getSort() : 0));
        category.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());

        categoryMapper.insert(category);
    }

    /**
     * 更新商品
     * @param request
     */
    @Override
    public void update(CategoryRequestDTO request) {
        if (request.getId() == null) {
            throw new RuntimeException("分类 id 不能为空");
        }
        Category exist = getById(request.getId());

        if(StringUtils.isEmpty(request.getName())){
            throw new RuntimeException("分类名称为空");
        }
        String name = request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new RuntimeException("分类名称不能为空");
        }
        // 改名时才查重名（排除自己）
        if (!name.equals(exist.getName()) && categoryMapper.countByName(name) > 0) {
            throw new RuntimeException("分类名称已存在");
        }

        exist.setName(name);
        if (request.getSort() != null) {
            exist.setSort(Long.valueOf(request.getSort()));
        }
        if (request.getStatus() != null) {
            exist.setStatus(request.getStatus());
        }
        exist.setUpdateTime(LocalDateTime.now());

        int rows = categoryMapper.update(exist);
        if (rows == 0) {
            throw new RuntimeException("更新失败");
        }
    }

    /**
     * 删除商品
     * @param id
     */
    @Override
    public void delete(Long id) {
        getById(id); // 不存在会抛异常
        // 以后有 product 表时：若该分类下还有商品，禁止删除
        int rows = categoryMapper.deleteById(id);
        if (rows == 0) {
            throw new RuntimeException("删除失败");
        }
    }
}
