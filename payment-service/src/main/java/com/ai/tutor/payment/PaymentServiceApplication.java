package com.ai.tutor.payment;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import com.ai.tutor.common.integration.BrokerageOrderFacade;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@EnableFeignClients(basePackages = "com.ai.tutor.payment.integration.feign")
@SpringBootApplication(scanBasePackages = {"com.ai.tutor.payment", "com.ai.tutor.common"})
@MapperScan("com.ai.tutor.payment.**.mapper")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean(RocketMQTemplate.class)
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() {
        return new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("No DataSource configured");
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                throw new SQLException("No DataSource configured");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(BrokerageOrderFacade.class)
    public BrokerageOrderFacade brokerageOrderFacade() {
        return (brokerageOrderId, uid) -> {
            throw new IllegalStateException("BrokerageOrderFacade not configured");
        };
    }
}
