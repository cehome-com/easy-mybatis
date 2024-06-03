package com.github.easy30.easymybatis;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

import static com.github.easy30.easymybatis.QuickProvider.PARAM_E;
import static com.github.easy30.easymybatis.QuickProvider.PARAM_P;

public interface QuickMapper {
    List<Map<String, Object>> listBySql(String sql, Map<String, Object> params);

    List<Map<String, Object>> listParams(String columns, String table, Map params, String orderBy);

    List<Map<String, Object>> pageBySql(Page page, String sql, Map params);

    List<Map<String, Object>> pageParams(Page page, String columns, String table, Map params, String orderBy);

    /**
     * insert
     * @param table
     * @param params
     * @return
     */
    int insert(String table, Map<String, Object> params);

    /**
     * insert and return primary keys and values
     * @param table
     * @param params
     * @param keyColumns
     * @return
     */
    int insert(String table, Map<String, Object> params, String keyColumns);
    Map<String, Object> get(String table, String key, Object value);

    int updateByParams(String table, @Param(PARAM_E) Map<String, Object> row, @Param(PARAM_P) Map<String, Object> params);

    int updateByKeys(String table, Map<String, Object> row, String keyColumns);

    int updateBySql(String sql, Map<String, Object> params);

    int save(String table, Map<String, Object> params, String keyColumns);

    int saveByQuery(String table, Map<String, Object> params, String keyColumns);

    Map<String, Object> getOneByKey(String columns, String table, String key, Object value);

    Map<String, Object> getOneByParams(String columns, String table, Map params, String orderBy);

    Map<String, Object> getOneBySql(String sql, Map<String, Object> params);

    Object getValueBySql(String sql, Map params);

    Object getValueByParams(String column, String table, Map params, String orderBy);

    Object getValueByKey(String column, String table, String key, Object value);

    Object deleteByKey(String table, String key, Object value);

    int deleteByParams(String table, Map params);

    int deleteBySql(String sql, Map params);
}
