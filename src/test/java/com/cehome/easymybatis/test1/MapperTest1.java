package com.cehome.easymybatis.test1;

import com.alibaba.fastjson.JSON;
import com.cehome.easymybatis.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class MapperTest1 {

    @Autowired
    DataSource dataSource;

    @Autowired
    UserMapper1 userMapper1;

    @Autowired
    SqlSessionTemplate sqlSessionTemplate;

    Long id;
    Integer age=25;
    String name="ma";
    String realName="coolma";

    @Before
    public void before(){
        setId();
    }

    @Test
    public void testAll()   {
        long t=System.currentTimeMillis();
        name="ma"+t;
        realName=realName+t;

        testDelete();
        testUpdate();
        testSelect();
    }
    @Test
    public void testSelect()   {


        setId();

        getByParams();
        getById();
        getValueByParams();
        getValueByWhere();
        listByParams();
        listBySQL();
        pageByEntity();
        pageBySQL();
    }

    @Test
    public void testUpdate()   {
        insert();
        update();
        updateByEntity();
        updateByWhere();

    }

    @Test
    public void testDelete()   {

        insert();deleteById();
        insert();deleteByEntity();
        insert();deleteByWhere();

    }
    @Test
    public void insert()   {
        User user = null;
        //-- insert into user(name,age,real_name) values('coolma',20,'mike')
        user = new User();
        user.setName(name);
        user.setAge(age);
        user.setRealName(realName);
        userMapper1.insert(user);
        id=user.getId();
    }

    /**
     *
     *
     *
     * @ 
     */
    @Test
    public void test()   {
        User user = null;
        //-- insert into user(name,age,real_name) values('coolma',20,'mike')
        user = new User();
        user.setName("coolma");
        user.setAge(20);
        user.setRealName("mike");
        user.setId(100L);
        userMapper1.insert(user);
        Long id = user.getId(); //return 100

        //-- update user set real_name='michael' where id=100
        user = new User();
        user.setRealName("michael");
        user.setId(100L);
        userMapper1.update(user);

        //-- update user set real_name='tom where id=100 and age=20
        user = new User();
        user.setRealName("tom");
        User params = new User();
        params.setId(100L);
        params.setAge(20);
        userMapper1.updateByParams(user, params);

        //-- delete from user where id=100
        userMapper1.deleteById(100L);


        //-- select one: select * from user where id=100
        user= userMapper1.getById(id,null);

        //-- select one: select * from user where real_name='tom'
        params=new User();
        params.setRealName("tom");
        user= userMapper1.getByParams(params,null);

        //-- list: select name,real_name from user where age=20
        params=new User();
        params.setAge(20);
        List<User> list=userMapper1.listByParams(params,null,"name,realName");

        //-- page: select name,real_name from user where age=20 order by name asc limit 0,20
        params=new User();
        params.setAge(20);
        Page<User> page=new Page(1,20);
        userMapper1.pageByParams(params,page,"name asc","name,realName");
        System.out.println(page.getData());

    }
    @Test
    public void update()   {
        User user=new User();
        user.setName("updateById");
        user.setId(id);
        //user.setValue("createTime","now()");
        Assert.assertEquals(1, userMapper1.update(user));
    }
    @Test
    public void updateByEntity()   {

        User user=new User();
        user.setName("updateByEntity");
        //user.setCreateTime(new Date());
        //user.setEmail("ube@a.com");

        User where=new User();
        where.setId(id);
        where.setAge(age);
        Assert.assertEquals(1, userMapper1.updateByParams(user,where));

    }
    @Test
    public void updateByWhere()   {

        User user=new User();
        user.setCreateTime(new Date());


        String where="{id}=#{id} and {realName}=#{realName}";
        Map map=new HashMap();
        map.put("id",id);
        map.put("realName",realName);

        int row= userMapper1.updateByWhere(user,where,map);
        System.out.println(row);

    }


    @Test
    public void deleteById()   {
        Assert.assertEquals(1, userMapper1.deleteById(id));


    }
    @Test
    public void deleteByEntity()   {

        User params=new User();
        params.setName(name);
        params.setAge(age);
        Assert.assertEquals(1, userMapper1.delete(params));


    }
    @Test
    public void deleteByWhere()   {


        String where="{name}=#{name} and {realName}=#{realName}";
        Map map=new HashMap();
        map.put("name",name);
        map.put("realName",realName);

        int row= userMapper1.deleteByWhere(where,map);
        Assert.assertEquals(1,row);

    }

    private void setId(){
        id = userMapper1.getValueByWhere( null, null,"max(id)");
    }


    @Test
    public void findById()   {

        //User user=userMapper.findById(36L);
        //System.out.println(JSON.toJSONString(user));
    }


    @Test
    public void getByParams()   {
        User params=new User();
        params.setId(id);
        //params.setAge(20);
        User user= userMapper1.getByParams(params,null);
        verify(user,id);

    }

 
    private void verify(User user, Long id){
        System.out.println(JSON.toJSONString(user));
        Assert.assertEquals(user.getId(),id);
        Assert.assertNotNull(user.getName());
    }

    @Test
    public void getById()   {
        //System.out.println(dataSource.getConnection().getMetaData().getURL());

        User user= userMapper1.getById(id,null);
        verify(user,id);
    }



    @Test
    public void getValueByParams()   {
        User params=new User();
        params.setId(id);
        Object value= userMapper1.getValueByParams(params,null);//"name");
        System.out.println(JSON.toJSONString(value));
        Assert.assertNotNull(value);


    }
    @Test
    public void getValueByWhere()   {

            User params = new User();
            params.setId(id);
            Object value = userMapper1.getValueByWhere( "{id}=#{id}", params,"name");
            System.out.println(JSON.toJSONString(value));
            Assert.assertNotNull(value);


    }

    @Test
    public void listByParams()   {
        User params=new User();
        params.setAge(20);
        List<User> list= userMapper1.listByParams(params," name asc, createTime desc","age,createTime");
        System.out.println(list.size()+"\r\n"+JSON.toJSONString(list));
        Assert.assertTrue(list.size()>0);

    }

    @Test
    public void pageByEntity()   {
        User params=new User();
        params.setAge(20);
        Page<User> page=new Page(1,3);
        List<User> list= userMapper1.pageByParams(params,page," name asc, createTime desc","age,createTime");
        System.out.println(page.getData().size()+"\r\n"+JSON.toJSONString(page));
        Assert.assertTrue(page.getData().size()>0);

    }

    @Test
    public void listBySQL()   {
        User params=new User();
        params.setAge(20);
        List<User> list= userMapper1.listBySQL(" age>#{age} order by {createTime} desc",params);
        System.out.println(list.size()+"\r\n"+JSON.toJSONString(list));
        Assert.assertTrue(list.size()>0);
    }


    @Test
    public void pageBySQL()   {
        User params=new User();
        params.setAge(age);
        Page<User> page=new Page(1,5);
        userMapper1.pageBySQL(" age>=#{age} order by {createTime} desc",params,page);
        System.out.println(page.getData().size()+"\r\n"+JSON.toJSONString(page));
        Assert.assertTrue(page.getData().size()>0);
    }




}