package com.github.easy30.easymybatis;

import java.util.List;
import java.util.Map;

public interface QuickMapper {
    List<Map<String, Object>> list(String sql, Map<String, Object> params);
    int insert(String table, Map<String, Object> params, String keyColumns);
    Map<String, Object> get(String table, String key, Object value);
    Map<String, Object> getOne(String sql, Map<String, Object> params);
}
