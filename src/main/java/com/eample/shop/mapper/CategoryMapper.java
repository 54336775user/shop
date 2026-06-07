package com.eample.shop.mapper;
import com.eample.shop.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    /** 查询全部分类（后台列表，按 sort 升序） */
    List<Category> findAll();

    /** 查询启用的分类（前台下拉/筛选用） */
    List<Category> findEnabled();

    /** 按 id 查询 */
    Category findById(@Param("id") Long id);

    /** 按名称统计数量（新增/修改时防重名） */
    Integer countByName(@Param("name") String name);

    /** 插入分类 */
    int insert(Category category);

    /** 更新分类 */
    int update(Category category);

    /** 按 id 删除 */
    int deleteById(@Param("id") Long id);
}
