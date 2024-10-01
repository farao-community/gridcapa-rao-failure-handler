/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_failure_handler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class RaoFailureHandlerListenerTest {

    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();
    @Autowired
    private RaoFailureHandlerListener listener;

    @MockBean
    private RabbitTemplate amqpTemplate;

    @Test
    void checkThatMdcMetadataIsPropagatedCorrectly() {
        final Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        listener.addMetaDataToLogsModelContext("process-id", "request-id", "client-id", Optional.of("prefix"));
        logger.info("message");
        assertEquals(4, listAppender.list.get(0).getMDCPropertyMap().size());
        assertEquals("process-id", listAppender.list.get(0).getMDCPropertyMap().get("gridcapaTaskId"));
        assertEquals("request-id", listAppender.list.get(0).getMDCPropertyMap().get("computationId"));
        assertEquals("client-id", listAppender.list.get(0).getMDCPropertyMap().get("clientAppId"));
        assertEquals("prefix", listAppender.list.get(0).getMDCPropertyMap().get("eventPrefix"));
    }

    @Test
    void checkThatMdcMetadataIsPropagatedCorrectlyWithoutPrefix() {
        final Logger logger = (Logger) LoggerFactory.getLogger("LOGGER");
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        listener.addMetaDataToLogsModelContext("process-id", "request-id", "client-id", Optional.empty());
        logger.info("message");
        assertEquals(3, listAppender.list.get(0).getMDCPropertyMap().size());
        assertEquals("process-id", listAppender.list.get(0).getMDCPropertyMap().get("gridcapaTaskId"));
        assertEquals("request-id", listAppender.list.get(0).getMDCPropertyMap().get("computationId"));
        assertEquals("client-id", listAppender.list.get(0).getMDCPropertyMap().get("clientAppId"));
        assertNull(listAppender.list.get(0).getMDCPropertyMap().get("eventPrefix"));
    }

    @Test
    void onMessageWithSpecificReplyToValueTest() {
        final RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("idValue")
                .withEventPrefix("eventPrefixValue")
                .build();
        final byte[] body = jsonApiConverter.toJsonMessage(raoRequest);
        final MessageProperties messageProperties = MessagePropertiesBuilder.newInstance()
                .setReplyTo("replyToValue")
                .setAppId("appIdValue")
                .setCorrelationId("correlationIdValue")
                .build();
        final Message message = new Message(body, messageProperties);

        listener.onMessage(message);

        final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

        Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.eq("replyToValue"), messageArgumentCaptor.capture());
        Assertions.assertThat(messageArgumentCaptor.getValue()).isNotNull();
        Assertions.assertThat(messageArgumentCaptor.getValue().getBody()).isNotNull();
        Assertions.assertThat(new String(messageArgumentCaptor.getValue().getBody())).isEqualTo("The RAO request reached the maximum number of attempts but could not be processed successfully");
        final MessageProperties messagePropertiesFromResponse = messageArgumentCaptor.getValue().getMessageProperties();
        Assertions.assertThat(messagePropertiesFromResponse).isNotNull();
        Assertions.assertThat(messagePropertiesFromResponse.getCorrelationId()).isEqualTo("correlationIdValue");
        Assertions.assertThat(messagePropertiesFromResponse.getExpiration()).isEqualTo("60000");
    }

    @Test
    void onMessageWithoutReplyToValueTest() {
        final RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId("idValue")
                .withEventPrefix("eventPrefixValue")
                .build();
        final byte[] body = jsonApiConverter.toJsonMessage(raoRequest);
        final MessageProperties messageProperties = MessagePropertiesBuilder.newInstance()
                .setAppId("appIdValue")
                .setCorrelationId("correlationIdValue")
                .build();
        final Message message = new Message(body, messageProperties);

        listener.onMessage(message);

        final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

        Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.eq("raoi-response"), Mockito.eq(""), messageArgumentCaptor.capture());
        Assertions.assertThat(messageArgumentCaptor.getValue()).isNotNull();
        Assertions.assertThat(messageArgumentCaptor.getValue().getBody()).isNotNull();
        Assertions.assertThat(new String(messageArgumentCaptor.getValue().getBody())).isEqualTo("The RAO request reached the maximum number of attempts but could not be processed successfully");
        final MessageProperties messagePropertiesFromResponse = messageArgumentCaptor.getValue().getMessageProperties();
        Assertions.assertThat(messagePropertiesFromResponse).isNotNull();
        Assertions.assertThat(messagePropertiesFromResponse.getCorrelationId()).isEqualTo("correlationIdValue");
        Assertions.assertThat(messagePropertiesFromResponse.getExpiration()).isEqualTo("60000");
    }

    @Test
    void onMessageThrowsExceptionTest() {
        final byte[] body = new byte[]{};
        final MessageProperties messageProperties = MessagePropertiesBuilder.newInstance()
                .setReplyTo("replyToValue")
                .setAppId("appIdValue")
                .setCorrelationId("correlationIdValue")
                .build();
        final Message message = new Message(body, messageProperties);

        listener.onMessage(message);

        final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

        Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.eq("replyToValue"), messageArgumentCaptor.capture());
        Assertions.assertThat(messageArgumentCaptor.getValue()).isNotNull();
        Assertions.assertThat(messageArgumentCaptor.getValue().getBody()).isNotNull();
        Assertions.assertThat(new String(messageArgumentCaptor.getValue().getBody())).startsWith("Unhandled exception");
        final MessageProperties messagePropertiesFromResponse = messageArgumentCaptor.getValue().getMessageProperties();
        Assertions.assertThat(messagePropertiesFromResponse).isNotNull();
        Assertions.assertThat(messagePropertiesFromResponse.getCorrelationId()).isEqualTo("correlationIdValue");
        Assertions.assertThat(messagePropertiesFromResponse.getExpiration()).isEqualTo("60000");
    }
}