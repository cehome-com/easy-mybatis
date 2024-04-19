package com.github.easy30.easymybatis.core;

import com.github.easy30.easymybatis.EasyConfiguration;
import org.apache.ibatis.session.Configuration;

/**
 *
 */
public class ConfigurationContext {
    protected static final ThreadLocal<Configuration> LOCAL = new ThreadLocal();

    public static void set(Configuration Configuration) {
        LOCAL.set(Configuration);
    }

    public static   Configuration get() {
        return (Configuration)LOCAL.get();
    }

    public static EasyConfiguration getEasyConfiguration(){
        Configuration configuration =  get();
        if(configuration!=null && configuration instanceof EasyConfiguration) return (EasyConfiguration) configuration;
        return null;
    }

    public static void remove() {
        LOCAL.remove();
    }
}
