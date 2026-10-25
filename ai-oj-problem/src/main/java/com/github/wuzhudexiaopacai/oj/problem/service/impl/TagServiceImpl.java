package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.problem.config.ProblemCacheProperties;
import com.github.wuzhudexiaopacai.oj.problem.entity.Tag;
import com.github.wuzhudexiaopacai.oj.problem.entity.TagCategory;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TagCategoryMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TagMapper;
import com.github.wuzhudexiaopacai.oj.problem.service.TagService;
import com.github.wuzhudexiaopacai.oj.problem.vo.TagTreeVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    /**
     * 标签树缓存 key
     */
    private static final String TAG_TREE_KEY = RedisKeys.PROBLEM_LIST + "tag:tree";

    private final TagCategoryMapper tagCategoryMapper;
    private final TagMapper tagMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProblemCacheProperties cacheProperties;

    @Override
    @SuppressWarnings("unchecked")
    public List<TagTreeVO> tree() {
        Object cached = redisTemplate.opsForValue().get(TAG_TREE_KEY);
        if (cached instanceof List<?> list) {
            return (List<TagTreeVO>) list;
        }
        List<TagCategory> categories = tagCategoryMapper.selectList(
                new LambdaQueryWrapper<TagCategory>().orderByAsc(TagCategory::getSort));
        List<Tag> tags = tagMapper.selectList(null);
        Map<Long, List<Tag>> tagMap = tags.stream().collect(Collectors.groupingBy(Tag::getCategoryId));
        List<TagTreeVO> tree = categories.stream().map(c -> {
            TagTreeVO node = new TagTreeVO();
            node.setCategoryId(c.getId());
            node.setCategoryName(c.getName());
            node.setSort(c.getSort());
            node.setTags(tagMap.getOrDefault(c.getId(), List.of()).stream().map(t -> {
                TagVO tagVO = new TagVO();
                tagVO.setId(t.getId());
                tagVO.setName(t.getName());
                tagVO.setColor(t.getColor());
                return tagVO;
            }).collect(Collectors.toList()));
            return node;
        }).collect(Collectors.toList());
        redisTemplate.opsForValue().set(TAG_TREE_KEY, tree,
                cacheProperties.getListTtlSeconds(), TimeUnit.SECONDS);
        return tree;
    }
}
