package com.github.easy30.easymybatis;

import com.github.easy30.easymybatis.annotation.EntitySelectKey;
import com.github.easy30.easymybatis.core.ColumnAnnotation;
import com.github.easy30.easymybatis.core.DefaultInterceptor;
import com.github.easy30.easymybatis.core.DialectFactory;
import com.github.easy30.easymybatis.core.EntityAnnotation;
import com.github.easy30.easymybatis.dialect.Dialect;
import com.github.easy30.easymybatis.utils.ObjectSupport;
import com.github.easy30.easymybatis.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import javax.persistence.Column;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * coolma 2019/10/25
 **/
@Slf4j
@Deprecated
public class MapperFactory implements BeanPostProcessor, InitializingBean, ApplicationContextAware {//}, ApplicationListener<ContextRefreshedEvent> {
    private static Boolean loaded = false;


    SqlSessionTemplate sqlSessionTemplate;
    SqlSessionFactory sqlSessionFactory;
    Configuration configuration;
    Set<String> methodNameSet = new HashSet<String>();
    //Map<String, AbstractMethodBuilder> methodBuilderMap = new HashMap<String, AbstractMethodBuilder>();
    private Dialect dialect;

    private String dialectName;


    private Class[] mapperInterfaces;
    private Map<String,Generation> generations=new ConcurrentHashMap<>();

    private boolean inited=false;
    private Map<Class,ResultMap> changeResultMapMap =Collections.synchronizedMap(new HashMap());
    public MapperFactory() {

    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(inited) return;
        if (sqlSessionFactory != null) configuration = sqlSessionFactory.getConfiguration();
        else if (sqlSessionTemplate != null) configuration =sqlSessionTemplate.getConfiguration();
        else throw new RuntimeException("SqlSessionTemplate or SqlSessionFactory not found");

        dialect =StringUtils.isNotBlank(dialectName)? DialectFactory.createDialect(dialectName)
                :DialectFactory.createDialect( configuration.getEnvironment().getDataSource());

        //-- default config
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setUseGeneratedKeys(true);

        configuration.addInterceptor(new DefaultInterceptor(dialect));
        inited=true;

    }

    /*private void addBuilders(Class c) throws Exception {
        for (Method method : c.getMethods()) {
            methodNameSet.add(method.getName());
            MethodBuilder mb = method.getAnnotation(MethodBuilder.class);
            if (mb == null || mb.value() == null) {
                if (!methodBuilderMap.containsKey(method.getName())) {
                    throw new RuntimeException("@MethodBuilder need for " + c.getName() + "." + method.getName());
                }
            }
            AbstractMethodBuilder methodBuilder = mb.value().newInstance();
            if (!method.getName().equals(methodBuilder.getMethodName().equals(method.getName())))
                throw new RuntimeException("method name not match, need" + method.getName() + ", but found "
                        + methodBuilder.getMethodName() + " in " + methodBuilder.getClass().getName());
            methodBuilderMap.put(methodBuilder.getMethodName(), methodBuilder);

        }
    }*/

    public Class[] getMapperInterfaces() {
        return mapperInterfaces;
    }

    public void setMapperInterfaces(Class[] mapperInterfaces) {
        this.mapperInterfaces = mapperInterfaces;
    }

    /*@Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (true) return;

        ApplicationContext context = event.getApplicationContext();

        initGenerators(context);

        Map<String, MapperFactoryBean> beanMap = context.getBeansOfType(MapperFactoryBean.class);

        //Map<String,Mapper> beanMap = event.getApplicationContext().getBeansOfType(Mapper.class);
        if (beanMap != null || beanMap.size() > 0) {
            for (Map.Entry<String, MapperFactoryBean> e : beanMap.entrySet()) {
                Class mapperClass = e.getValue().getObjectType();
                add(mapperClass);

            }
        }

    }

    private void initGenerators(ApplicationContext context) {
        Generations generations = Generations.getInstance();
        Map<String, Generation> beans = context.getBeansOfType(Generation.class);
        generations.putAll(beans);

    }*/

