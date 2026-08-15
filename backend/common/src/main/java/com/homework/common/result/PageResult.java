package com.homework.common.result;

import lombok.Data;

import java.util.List;

@Data
//因为 Page 属于 MyBatis-Plus，包含很多前端不需要的数据库框架字段，会让接口依赖具体持久层框架。
//更推荐：
//Mapper/Service 内部：Page<Entity>
//Controller 返回：PageResult<VO>
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;
}
