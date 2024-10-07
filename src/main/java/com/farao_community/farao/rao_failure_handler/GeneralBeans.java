/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_failure_handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class GeneralBeans {

    @Bean
    public Logger getLogger() {
        return  LoggerFactory.getLogger("RAO_FAILURE_HANDLER_BUSINESS_LOGGER");
    }
}
