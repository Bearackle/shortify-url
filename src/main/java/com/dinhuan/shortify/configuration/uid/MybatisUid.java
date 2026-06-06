package com.dinhuan.shortify.configuration.uid;

import lombok.Getter;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Getter
@Configuration
@ComponentScan(basePackages = "com.baidu.fsg.uid")
@MapperScan(
        basePackages = "com.baidu.fsg.uid.worker.dao",
        sqlSessionFactoryRef = "uidSqlSessionFactory"
)
@EnableTransactionManagement
public class MybatisUid {

    @Bean(name = "uidSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:/META-INF/mybatis/mapper/WORKER*.xml")
        );
        return factory.getObject();
    }
    @Bean(name = "uidBatchSqlSession")
    public SqlSessionTemplate batchSqlSession(@Qualifier("uidSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }
}
