/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_failure_handler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@ConfigurationProperties("rao-failure-handler.messages")
public record AmqpConfiguration(RaoRequestConfiguration raoRequest, RaoResponseConfiguration raoResponse) {
    public record RaoRequestConfiguration(String queueName, int deliveryLimit) {
    }
    public record RaoResponseConfiguration(String exchange, String expiration) {
    }
}
