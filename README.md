# MyBatis3.4

需要导入模块：需要将mybatis-parent模块导入。mybatis-parent模块链接 https://github.com/mybatis/parent
## 基础支持层

### 1.解析器模块
org.apache.ibatis.parsing
xml解析，$、{} 格式的字符串解析
源码分析可以参考http://www.cnblogs.com/sunzhenchao/p/3161093.html

###2.反射工具
 - org.apache.ibatis.reflection
 - org.apache.ibatis.reflection.factory
 - org.apache.ibatis.reflection.invoker
 - org.apache.ibatis.reflection.property
 - org.apache.ibatis.reflection.wrapper

可以参考MetaObjectTest来跟踪调试，基本上用到了reflection包下所有的类

### 3.类型转换
 - org.apache.ibatis.type 实现java和jdbc中的类型之间转换

源码分析可以参考http://www.cnblogs.com/sunzhenchao/archive/2013/04/09/3009431.html

### 4.日志模块  

> org.apache.ibatis.logging

适配器设计模式
设计模式可参考http://www.cnblogs.com/liuling/archive/2013/04/12/adapter.html

### 5.资源加载
org.apache.ibatis.io

单例模式

MyBatis的IO包中封装了CLassLoader以及读取资源文件的相关API

通过类加载器在jar包中寻找一个package下满足条件(比如某个接口的子类)的所有类

### 6.数据源  

org.apache.ibatis.datasource

org.apache.ibatis.datasource.jndi

org.apache.ibatis.datasource.pooled

org.apache.ibatis.datasource.unpooled

工厂方法模式

### 7.事务
 - org.apache.ibatis.transaction
 - org.apache.ibatis.transaction.jdbc
 - org.apache.ibatis.transaction.managed

工厂方法模式

### 8.绑定
 - org.apache.ibatis.binding

核心模块之一

### 9.缓存模块
 - org.apache.ibatis.cache

装饰器模式

## 核心处理层

### 1.Mybatis初始化
- org.apache.ibatis.session

构建者模式(生成器模式) SqlSessionFactoryBuilder.build() 入口

### 99.执行器  
- org.apache.ibatis.executor

## 其他
### 1.异常  
- org.apache.ibatis.exceptions

### 3.会话  
- org.apache.ibatis.session

### 4.jdbc单元测试工具  
- org.apache.ibatis.jdbc

### 5.构建  
- org.apache.ibatis.builder
- org.apache.ibatis.builder.annotation
- org.apache.ibatis.builder.xml

### 6.映射  
- org.apache.ibatis.mapping

### 7.脚本  
- org.apache.ibatis.scripting
- org.apache.ibatis.scripting.defaults
- org.apache.ibatis.scripting.xmltags

### 8.注解  
- org.apache.ibatis.annotations

## 高级主题
### 1.插件  
- org.apache.ibatis.plugin