package com.spring.app.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
/*
  @EnableTransactionManagement
  - @Transactional 을 인식하여 트랜잭션 AOP 프록시를 적용한다.
  - 서비스 계층에서 여러 DB 작업을 "한 단위 작업"으로 묶어 커밋/롤백을 자동 처리할 수 있게 한다.
*/
public class DataSourceConfig {

    /*
      mybatis.mapper-locations
      - MyBatis mapper XML 파일들의 위치(패턴)를 설정한다.
      - SqlSessionFactoryBean#setMapperLocations 에 전달되어 mapper XML을 로딩한다.
    */
    @Value("${mybatis.mapper-locations}")
    private String mapperLocations;

    /*
      DataSource Bean
      - DB 접속 정보(url, username, password, driver-class-name 등)를 담는 커넥션 팩토리.
      - @ConfigurationProperties(prefix=...)
        application.yml 의 spring.datasource-final_orauser1.* 값을 자동 바인딩하여 DataSource를 만든다.
      
      참고) @ConfigurationProperties 를 쓰려면(옵션)
      - build.gradle 에 configuration-processor 를 추가하면 메타데이터 자동완성에 도움이 된다.
        annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    */
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource-finalorauser1")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    /*
      SqlSessionFactory Bean
      - MyBatis의 핵심 객체(세션을 만드는 공장).
      - 어떤 DataSource를 사용할지, mybatis-config.xml 과 mapper XML 위치를 여기서 연결한다.

      setConfigLocation:
      - MyBatis 전역 설정(mybatis-config.xml) 위치.
      - typeAliases, settings 등 공통 설정을 읽는다.

      setMapperLocations:
      - mapper XML 들을 로딩하는 위치 패턴.
      - yml의 mybatis.mapper-locations 값을 그대로 적용한다.
    */
    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(
            @Qualifier("dataSource") DataSource dataSource,
            ApplicationContext applicationContext) throws Exception {

        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);

        // MyBatis 전역 설정 파일
        bean.setConfigLocation(applicationContext.getResource("classpath:mybatis/mybatis-config.xml"));

        // Mapper XML 파일들 로딩
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(mapperLocations)
        );

        return bean.getObject();
    }

    /*
      SqlSessionTemplate Bean
      - MyBatis SqlSession의 Spring 연동 버전(스레드 세이프).
      - DAO에서 selectList("namespace.id"), insert(...) 등을 호출할 때 내부적으로 이 템플릿이 사용된다.

      참고) 주입 방식 차이
      - @Autowired SqlSessionTemplate st;  (이름/Qualifier 지정 없으면, Bean 이름 기준으로 찾아 주입)
      - @Autowired @Qualifier("sqlSessionTemplate") SqlSessionTemplate st; 처럼 명시도 가능
    */
    @Bean(name = "sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(
            @Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {

        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /*
      TransactionManager Bean
      - @Transactional 이 실제로 "커밋/롤백"을 수행할 때 사용하는 매니저.
      - DB가 1개면 @Transactional(value="...") 로 특정 매니저를 지정할 필요가 없다.

      주의) DataSource를 직접 dataSource()로 호출하기보다,
           스프링이 관리하는 Bean을 주입받아 사용하는 편이 더 명확하고 안전하다.
    */
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("dataSource") DataSource dataSource) {

        return new DataSourceTransactionManager(dataSource);
    }

    /*
      [트랜잭션 적용 예시]
      @Service
      public class SampleService {

          private final SampleDao dao;

          public SampleService(SampleDao dao) {
              this.dao = dao;
          }

          @Transactional
          public void doWork() {
              dao.insertA();
              dao.insertB();
              // 중간에 예외가 발생하면 insertA/insertB 모두 롤백
          }
      }

      참고)
      - TransactionManager 가 여러 개일 때만 @Transactional(value="특정매니저빈이름") 이 필요하다.
      - 현재 프로젝트처럼 DB가 1개면 기본 transactionManager가 사용된다.
    */
}
