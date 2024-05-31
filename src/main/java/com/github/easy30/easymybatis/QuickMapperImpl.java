package com.github.easy30.easymybatis;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.*;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface QuickMapperImpl extends QuickMapper{
    @SelectProvider(type = QuickProvider.class, method = "list")
    List<Map> doList(Map params);
    //@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @InsertProvider(type = QuickProvider.class, method = "insert")
    int doInsert(Map params);

    @UpdateProvider(type = QuickProvider.class, method = "update")
    int doUpdate(Map params);

    default List<Map<String, Object>> list( String sql, Map params){

        try{
            QuickProvider.addOptions("sql",sql);
            return (List<Map<String,Object>>)(Object) doList(params);
        }finally {
            QuickProvider.removeOptions();
        }


    }

    default  int insert(String table,  Map<String, Object> params){
        return insert(table,params,null);
    }
    default  int insert(String table,  Map<String, Object> params,String keyColumns){
        try{
            QuickProvider.addOptions("table",table);
            if(StringUtils.isNotBlank(keyColumns)) QuickProvider.addOptions(QuickProvider.KEY_KEY_COLUMNS,keyColumns);
            return  doInsert(params);
        }finally {
            QuickProvider.removeOptions();
        }

    }


    default  int update(String table,  Map<String, Object> params,String keyColumns){
        return 1;
    }


    default Map<String, Object> get( String table,String key ,Object value){
        String sql="select * from "+table+" where "+key+"=#{"+key+"}";
        return getOne(sql, Collections.singletonMap(key,value));
    }
    default Map<String, Object> getOne( String sql, Map params){
        List<Map<String, Object>> list = list(sql, params);
        return CollectionUtils.isEmpty(list)?null:list.get(0);
    }

    @Select("select * from ${tt} where 1=1 and company_name =#{company_name}")
    List<Map> list2(String sql, Map params);
}
