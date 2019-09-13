/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc.model;

public class BooleanParameter extends Parameter {
    public BooleanParameter(String name, String type, String doc, String defaultValue, BrokerUpdateMode brokerUpdateMode) {
        super(name, type, doc, defaultValue, brokerUpdateMode);
    }

    @Override
    public boolean validateValue(String value) {
        return false;
    }
}
