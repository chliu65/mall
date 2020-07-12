package com.lc.mallorder.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class RMQConfig {
//    @Autowired
//    private Parameters parameters;
//    @Bean
//    public Connection GetConnection(){
//        Connection connection=null;
//        try {
//            ConnectionFactory connectionFactory=new ConnectionFactory();
//            connectionFactory.setHost(parameters.getRmqHost());
//            connectionFactory.setPort(Integer.valueOf(parameters.getRmqPort()));
//            connectionFactory.setVirtualHost(parameters.getRmqVirtualHost());
//            connectionFactory.setUsername(parameters.getRmqUsername());
//            connectionFactory.setPassword(parameters.getRmqPassword());
//            connection=connectionFactory.newConnection();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return connection;
//    }
}
