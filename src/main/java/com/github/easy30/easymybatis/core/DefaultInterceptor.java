package com.github.easy30.easymybatis.core;

import com.github.easy30.easymybatis.*;
import com.github.easy30.easymybatis.annotation.LimitOne;
import com.github.easy30.easymybatis.dialect.Dialect;
import com.github.easy30.easymybatis.utils.ObjectSupport;
import com.github.easy30.easymybatis.utils.Utils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * coolma 2019/11/1
 **/
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
})

public class DefaultInterceptor implements Interceptor {

    private static Logger logger = LoggerFactory.getLogger(DefaultInterceptor.class);
    private Map<String, MappedStatement> countMap = new ConcurrentHashMap();
    private Dialect dialect;
    private static ThreadLocal<Boolean> inPage = new ThreadLocal<>();
    private Map<String,ValueContainer<Method>> statementMethodMap=new ConcurrentHashMap<>();
    class ValueContainer<T> {
        T value;
        public ValueContainer(T value){
            this.value=value;
        }

    }
    public DefaultInterceptor(Dialect dialect) {
        this.dialect = dialect;
    }

  /*  public static MappedStatement getCurrentMappedStatement(){
        return mappedStatementHolder.get();
    }*/

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (inPage.get() != null) return invocation.proceed();

        final Object[] args = invocation.getArgs();

        MappedStatement statement = (MappedStatement) args[0];

