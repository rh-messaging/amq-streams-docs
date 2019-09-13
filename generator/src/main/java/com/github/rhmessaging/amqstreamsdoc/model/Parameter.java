/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc.model;

public abstract class Parameter {
    private final String name;
    private final String type;
    private final String defaultValue;
    private final BrokerUpdateMode brokerUpdateMode;
    private final String doc;

    public Parameter(String name, String type, String doc, String defaultValue, BrokerUpdateMode brokerUpdateMode) {
        this.name = name;
        this.type = type;
        this.doc = doc;
        this.defaultValue = defaultValue;
        this.brokerUpdateMode = brokerUpdateMode;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public BrokerUpdateMode getBrokerUpdateMode() {
        return brokerUpdateMode;
    }

    public abstract boolean validateValue(String value);
}
