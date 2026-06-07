package com.eample.shop.Service;

import com.eample.shop.dto.CategoryRequestDTO;
import com.eample.shop.entity.Category;

import java.util.List;

public interface CategoryService {
    /** 后台：全部分类列表 */
    List<Category> listAll();
    /** 前台：仅启用分类 */
    List<Category> listEnabled();
    /** 按 id 查询 */
    Category getById(Long id);
    /** 新增 */
    void add(CategoryRequestDTO request);
    /** 修改 */
    void update(CategoryRequestDTO request);
    /** 删除 */
    void delete(Long id);
}
