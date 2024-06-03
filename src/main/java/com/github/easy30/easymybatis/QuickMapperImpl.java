package com.github.easy30.easymybatis;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.*;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.easy30.easymybatis.QuickProvider.PARAM_E;
import static com.github.easy30.easymybatis.QuickProvider.PARAM_P;

public interface QuickMapperImpl extends QuickMapper {
    @SelectProvider(type = QuickProvider.class, method = "execute")
    List<Map> doList(Map params);

    //@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @InsertProvider(type = QuickProvider.class, method = "insert")
    int doInsert(Map row);

    @UpdateProvider(type = QuickProvider.class, method = "save")
    int doSave(Map row);

    @DeleteProvider(type = QuickProvider.class, method = "execute")
    int doDelete(Map params);

    @DeleteProvider(type = QuickProvider.class, method = "execute")
    int doUpdate(Map params);

    @UpdateProvider(type = QuickProvider.class, method = "updateByParams")
    int doUpdateByParams(String table, @Param(PARAM_E) Map<String, Object> row, @Param(PARAM_P) Map<String, Object> params);

    @Override
    default List<Map<String, Object>> listBySql(String sql, Map params) {

        try {
            QuickProvider.addOptions("sql", sql);
            return (List<Map<String, Object>>) (Object) doList(params);
        } finally {
            QuickProvider.removeOptions();
        }


    }

    @Override
    default List<Map<String, Object>> listParams(String columns, String table, Map params, String orderBy) {

        String sql = QuickProvider.getSelectSql(columns, table, QuickProvider.getConditions(params), orderBy);
        return listBySql(sql, params);
    }

    @Override
    default List<Map<String, Object>> pageBySql(Page page, String sql, Map params) {
        try {
            PageContext.set(page);
            return listBySql(sql, params);

        } finally {
            PageContext.remove();
        }


    }

    @Override
    default List<Map<String, Object>> pageParams(Page page, String columns, String table, Map params, String orderBy) {

        try {
            PageContext.set(page);
            return listParams(columns, table, params, orderBy);

        } finally {
            PageContext.remove();
        }

    }


    @Override
    default int insert(String table, Map<String, Object> params) {
        return insert(table, params, null);
    }

    @Override
    default int insert(String table, Map<String, Object> params, String keyColumns) {
        try {
            QuickProvider.addOptions("table", table);
            if (StringUtils.isNotBlank(keyColumns)) QuickProvider.addOptions(QuickProvider.KEY_KEY_COLUMNS, keyColumns);
            return doInsert(params);
        } finally {
            QuickProvider.removeOptions();
        }

    }
    @Override
    default int updateByParams(String table, @Param(PARAM_E) Map<String, Object> row, @Param(PARAM_P) Map<String, Object> params) {
        return doUpdateByParams(table, row, params);
    }
    @Override
    default int updateByKeys(String table, Map<String, Object> row, String keyColumns) {
        if (StringUtils.isBlank(keyColumns)) throw new RuntimeException("keyColumns required");
        return doUpdateByParams(table, row, QuickProvider.getKeyColumnMap(row, keyColumns.split(",")));

    }

    @Override
    default int updateBySql(String sql, Map<String, Object> params) {
        try {
            QuickProvider.addOptions("sql", sql);
            return doUpdate(params);
        } finally {
            QuickProvider.removeOptions();
        }
    }

    @Override
    default int save(String table, Map<String, Object> params, String keyColumns) {
        try {
            QuickProvider.addOptions("table", table, QuickProvider.KEY_KEY_COLUMNS, keyColumns);
            return doSave(params);
        } finally {
            QuickProvider.removeOptions();
        }
    }
    @Override
    default int saveByQuery(String table, Map<String, Object> params, String keyColumns) {
        if (StringUtils.isBlank(keyColumns)) throw new RuntimeException("keyColumns required");
        try {
            QuickProvider.addOptions("table", table, QuickProvider.KEY_KEY_COLUMNS, keyColumns);
            Map<String, Object> keyColumnMap = QuickProvider.getKeyColumnMap(params, keyColumns.split(","));
            Long count = ((Number) getValueByParams("count(*)", table, keyColumnMap, null)).longValue();
            if (count == 0) return insert(table, params);
            else if (count == 1) return updateByKeys(table, params, keyColumns);
            else throw new RuntimeException("more than one record for " + keyColumns);

        } finally {
            QuickProvider.removeOptions();
        }
    }
    @Override
    default Map<String, Object> getOneByKey(String columns, String table, String key, Object value) {
        String sql = QuickProvider.getSelectSql(columns, table, key + "=#{" + key + "}", null);
        return getOneBySql(sql, Collections.singletonMap(key, value));
    }
    @Override
    default Map<String, Object> getOneByParams(String columns, String table, Map params, String orderBy) {
        String sql = QuickProvider.getSelectSql(columns, table, QuickProvider.getConditions(params), orderBy);
        if (StringUtils.isNotBlank(orderBy)) sql += " order by " + orderBy;
        return getOneBySql(sql, params);
    }
    @Override
    default Map<String, Object> getOneBySql(String sql, Map params) {
        List<Map<String, Object>> list = listBySql(sql, params);
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }
    @Override
    default Object getValueBySql(String sql, Map params) {
        return QuickProvider.getFirstValue(getOneBySql(sql, params));
    }
    @Override
    default Object getValueByParams(String column, String table, Map params, String orderBy) {
        String sql = QuickProvider.getSelectSql(column, table, QuickProvider.getConditions(params), orderBy);
        if (StringUtils.isNotBlank(orderBy)) sql += " order by " + orderBy;
        return getValueBySql(sql, params);

    }
    @Override
    default Object getValueByKey(String column, String table, String key, Object value) {
        return getValueByParams(column, table, Collections.singletonMap(key, value), null);

    }
    @Override
    default Object deleteByKey(String table, String key, Object value) {
        return deleteByParams(table, Collections.singletonMap(key, value));

    }
    @Override
    default int deleteByParams(String table, Map params) {
        String sql = "delete from " + table + " where " + QuickProvider.getConditions(params);
        return deleteBySql(sql, params);
    }
    @Override
    default int deleteBySql(String sql, Map params) {
        try {
            QuickProvider.addOptions("sql", sql);
            return doDelete(params);
        } finally {
            QuickProvider.removeOptions();
        }

    }


}
