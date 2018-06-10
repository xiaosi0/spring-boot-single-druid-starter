package com.github.mini.druid;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.mysql.jdbc.StringUtils;

/**
 * @author bill
 */
@Configuration
@EnableConfigurationProperties({ DruidDataSourceProperties.class })
@ConditionalOnProperty(name = "spring.druid.datasource.url", matchIfMissing = false)
public class DruidAutoConfiguration {

    static String                     MAPPER_LOCATION = "classpath*:sqlmap/*Mapper.xml";

    @Autowired
    private DruidDataSourceProperties druidDataSourceProperties;

    @Bean(name = "defaultDruidDataSource", initMethod = "init", destroyMethod = "close")
    @ConditionalOnMissingBean(name = "defaultDruidDataSource")
    @Primary
    public DruidDataSource druidDataSource() throws Exception {
        DruidDataSource result = new DruidDataSource();
        result.setUrl(druidDataSourceProperties.getUrl());
        result.setUsername(druidDataSourceProperties.getUsername());
        result.setPassword(druidDataSourceProperties.getPassword());
        result.setConnectionProperties(
                "config.decrypt=true;config.decrypt.key=" + druidDataSourceProperties.getPwdPublicKey());
        result.setFilters("config");
        result.setMaxActive(druidDataSourceProperties.getMaxActive());
        result.setInitialSize(druidDataSourceProperties.getInitialSize());
        result.setMaxWait(druidDataSourceProperties.getMaxWait());
        result.setMinIdle(druidDataSourceProperties.getMinIdle());
        result.setTimeBetweenEvictionRunsMillis(druidDataSourceProperties.getTimeBetweenEvictionRunsMillis());
        result.setMinEvictableIdleTimeMillis(druidDataSourceProperties.getMinEvictableIdleTimeMillis());
        result.setValidationQuery(druidDataSourceProperties.getValidationQuery());
        result.setValidationQueryTimeout(druidDataSourceProperties.getValidationQueryTimeout());
        result.setTestWhileIdle(druidDataSourceProperties.isTestWhileIdle());
        result.setTestOnBorrow(druidDataSourceProperties.isTestOnBorrow());
        result.setTestOnReturn(druidDataSourceProperties.isTestOnReturn());
        result.setPoolPreparedStatements(druidDataSourceProperties.isPoolPreparedStatements());
        result.setMaxOpenPreparedStatements(druidDataSourceProperties.getMaxOpenPreparedStatements());

        if (druidDataSourceProperties.isEnableMonitor()) {
            StatFilter filter = new StatFilter();
            filter.setLogSlowSql(druidDataSourceProperties.isLogSlowSql());
            filter.setMergeSql(druidDataSourceProperties.isMergeSql());
            filter.setSlowSqlMillis(druidDataSourceProperties.getSlowSqlMillis());
            List<Filter> list = new ArrayList<Filter>();
            list.add(filter);
            result.setProxyFilters(list);
        }
        return result;
    }

    @Bean(name = "defaultTransactionManager")
    @ConditionalOnMissingBean(name = "defaultTransactionManager")
    @Primary
    public DataSourceTransactionManager transactionManager(@Qualifier("defaultDruidDataSource") DruidDataSource druidDataSource) {
        return new DataSourceTransactionManager(druidDataSource);
    }

    @Bean(name = "defaultTransactionTemplate")
    @ConditionalOnMissingBean(name = "defaultTransactionTemplate")
    @Primary
    public TransactionTemplate transactionTemplate(@Qualifier("defaultTransactionManager") PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @Bean(name = "defaultSqlSessionFactory")
    @ConditionalOnMissingBean(name = "defaultSqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("defaultDruidDataSource") DruidDataSource druidDataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(druidDataSource);
        if (!StringUtils.isNullOrEmpty(druidDataSourceProperties.getMybatisMapperLocations())) {
            MAPPER_LOCATION = druidDataSourceProperties.getMybatisMapperLocations();
        }
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION));
        SqlSessionFactory sqlSessionFactory = sessionFactory.getObject();
        sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(true);
        return sqlSessionFactory;
    }

    /**
     * 注册druid数据源统计servlet,查看监控数据源页面
     * <p>
     * url:http://ip:port/druid/index.html
     */
    @Bean(name = "druidServlet")
    @ConditionalOnProperty(name = "spring.druid.datasource.enable-monitor", havingValue = "true")
    @ConditionalOnMissingBean(name = "druidServlet")
    public ServletRegistrationBean registerDruidStatServlet() {
        return new ServletRegistrationBean(new com.alibaba.druid.support.http.StatViewServlet(), "/druid/*");
    }
}
