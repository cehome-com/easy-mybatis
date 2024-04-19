package com.github.easy30.easymybatis;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

public interface EntityTypeHandler {
    void register(Configuration configuration, TypeHandlerRegistry registry, Class entityClass);
}