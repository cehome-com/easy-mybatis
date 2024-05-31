package com.github.easy30.easymybatis.core;

import com.alibaba.fastjson.JSONObject;
import com.github.easy30.easymybatis.QuickProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Statement;

/**
 *  非实体类的Map插入时候,动态指定id
 */
public class QuickJdbc3KeyGenerator extends Jdbc3KeyGenerator {
    public static final Jdbc3KeyGenerator INSTANCE = new QuickJdbc3KeyGenerator();
    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        JSONObject options = QuickProvider.getOptions();
        if(options!=null){
           String keyColumns= options.getString(QuickProvider.KEY_KEY_COLUMNS);
           if(StringUtils.isNotBlank(keyColumns)){
               MappedStatement.Builder msBuilder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), ms.getSqlSource(), ms.getSqlCommandType());
               msBuilder.keyProperty(keyColumns);
               msBuilder.keyColumn(keyColumns);
               ms=msBuilder.build();
           }
        }

        super.processAfter(executor,ms, stmt, parameter);
    }
}
