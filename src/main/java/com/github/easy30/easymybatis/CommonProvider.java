package com.github.easy30.easymybatis;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class CommonProvider {

    private static ThreadLocal<JSONObject> optionsLocal = new ThreadLocal<JSONObject>();

    public static String KEY_KEY_COLUMNS="keyColumns";

    public static String KEY_SELECT_COLUMN="selectColumn";
    public static final String PARAM_E ="e";
    public static final String PARAM_P="p";

    public   static void addOptions(Object... kvs){
        if(kvs==null || kvs.length==0 ) return ;
        JSONObject options= getOptions();
        if(options==null) {
            options=new JSONObject(true);
            setOptions(options);
        }
        for(int i=0;i<kvs.length;i+=2){
            options.put(kvs[i].toString(),kvs[i+1]);
        }


    }
   public   static void setOptions(JSONObject options){
        optionsLocal.set(options);

    }
    public   static JSONObject getOptions(){
      return   optionsLocal.get();

    }
    public   static void removeOptions(){
        optionsLocal.remove( );

    }

    public static String getConditions(Map<String, Object> params) {
        StringBuilder sb=new StringBuilder();
        if(params!=null) {
            params.forEach((k, v) -> {
                sb.append(k + "=" + "#{" + k + "}");
            });
        }

        return sb.toString();
    }

    public static Object getFirstValue(Map<String, Object> row){
        if(row!=null && row.values()!=null) {
            for (Object v : row.values()) {
                return v;
            }
        }
        return null;
    }
    public static String getSelectSql(String select,String from, String where ,String orderBy){
        StringBuilder sql=new StringBuilder();
        sql.append("select ").append(StringUtils.isBlank(select)?" * ":select)
                .append(" from "+from+" ");
        if(StringUtils.isNotBlank(where)){
            sql.append(" where ").append(where);
        }
        if(StringUtils.isNotBlank(orderBy)){
            sql.append(" order by ").append(orderBy);
        }
        return sql.toString();
    }




    public String execute( Map params) {
        return "<script>\n"+  optionsLocal.get().getString("sql")+"\n</script>";
    }


    public String insert( Map<String, Object> params) {
        String tableName =  optionsLocal.get().getString("table");
        String sql = "insert into " + tableName + "({0}) values({1})";
        StringJoiner s1 = new StringJoiner(",");
        StringJoiner s2 = new StringJoiner(",");
        for (Map.Entry<String, Object> e : params.entrySet()) {

            if(e.getKey().startsWith("@")){
                s1.add(e.getKey().substring(1));
                s2.add(e.getValue().toString());

            }else {
                s1.add(e.getKey());
                s2.add("#{" + e.getKey() + "}");
            }

        }

        MessageFormat fmt = new MessageFormat(sql);
        Object[] args = { s1, s2 };
        sql = fmt.format(args);

        return "<script>\n"+ sql+"\n</script>";
    }


    public String updateByParams(@Param("table")String table, @Param(PARAM_E) Map<String, Object> row, @Param(PARAM_P) Map<String, Object> params) {
        //String tableName =  optionsLocal.get().getString("table");
       // String[] keyColumns= optionsLocal.get().getString(KEY_KEY_COLUMNS).split(",");
        StringBuilder sb = new StringBuilder("update " + table + " set ");
        StringJoiner setJoiner = new StringJoiner(",");
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String k=e.getKey();
            Object v=e.getValue();
            if(e.getKey().startsWith("@")){
                k=k.substring(1);
            }else {
               v="#{"+ PARAM_E +"."+k+"}";

            }
            setJoiner.add( k+"="+v);
        }
        sb.append(setJoiner);

        StringJoiner   whereJoiner = new StringJoiner(" and ");
        for (String p:params.keySet()) {
            whereJoiner.add(p+"="+"#{"+PARAM_P +"."+p+"}");
        }
        sb.append(" where ").append(whereJoiner);


        return getScript(sb.toString());
    }



    public static Map<String, Object> getKeyColumnMap(Map<String, Object> params,String[] keyColumnArray){
        Map<String, Object> keyColumnMap = new HashMap<>();
        for (String key : keyColumnArray) {
            Object value = params.get(key);
            if (value == null) throw new RuntimeException("value of '" + key + "' required");
            keyColumnMap.put(key, params.get(key));
        }
        return keyColumnMap;
    }

    private String getScript(String sql){
        return "<script>\n"+ sql+"\n</script>";
    }

   /* public String list3( @Param("ss") String sql,@Param("pp")Map params) {
        return  sql;
    }*/
}
