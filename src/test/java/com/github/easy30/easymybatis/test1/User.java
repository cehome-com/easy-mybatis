package com.github.easy30.easymybatis.test1;


import com.github.easy30.easymybatis.annotation.ColumnDefault;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Data
@Table(name = "user")
public class User {

    @Id
    private Long id;
    private String name;
    private Integer age;
    private String realName;
    @ColumnDefault(insertValue = "now()")
    private Date createTime;

    @ColumnDefault("now()")
    private Date updateTime;

    @Column(name="nick_name")
    private String nick;

}
