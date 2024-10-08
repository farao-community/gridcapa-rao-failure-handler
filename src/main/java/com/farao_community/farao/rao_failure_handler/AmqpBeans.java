/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_failure_handler;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Configuration
public class AmqpBeans {
    private final AmqpConfiguration amqpConfiguration;

    public AmqpBeans(AmqpConfiguration amqpConfiguration) {
        this.amqpConfiguration = amqpConfiguration;
    }

    @Bean
    public Queue raoRequestQueue() {
        return new Queue(amqpConfiguration.raoRequest().queueName());
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory, Queue raoRequestQueue, RaoFailureHandlerListener listener) {
        final SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(raoRequestQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        simpleMessageListenerContainer.setPrefetchCount(1);
        return simpleMessageListenerContainer;
    }
}