    public void setupMapper(Class mapperClass) {
        String namespace = mapperClass.getName();
        Class entityClass = ObjectSupport.getGenericInterfaces(mapperClass, 0, 0);
        if(EntityAnnotation.getInstanceOnly(entityClass)!=null) return;
        EntityAnnotation entityAnnotation = EntityAnnotation.getInstance(entityClass);
        entityAnnotation.setDialect(dialect);
        entityAnnotation.setMapperFactory(this);
        /*String resource = namespace.replace('.', '/') + ".java (best guess)";
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(configuration, resource);
        assistant.setCurrentNamespace(namespace);*/

        for (Method method : mapperClass.getMethods()) {
            //AbstractMethodBuilder methodBuilder= methodBuilderMap.get(method.getName());
            //if(methodBuilder!=null) methodBuilder.add(assistant,mapperClass,entityClass,entityAnnotation);
            if (method.isDefault() || Object.class.equals(method.getDeclaringClass())) continue;
            //System.out.println("=====" + namespace + "." + method.getName());
            MappedStatement ms = configuration.getMappedStatement(namespace + "." + method.getName());
            if (ms != null) {
                if (ms.getSqlCommandType().equals(SqlCommandType.INSERT)) {
                    doKeyGenerator(mapperClass, entityClass, method, ms);

                }else  if (ms.getSqlCommandType().equals(SqlCommandType.SELECT)) {
                    changeResultMaps(ms);

                }

            }


        }
    }

    /**
     * find @Column(name="ccc"), build prop and column name mapping;
     * @param ms
     */
    private void changeResultMaps(MappedStatement ms) {
        List<ResultMap> resultMaps = ms.getResultMaps();
        if (CollectionUtils.isEmpty(resultMaps)) return;
        List<ResultMap> newResultMaps = new ArrayList<>();
        boolean change = false;
        for (ResultMap resultMap : resultMaps) {
            Class<?> resultClass = resultMap.getType();
            ResultMap newResultMap = changeResultMapMap.get(resultClass);
            if (newResultMap == null) {
                List<ResultMapping> newResultMappings = getColumnResultMappings(resultClass);
                if (newResultMappings.size() > 0) {
                    //keep old ResultMapping . resultMap.getResultMappings() mostly  empty
                    if (resultMap.getResultMappings() != null) {
                        Set<String> propSet = newResultMappings.stream().map(ResultMapping::getProperty).collect(Collectors.toSet());
                        for (ResultMapping oldResultMapping : resultMap.getResultMappings()) {
                            if(!propSet.contains(oldResultMapping.getProperty())){
                                newResultMappings.add(oldResultMapping);
                            }
                        }
                    }

                    ResultMap.Builder newResultMapBuilder = new ResultMap.Builder(configuration, resultMap.getId(), resultMap.getType(), newResultMappings, resultMap.getAutoMapping());
                    newResultMap = newResultMapBuilder.build();
                    changeResultMapMap.put(resultClass, newResultMap);

                }
            }
            if (newResultMap == null) {
                newResultMaps.add(resultMap);
            } else {
                change = true;
                newResultMaps.add(newResultMap);
            }
        }
        if (change) {
            ObjectSupport.setField(ms, "resultMaps", newResultMaps);
        }


    }

    private List<ResultMapping> getColumnResultMappings(Class<?> resultClass) {
        PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(resultClass);
        List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
        for (PropertyDescriptor pd : propertyDescriptors) {
            Column column = ObjectSupport.getAnnotation(Column.class, resultClass, pd);
            if (column == null || StringUtils.isBlank(column.name())) continue;
            ResultMapping.Builder builder = new ResultMapping.Builder(configuration, pd.getName(), column.name(), pd.getPropertyType());
            resultMappings.add(builder.build());
        }
        return resultMappings;
    }


