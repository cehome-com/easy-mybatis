package com.github.easy30.easymybatis;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class QuickProvider {

    private static ThreadLocal<JSONObject> optionsLocal = new ThreadLocal<JSONObject>();

    public static String KEY_KEY_COLUMNS="keyColumns";

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



    public String list( Map params) {
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

    public String update( Map<String, Object> params) {
        String tableName =  optionsLocal.get().getString("table");
        String[] keyColumns= optionsLocal.get().getString(KEY_KEY_COLUMNS).split(",");
        StringBuilder sb = new StringBuilder("update " + tableName + " set ");
        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String k=e.getKey();
            Object v=e.getValue();
            if(e.getKey().startsWith("@")){
                k=k.substring(1);
            }else {
               v="#{"+k+"}";

            }
            joiner.add( k+"="+v);
        }
        sb.append(joiner);

        joiner = new StringJoiner(" and ");
        for (String keyColumn:keyColumns) {
            joiner.add(keyColumn+"="+"#{"+keyColumn+"}");
        }
        sb.append(" where ").append(joiner);


        return getScript(sb.toString());
    }

    public String save( Map<String, Object> params) {
        String tableName = optionsLocal.get().getString("table");
        String keyColumns= optionsLocal.get().getString(KEY_KEY_COLUMNS);
        if(StringUtils.isBlank(keyColumns)) throw new RuntimeException("keyColumns required");
        String[] keyColumnArray = optionsLocal.get().getString(KEY_KEY_COLUMNS).split(",");

        for (String keyColumn:keyColumnArray) {
            if(params.get(keyColumn)==null) return insert(params);
        }
        return update(params);
    }

    private String getScript(String sql){
        return "<script>\n"+ sql+"\n</script>";
    }

   /* public String list3( @Param("ss") String sql,@Param("pp")Map params) {
        return  sql;
    }*/
}
