/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc.model;

import java.util.Collection;

public class StringParameter extends Parameter {
    private final Collection<String> validValues;

    public StringParameter(String name, String type, String doc, String defaultValue, BrokerUpdateMode brokerUpdateMode, Collection<String> validValues) {
        super(name, type, doc, defaultValue, brokerUpdateMode);
        this.validValues = validValues;
    }

    public Collection<String> getValidValues() {
        return validValues;
    }

    @Override
    public boolean validateValue(String value) {
        return validValues.contains(value);
    }
}
