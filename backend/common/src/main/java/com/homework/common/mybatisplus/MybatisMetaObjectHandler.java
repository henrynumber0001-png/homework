package com.homework.common.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {
    //类的作用：在插入数据时，自动填充创建时间；在更新数据时自动填充更新时间，避免在每个Service方法里手动setCreatedTime,setUpdatedTime
    @Override
    public void insertFill(MetaObject metaObject) {
        // BaseEntity 使用 LocalDateTime，填充类型必须完全一致，否则字段会保持 null。
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, now);
        //新增数据时，created_time和updated_time同时更新
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
    }
}
