## introduce
A mybatis generator plugin extends package

usage see:
devtool-example

#### Example for mybatis generator plugin use

```xml
<!--mybatis generator plugin-->
<plugin>
    <groupId>org.mybatis.generator</groupId>
    <artifactId>mybatis-generator-maven-plugin</artifactId>
    <version>1.4.0</version>
    <configuration>
        <!--<configurationFile>${basedir}/mybatis-generator/generatorConfig.xml</configurationFile>-->
        <overwrite>true</overwrite>
        <verbose>true</verbose>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.46</version>
        </dependency>
        //use
        <dependency>
            <groupId>pro.leaco.dev</groupId>
            <artifactId>devtool</artifactId>
            <version>0.0.1</version>
        </dependency>
    </dependencies>
</plugin>
```


#### Example for jooq generator plugin use

```xml
<!-- jooq generator plugin -->
<plugin>
<groupId>org.jooq</groupId>
<artifactId>jooq-codegen-maven</artifactId>
<version>${jooq.version}</version>
<configuration>
    <jdbc>
        <driver>com.mysql.cj.jdbc.Driver</driver>
        <url>jdbc:mysql://127.0.0.1:3306/database?serverTimezone=Asia/Shanghai</url>
        <user>root</user>
        <password>password</password>
    </jdbc>
    <generator>
        <name>pro.leaco.jooq.generator.FJavaGenerator</name>
        <generate>
            <pojos>true</pojos>
            <interfaces>true</interfaces>
            <daos>true</daos>
            <springAnnotations>true</springAnnotations>
        </generate>
        <database>
            <inputSchema>//TODO</inputSchema>
            <includes>biz_.*</includes>
        </database>
        <target>
            <packageName>com.xx.xx.xx</packageName>
            <directory>${basedir}/src/main/java</directory>
        </target>
    </generator>
</configuration>
<dependencies>
    //use
    <dependency>
        <groupId>pro.leaco.dev</groupId>
        <artifactId>devtool</artifactId>
        <version>0.0.1</version>
    </dependency>
</dependencies>
</plugin>
```
