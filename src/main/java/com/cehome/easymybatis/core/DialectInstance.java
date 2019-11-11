package com.cehome.easymybatis.core;

import com.cehome.easymybatis.dialect.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.Configuration;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * coolma 2019/11/11
 **/
public class DialectInstance {
    private static Map<String, Class> map = new HashMap();
    private volatile AbstractDialect dialect = null;

    static {
        registerDialect("mysql", MysqlDialect.class);
        registerDialect("mariadb", MysqlDialect.class);
        registerDialect("hsqldb", HsqldbDialect.class);
        registerDialect("h2", H2Dialect.class);
        registerDialect("sqlite", SqliteDialect.class);

        registerDialect("postgresql",PostgresqlDialect.class);
        registerDialect("oracle", OracleDialect.class);
        registerDialect("db2", Db2Dialect.class);
        registerDialect("sqlserver", SqlserverDialect.class);
        registerDialect("derby", DerbyDialect.class);
        registerDialect("informix", InformixDialect.class);

    }

    public static void registerDialect(String name, Class dialectClass) {
        map.put(name, dialectClass);
    }

    private String name;
    private Configuration configuration;

    public DialectInstance(String name, Configuration configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    public AbstractDialect getInstance() {
        if (dialect != null) return dialect;
        Class c = null;
        synchronized (this) {
            if (dialect != null) return dialect;
            try {
                if (StringUtils.isNotBlank(this.name)) {
                    c = map.get(name);
                    if (c == null) throw new RuntimeException("Dialect class not found for name: " + this.name);

                } else {
                    Connection connection = null;
                    try {
                        connection = configuration.getEnvironment().getDataSource().getConnection();
                        String url = connection.getMetaData().getURL().toLowerCase();
                        for (String name : map.keySet()) {
                            if (url.startsWith("jdbc:" + name.toLowerCase())) {
                                c = map.get(name);
                                break;
                            }
                        }
                    } finally {
                        if (connection != null) connection.close();
                    }


                }

                dialect = (AbstractDialect) c.newInstance();
                return dialect;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }


}
