/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_failure_handler;

import com.farao_community.farao.rao_runner.api.JsonApiConverter;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.stereotype.Component;

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

    public RaoFailureHandlerListener(AmqpTemplate amqpTemplate, Logger businessLogger, AmqpConfiguration amqpConfiguration) {
        this.amqpTemplate = amqpTemplate;
        this.businessLogger = businessLogger;
        this.amqpConfiguration = amqpConfiguration;
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
            sendRaoFailedResponse(raoRequest.getId(), "The RAO request reached the maximum number of attempts but could not be processed successfully", replyTo, brokerCorrelationId);
        } catch (Exception e) {
            sendRaoFailedResponse("defaultId", "Unhandled exception: " + e.getMessage(), replyTo, brokerCorrelationId);
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

    private void sendRaoFailedResponse(final String id, final String errorMessage, final String replyTo, final String correlationId) {
        final RaoFailureResponse response = new RaoFailureResponse.Builder()
                .withId(id)
                .withErrorMessage(errorMessage)
                .build();
        final Message responseMessage = MessageBuilder.withBody(jsonApiConverter.toJsonMessage(response))
                .andProperties(buildMessageResponseProperties(correlationId, response.isRaoFailed()))
                .build();
        sendMessage(replyTo, responseMessage);
    }

    private void sendMessage(final String replyTo, final Message responseMessage) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, responseMessage);
        } else {
            amqpTemplate.send(amqpConfiguration.raoResponse().exchange(), "", responseMessage);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Response message: {}", new String(responseMessage.getBody()));
        }
    }

    private MessageProperties buildMessageResponseProperties(final String correlationId, final boolean failed) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(APPLICATION_ID)
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setCorrelationId(correlationId)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(amqpConfiguration.raoResponse().expiration())
                .setPriority(PRIORITY)
                .setHeaderIfAbsent("rao-failure", failed)
                .build();
    }
}
