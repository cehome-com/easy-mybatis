package com.github.easy30.easymybatis.core;


import com.github.easy30.easymybatis.dialect.*;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * coolma 2019/11/11
 **/
public class DialectFactory {
    private static Map<String, Class> map = new HashMap();
    //private volatile AbstractDialect dialect = null;

    static {
        registerDialect("mysql", MysqlDialect.class);
        registerDialect("mariadb", MysqlDialect.class);
        registerDialect("hsqldb", HsqldbDialect.class);
        registerDialect("h2", H2Dialect.class);
        registerDialect("sqlite", SqliteDialect.class);

        registerDialect("postgresql", PostgresqlDialect.class);
        registerDialect("oracle", OracleDialect.class);
        registerDialect("db2", Db2Dialect.class);
        registerDialect("sqlserver", SqlserverDialect.class);
        registerDialect("derby", DerbyDialect.class);
        registerDialect("informix", InformixDialect.class);

    }

    public static void registerDialect(String name, Class dialectClass) {
        map.put(name, dialectClass);
    }

    /*private String name;
    private Configuration configuration;

    public DialectFactory(String name, Configuration configuration) {
        this.name = name;
        this.configuration = configuration;
    }*/

    public static Dialect createDialect(String name) {
        //if (dialect != null) return dialect;
        Class c = null;
        if (StringUtils.isBlank(name)) throw new RuntimeException("Dialect name is blank");

        //if (dialect != null) return dialect;
        try {

            c = map.get(name);
            if (c == null) throw new RuntimeException("Dialect class not found for name: " + name);


            Dialect dialect = (Dialect) c.newInstance();
            return dialect;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static Dialect createDialect(DataSource dataSource) {
        Class c = null;

        try {

            Connection connection = null;
            String url = null;
            try {
                connection = dataSource.getConnection();
                url = connection.getMetaData().getURL().toLowerCase();
                for (String db : map.keySet()) {
                    if (url.startsWith("jdbc:" + db.toLowerCase())) {
                        c = map.get(db);
                        break;
                    }
                }
            } finally {
                if (connection != null) connection.close();
            }
            if (c == null) throw new RuntimeException("Not Dialect found for url " + url);


            Dialect dialect = (Dialect) c.newInstance();
            return dialect;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


}
