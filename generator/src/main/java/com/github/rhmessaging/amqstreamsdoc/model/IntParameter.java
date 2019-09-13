/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc.model;

public class IntParameter extends Parameter {
    private Integer minimum;
    private Integer maximum;

    public IntParameter(String name, String type, String doc, String defaultValue, BrokerUpdateMode brokerUpdateMode, Integer minimum, Integer maximum) {
        super(name, type, doc, defaultValue, brokerUpdateMode);
    }

    public Integer getMinimum() {
        return minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    @Override
    public boolean validateValue(String value) {
        int v;
        try {
            v = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return minimum != null ? v >= minimum : true
                && maximum != null ? v <= maximum : true;
    }
}
