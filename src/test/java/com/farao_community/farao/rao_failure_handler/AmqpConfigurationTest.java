/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_failure_handler;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class AmqpConfigurationTest {

    @Autowired
    private AmqpConfiguration amqpConfiguration;

    @Test
    void checkAmqpMessageConfiguration() {
        Assertions.assertThat(amqpConfiguration).isNotNull();
        Assertions.assertThat(amqpConfiguration.raoRequest()).isNotNull();
        Assertions.assertThat(amqpConfiguration.raoRequest().queueName()).isEqualTo("raoi-request-queue");
        Assertions.assertThat(amqpConfiguration.raoRequest().deliveryLimit()).isEqualTo(2);
        Assertions.assertThat(amqpConfiguration.raoResponse()).isNotNull();
        Assertions.assertThat(amqpConfiguration.raoResponse().exchange()).isEqualTo("raoi-response");
        Assertions.assertThat(amqpConfiguration.raoResponse().expiration()).isEqualTo("60000");
    }
}
