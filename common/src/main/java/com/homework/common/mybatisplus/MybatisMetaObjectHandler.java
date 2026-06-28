package com.homework.common.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {
    //类的作用：在插入数据时，自动填充创建时间；在更新数据时自动填充更新时间，避免在每个Service方法里手动setCreatedTime,setUpdatedTime
    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        this.strictInsertFill(metaObject, "createdTime", Date.class, now);
        this.strictInsertFill(metaObject, "updatedTime", Date.class, now);
        //新增数据时，created_time和updated_time同时更新
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedTime", Date.class, new Date());
    }
}
