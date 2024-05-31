package com.github.easy30.easymybatis;

import com.alibaba.fastjson.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        String s1 = "";
        String s2 = "";
        List<Object> list = new ArrayList<Object>();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (s1 != "") {
                s1 += ",";
                s2 += ",";
            }
            s1 += e.getKey();
           /* if (e!=null && ( e.getValue() instanceof JdbcNativeValue)) {
                s2 += e.getValue();
            }else{*/
                list.add(e.getValue());
                s2 += "#{"+ e.getKey()+"}";
            //}
        }

        MessageFormat fmt = new MessageFormat(sql);
        Object[] args = { s1, s2 };
        sql = fmt.format(args);

        return "<script>\n"+ sql+"\n</script>";
    }

   /* public String list3( @Param("ss") String sql,@Param("pp")Map params) {
        return  sql;
    }*/
}
