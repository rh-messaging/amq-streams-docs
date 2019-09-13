/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc.model;

import java.util.Collection;

public class ClassParameter extends Parameter {

    public ClassParameter(String name, String type, String doc, String defaultValue, BrokerUpdateMode brokerUpdateMode) {
        super(name, type, doc, defaultValue, brokerUpdateMode);
    }

    @Override
    public boolean validateValue(String value) {
        // TODO validate it's a legal classname?
        return true;
    }
}
