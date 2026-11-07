package com.github.wuzhudexiaopacai.oj.common.web.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 审计字段自动填充测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class MybatisPlusConfigTest {

    private final MetaObjectHandler handler = new MybatisPlusConfig().metaObjectHandler();

    /**
     * 测试用实体：复刻 BaseEntity 的审计字段，避免依赖未注册的 BaseEntity TableInfo
     */
    @Data
    @TableName("unit_test_entity")
    static class TestEntity {

        @TableId(type = IdType.ASSIGN_ID)
        private Long id;

        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;

        @TableField(fill = FieldFill.INSERT)
        private Integer deleted;

        @TableField(fill = FieldFill.INSERT)
        private Long createBy;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private Long updateBy;
    }

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TestEntity.class);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        LoginUser loginUser = LoginUser.builder().id(userId).username("tester").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    @Test
    void insertFillShouldRecordCurrentUserId() {
        login(1001L);
        TestEntity entity = new TestEntity();

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertEquals(1001L, entity.getCreateBy());
        assertEquals(1001L, entity.getUpdateBy());
        assertNotNull(entity.getCreateTime());
        assertNotNull(entity.getUpdateTime());
        assertEquals(0, entity.getDeleted());
    }

    @Test
    void insertFillWithoutLoginShouldKeepAuditUserNull() {
        TestEntity entity = new TestEntity();

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertNull(entity.getCreateBy());
        assertNull(entity.getUpdateBy());
        assertNotNull(entity.getCreateTime());
        assertEquals(0, entity.getDeleted());
    }

    @Test
    void insertFillShouldNotOverwriteExistingAuditUser() {
        login(1001L);
        TestEntity entity = new TestEntity();
        // 例如操作日志消费者落库时已从消息体回填操作人
        entity.setCreateBy(2002L);
        entity.setUpdateBy(2002L);

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertEquals(2002L, entity.getCreateBy());
        assertEquals(2002L, entity.getUpdateBy());
    }

    @Test
    void updateFillShouldRefreshUpdateUserAndTime() {
        login(1001L);
        TestEntity entity = new TestEntity();
        entity.setCreateBy(2002L);

        handler.updateFill(SystemMetaObject.forObject(entity));

        assertEquals(2002L, entity.getCreateBy());
        assertEquals(1001L, entity.getUpdateBy());
        assertNotNull(entity.getUpdateTime());
    }

    @Test
    void baseEntityShouldDeclareAuditFieldsWithFillAnnotations() throws NoSuchFieldException {
        Field createBy = BaseEntity.class.getDeclaredField("createBy");
        Field updateBy = BaseEntity.class.getDeclaredField("updateBy");

        assertEquals(Long.class, createBy.getType());
        assertEquals(Long.class, updateBy.getType());
        TableField createByField = createBy.getAnnotation(TableField.class);
        TableField updateByField = updateBy.getAnnotation(TableField.class);
        assertNotNull(createByField);
        assertNotNull(updateByField);
        assertEquals(FieldFill.INSERT, createByField.fill());
        assertEquals(FieldFill.INSERT_UPDATE, updateByField.fill());
    }
}