    private void doKeyGenerator(Class mapperClass, Class entityClass, Method method, MappedStatement ms) {
        log.debug("------ mapper:{},entity:{},method:{}",mapperClass,entityClass,method);
        KeyGenerator keyGenerator = ms.getKeyGenerator();
        // SelectKey exists ,so do nothing
        if (keyGenerator != null && keyGenerator instanceof SelectKeyGenerator) return;

        final EntityAnnotation entityAnnotation = EntityAnnotation.getInstance(entityClass);
        final EntitySelectKey entitySelectKey = entityAnnotation.getEntitySelectKey();

        List<String> idPropertyNames = entityAnnotation.getIdPropertyNames();
        List<String> idColumnNames = entityAnnotation.getIdColumnNames();
        if (idPropertyNames.size() == 0) return;


        if (entitySelectKey != null) {

            //org.apache.ibatis.builder.annotation.MapperAnnotationBuilder.handleSelectKeyAnnotation
            SelectKey selectKey = new SelectKey() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return SelectKey.class;
                }

                @Override
                public String[] statement() {
                    return entitySelectKey.statement();
                }

                @Override
                public String keyProperty() {
                    if (!StringUtils.isBlank(entitySelectKey.keyProperty())) return entitySelectKey.keyProperty();
                    if (entityAnnotation.getIdPropertyNames().size() > 1) {

                        throw new RuntimeException("keyProperty can not be empty for multiple columns key");

                    }
                    return Utils.toString(entityAnnotation.getIdPropertyNames(), ",", null);
                }

                @Override
                public String keyColumn() {
                    if (!StringUtils.isBlank(entitySelectKey.keyColumn())) return entitySelectKey.keyColumn();
                    if (!StringUtils.isBlank(entitySelectKey.keyProperty())) {
                        String kc = "";
                        Map<String, ColumnAnnotation> propertyColumnMap = entityAnnotation.getPropertyColumnMap();
                        for (String p : entitySelectKey.keyProperty().split(",")) {
                            if (kc.length() > 0) kc += ",";
                            kc += propertyColumnMap.get(p).getName();
                        }
                        return kc;
                    }
                    return Utils.toString(entityAnnotation.getIdColumnNames(), ",", null);


                }

                @Override
                public boolean before() {
                    return entitySelectKey.before();
                }

                @Override
                public Class<?> resultType() {
                    return entitySelectKey.resultType();
                }

                @Override
                public StatementType statementType() {
                    return entitySelectKey.statementType();
                }

                //@Override  high version use, maybe > 3.5.6
                public String databaseId() {
                    return dialectName;
                }
            };


            MapperAnnotationBuilder builder = new MapperAnnotationBuilder(configuration, mapperClass);

            //invok private method : handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId, Class<?> parameterTypeClass, LanguageDriver languageDriver)

            Method handleSelectKeyAnnotationMethod = ObjectSupport.getMethod(MapperAnnotationBuilder.class, "handleSelectKeyAnnotation");

            Method getParameterTypeMethod = ObjectSupport.getMethod(MapperAnnotationBuilder.class, "getParameterType");
            getParameterTypeMethod.setAccessible(true);

            handleSelectKeyAnnotationMethod.setAccessible(true);
            try {
                keyGenerator = (KeyGenerator) handleSelectKeyAnnotationMethod.invoke(builder, selectKey, ms.getId(),
                        getParameterTypeMethod.invoke(builder, method), ms.getLang());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }

        if (entitySelectKey == null) keyGenerator = Jdbc3KeyGenerator.INSTANCE;
        // set private field
        String[] keyProperties = new String[idPropertyNames.size()];
        for (int i = 0; i < idPropertyNames.size(); i++) {
            keyProperties[i] = "e." + idPropertyNames.get(i);
        }

        ObjectSupport.setFieldValue(ms, "keyProperties", keyProperties);
        ObjectSupport.setFieldValue(ms, "keyColumns", idColumnNames.toArray(new String[0]));
        ObjectSupport.setFieldValue(ms, "keyGenerator", keyGenerator);


    }

    public String getDialectName() {
        return dialectName;
    }

    public void setDialectName(String dialectName) {
        this.dialectName = dialectName;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        /*if (bean instanceof Generation) {
            generations.put(beanName, (Generation) bean);
        }*/
        //初始化实体类
        if (bean instanceof SqlSessionDaoSupport) {
            setupMapperBean((SqlSessionDaoSupport)bean);
        }

        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String, SqlSessionDaoSupport> beans = applicationContext.getBeansOfType(SqlSessionDaoSupport.class);
        for(SqlSessionDaoSupport bean:beans.values()){
            setupMapperBean(bean);

        }
    }

    /**
     * 缺省是org.mybatis.spring.mapper.MapperFactoryBean , 但如果存在其它第三方框架可能有自己
     * @param bean
     */
    private void setupMapperBean(SqlSessionDaoSupport bean){
        if(bean instanceof FactoryBean  ) {
                Class mapperClass = ((FactoryBean) bean).getObjectType();
                if(Mapper.class.isAssignableFrom(mapperClass)) {
                    setupMapper(mapperClass);
                }

        }
    }

    public Map<String, Generation> getGenerations() {
        return generations;
    }

    /**
     * put generations
     * @param generations
     */
    public void setGenerations(Map<String, Generation> generations) {
        this.generations.putAll(generations);
    }

/*private void dd(String resourceLocation){
        XMLMapperBuilder
        ResourceUtils.getFile(resourceLocation)
    }*/
}
