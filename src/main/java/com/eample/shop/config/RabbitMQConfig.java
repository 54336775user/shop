package com.eample.shop.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_EXCHANGE = "shop.seckill.exchange";
    public static final String SECKILL_QUEUE = "shop.seckill.order.queue";
    public static final String SECKILL_ROUTING_KEY = "shop.seckill.order";
    public static final String SECKILL_DLX_EXCHANGE = "shop.seckill.dlx.exchange";
    public static final String SECKILL_DLQ = "shop.seckill.order.dlq";
    public static final String SECKILL_DLQ_ROUTING_KEY = "shop.seckill.order.dlq";

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .deadLetterExchange(SECKILL_DLX_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillBinding(Queue seckillQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue)
                .to(seckillExchange)
                .with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillDlxExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillDlq() {
        return QueueBuilder.durable(SECKILL_DLQ).build();
    }

    @Bean
    public Binding seckillDlqBinding(Queue seckillDlq, DirectExchange seckillDlxExchange) {
        return BindingBuilder.bind(seckillDlq)
                .to(seckillDlxExchange)
                .with(SECKILL_DLQ_ROUTING_KEY);
    }
}
