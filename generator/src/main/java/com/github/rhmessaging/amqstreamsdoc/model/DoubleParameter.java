/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc.model;

public class DoubleParameter extends Parameter {
    private Double minimum;
    private Double maximum;

    public DoubleParameter(String name, String type, String doc, String defaultValue, BrokerUpdateMode brokerUpdateMode, Double minimum, Double maximum) {
        super(name, type, doc, defaultValue, brokerUpdateMode);
    }

    public Double getMinimum() {
        return minimum;
    }

    public Double getMaximum() {
        return maximum;
    }

    @Override
    public boolean validateValue(String value) {
        double v;
        try {
            v = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return minimum != null ? v >= minimum : true
                && maximum != null ? v <= maximum : true;
    }
}