        try {
            ConfigurationContext.set(statement.getConfiguration());
            // -- do with select
            if (statement.getSqlCommandType() == SqlCommandType.SELECT) {

                Page page = getPage(args[1]);
                Page contextPage = PageContext.get();
                boolean limitOne = isLimitOne(statement);

                //-- do with page  or  limit 1
                if (page != null || contextPage != null || limitOne) {
                    try {
                        // -- avoid recursively invoke :executor.query
                        inPage.set(true);
                        Object parameterObject = args[1];
                        RowBounds rowBounds = (RowBounds) args[2];
                        Executor executor = (Executor) invocation.getTarget();

                        BoundSql boundSql = statement.getBoundSql(parameterObject);
                        String sql = boundSql.getSql();

                        String pageSql = dialect.getPageSql(sql);
                        List<ParameterMapping> pms = dialect.getPageParameterMapping(statement.getConfiguration(), boundSql.getParameterMappings());

                        BoundSql pageBoundSql = new BoundSql(statement.getConfiguration(), pageSql, pms, parameterObject);
                        // fix "mybatis Expected one result (or null) to be returned by selectOne()"
                        // If method getByParams() ,getValueByParams() has more than one records , return the first one.
                        //@see org.apache.ibatis.session.defaults.DefaultSqlSession.selectOne(java.lang.String, java.lang.Object)
                        if (page == null) {
                            if (contextPage != null) {
                                page = contextPage;
                            } else { //limitOne
                                page = new Page(1, 1);
                            }
                            pageBoundSql.setAdditionalParameter(Const.PAGE, page);
                        }


                        copyAdditionalParameter(boundSql, pageBoundSql);
                        CacheKey cacheKey = executor.createCacheKey(statement, parameterObject, rowBounds, pageBoundSql);
                        //Class entityClass= EntityAnnotation.getInstanceByMapper(getMapperClass(statement.getId())).getEntityClass();
                        List list = executor.query(statement, parameterObject, rowBounds, null, cacheKey, pageBoundSql);

                        page.setData(list);
                        if (page.isQueryCount()) {
                            String countSql = dialect.getCountSql(sql);
                            BoundSql countBoundSql = new BoundSql(statement.getConfiguration(), countSql, boundSql.getParameterMappings(), parameterObject);
                            copyAdditionalParameter(boundSql, countBoundSql);
                            cacheKey = executor.createCacheKey(statement, parameterObject, rowBounds, countBoundSql);
                            int total = (Integer) executor.query(createMappedStatement(statement, Integer.class), parameterObject, rowBounds, null, cacheKey, countBoundSql).get(0);
                            page.setRecordCount(total);
                            page.setPageCount(total == 0 ? 0 : (total - 1) / page.getPageSize() + 1);
                        }

                        if (limitOne && list != null && list.size() > 1) {
                            list = list.subList(0, 1);
                        }
                        ListPage result = new ListPage(page.getPageIndex(), page.getPageSize());
                        result.setData(list);
                        result.setRecordCount(page.getRecordCount());
                        result.setPageCount(page.getPageCount());
                        result.setQueryCount(page.isQueryCount());
                        return result;

                    } finally {
                        if (contextPage != null) PageContext.remove();
                        inPage.remove();
                    }
                } else {
                    List list = (List) invocation.proceed();
                    Method m = getMethod(statement.getId());
                    if (!hasPage(m)) {
                        return list;
                    }

                    Page p = createListPage(list);
                    return p;

                }


            } else { //update
                if(Objects.equals(statement.getId(), QuickMapperImpl.class.getName()+"._insert")){
                    ObjectSupport.setFieldValue(statement, "keyProperties", new String[]{"id"});
                    ObjectSupport.setFieldValue(statement, "keyColumns",  new String[]{"id"});

                }
                return invocation.proceed();
            }
        }finally {
            ConfigurationContext.remove();
        }


    }

    private Page createListPage(List list) {
        int size= list ==null?0: list.size();
        Page p=new ListPage(1, size);
        p.setPageCount(size==0?0:1);
        p.setRecordCount(size);
        p.setData(list);
        return p;
    }

    /**
     * for  <foreach></foreach>  auto generate AdditionalParameter such as '__frch_item_0' '__frch_item_1'
     * so need to copy from source to target
     * BoundSql.parameterMappings :  all ? params in sql
     * BoundSql .parameterObject : all params name and values ( not only ? params）
     * BoundSql.additionalParameters ： temp  params by mybatis such as '__frch_item_0'... in <foreach></foreach>
     *
     * @param source
     * @param target
     */
    private void copyAdditionalParameter( BoundSql source , BoundSql  target){
        //parameterMappings include all ? params names(not value)
        for(ParameterMapping pm:source.getParameterMappings()){
            if(source.hasAdditionalParameter(pm.getProperty()) && !target.hasAdditionalParameter(pm.getProperty())){
                target.setAdditionalParameter(pm.getProperty(),source.getAdditionalParameter(pm.getProperty()));
            }
        }
    }

    private boolean hasPage( Method m){
        if(m!=null) {
            Class<?>[] parameterTypes = m.getParameterTypes();
            if (parameterTypes != null && parameterTypes.length > 0) {
                for (Class c : parameterTypes) {
                    if (Page.class.isAssignableFrom(c)) return true;
                }
            }
        }
        return false;

    }

    private Page getPage(Object arg) {
        Page page = null;
        if (arg!=null && arg instanceof  Map) {
            Map parameterObject = (Map) arg;

            for (Object value : parameterObject.values()) {
                if (value!=null && value instanceof Page) {
                    page = (Page) value;
                    break;
                }
            }

        }
        return page;
    }

    private boolean isLimitOne(MappedStatement statement) {
        Method m = getMethod(statement.getId());
        if(m==null) return false;
        LimitOne limitOne = m.getAnnotation(LimitOne.class);
        return limitOne != null;
    }

   /* private Page getPageLimitOne(MappedStatement statement, Object arg) {
        Page page = null;
        Method m = getMethod(statement.getId());
        ReturnFirst returnFirst = m.getAnnotation(ReturnFirst.class);
        if (returnFirst != null) {
            if (arg instanceof MapperMethod.ParamMap) {
                MapperMethod.ParamMap parameterObject = (MapperMethod.ParamMap) arg;
                page = new Page(1, 1);
                parameterObject.put(Const.PAGE, page);
            }
        }
        return page;
    }*/


    private Class getMapperClass(String id) {
        int n = id.lastIndexOf('.');
        String className = id.substring(0, n);
        String methodName = id.substring(n + 1);

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private Method getMethod(String id) {
        ValueContainer<Method> container = statementMethodMap.get(id);
        if(container!=null) return container.value;

        int n = id.lastIndexOf('.');
        String className = id.substring(0, n);
        String methodName = id.substring(n + 1);
        Class c = null;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.warn(""+e);
        }
        Method method=null;
        if(c!=null) {
            for (Method m : c.getMethods()) {
                if (m.getName().equals(methodName)) {
                    method=m;
                    break;
                }
            }
        }

        statementMethodMap.put(id,new ValueContainer<>(method));
        return method;
    }

    private MappedStatement createMappedStatement(final MappedStatement statement, final Class resultTypeClass) {
        String id = statement.getId() + "!count";
        MappedStatement result = countMap.get(id);
        if (result != null) return result;
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(statement.getConfiguration(),

                id, statement.getSqlSource(), statement.getSqlCommandType())
                .resource(statement.getResource())
                .fetchSize(statement.getFetchSize())
                .timeout(statement.getTimeout())
                .statementType(statement.getStatementType())

                .databaseId(statement.getDatabaseId())
                .lang(statement.getLang())
                .resultOrdered(statement.isResultOrdered())
                .resultSets(Utils.toString(statement.getResulSets(), ",", null))
                .resultMaps(new ArrayList() {
                    {
                        add(new ResultMap.Builder(statement.getConfiguration(), statement.getId(), resultTypeClass, new ArrayList()).build());
                    }
                })
                .resultSetType(statement.getResultSetType())
                .flushCacheRequired(statement.isFlushCacheRequired())
                .useCache(statement.isUseCache())
                .cache(statement.getCache());
        result = statementBuilder.build();
        countMap.put(id, result);
        return result;
    }


}
