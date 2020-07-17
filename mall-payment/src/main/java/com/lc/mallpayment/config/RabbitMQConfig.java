package com.lc.mallpayment.config;




import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    /**
     * 创建队列
     */
    @Bean
    public Queue paymentToOrderQueue(){
        return new Queue("paymentstatus");
    }

    /**
     *创建交换机
     */
    @Bean
    public Exchange OrderExchange(){
        return new DirectExchange("order",true,false);
    }

    /**
     * 创建绑定关系
     * @param queue
     * @param exchange
     * @return
     */
    @Bean
    public Binding orderQueueExchange(Queue queue,Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with("paymentstatus.to.order").noargs();
    }
}
