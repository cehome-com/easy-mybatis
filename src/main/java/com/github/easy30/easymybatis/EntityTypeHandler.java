package com.github.easy30.easymybatis;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * convert between entityClass props and database columns
 */
public interface EntityTypeHandler {
    /**
     * find props of entityClass which not register and register it;
     * @param configuration
     * @param registry
     * @param entityClass
     */
    void register(Configuration configuration, TypeHandlerRegistry registry, Class entityClass);
}