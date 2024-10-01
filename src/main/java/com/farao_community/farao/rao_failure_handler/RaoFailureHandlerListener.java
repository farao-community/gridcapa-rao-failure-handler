/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_failure_handler;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class RaoFailureHandlerListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoFailureHandlerListener.class);
    private static final String APPLICATION_ID = "rao-failure-handler";
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";
    private static final int PRIORITY = 1;

    private final AmqpTemplate amqpTemplate;
    private final Logger businessLogger;
    private final JsonApiConverter jsonApiConverter;
    private final AmqpConfiguration amqpConfiguration;
    private final Exchange raoResponseExchange;

    public RaoFailureHandlerListener(AmqpTemplate amqpTemplate, Logger businessLogger, AmqpConfiguration amqpConfiguration, Exchange raoResponseExchange) {
        this.amqpTemplate = amqpTemplate;
        this.businessLogger = businessLogger;
        this.amqpConfiguration = amqpConfiguration;
        this.raoResponseExchange = raoResponseExchange;
        this.jsonApiConverter = new JsonApiConverter();
    }

    @Override
    public void onMessage(final Message message) {
        final String replyTo = message.getMessageProperties().getReplyTo();
        final String brokerCorrelationId = message.getMessageProperties().getCorrelationId();

        try {
            final RaoRequest raoRequest = jsonApiConverter.fromJsonMessage(message.getBody(), RaoRequest.class);
            LOGGER.info("Failed RAO request received from DLQ: {}", raoRequest);
            addMetaDataToLogsModelContext(raoRequest.getId(), brokerCorrelationId, message.getMessageProperties().getAppId(), raoRequest.getEventPrefix());
            businessLogger.error("The RAO request reached the maximum number of attempts but could not be processed successfully");
            sendErrorResponse("The RAO request reached the maximum number of attempts but could not be processed successfully", replyTo, brokerCorrelationId);
        } catch (Exception e) {
            sendErrorResponse("Unhandled exception: " + e.getMessage(), replyTo, brokerCorrelationId);
        }
    }

    void addMetaDataToLogsModelContext(final String gridcapaTaskId, final String computationId, final String clientAppId, final Optional<String> optPrefix) {
        MDC.put("gridcapaTaskId", gridcapaTaskId);
        MDC.put("computationId", computationId);
        MDC.put("clientAppId", clientAppId);
        if (optPrefix.isPresent()) {
            MDC.put("eventPrefix", optPrefix.get());
        } else {
            MDC.remove("eventPrefix");
        }
    }

    private void sendErrorResponse(final String errorMessageText, final String replyTo, final String correlationId) {
        final byte[] errorMessageJson = errorMessageText.getBytes(StandardCharsets.UTF_8);
        final MessageProperties messageProperties = buildMessageResponseProperties(correlationId);
        final Message errorResponse = MessageBuilder.withBody(errorMessageJson).andProperties(messageProperties).build();
        if (replyTo != null) {
            amqpTemplate.send(replyTo, errorResponse);
        } else {
            amqpTemplate.send(raoResponseExchange.getName(), "", errorResponse);
        }
    }

    private MessageProperties buildMessageResponseProperties(final String correlationId) {
        return MessagePropertiesBuilder.newInstance()
            .setAppId(APPLICATION_ID)
            .setContentEncoding(CONTENT_ENCODING)
            .setContentType(CONTENT_TYPE)
            .setCorrelationId(correlationId)
            .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
            .setExpiration(amqpConfiguration.raoResponse().expiration())
            .setPriority(PRIORITY)
            .build();
    }
}
