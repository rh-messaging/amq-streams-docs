/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc;

import org.apache.kafka.clients.consumer.internals.ConsumerMetrics;
import org.apache.kafka.clients.producer.internals.ProducerMetrics;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.MetricNameTemplate;
import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.connect.runtime.ConnectMetricsRegistry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MetricsDocGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Specify a config to generate docs for, one of [producer, consumer, metrics]");
        }
        for (String arg : args) {
            switch (arg) {
                case "producer":
                    producerMetrics();
                    break;
                case "consumer":
                    consumerMetrics();
                    break;
                case "connect":
                    connectMetrics();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown metrics " + arg);
            }
        }
    }

    private static void connectMetrics() throws Exception {
        ConnectMetricsRegistry metrics = new ConnectMetricsRegistry();
        documentMetrics("kafka.connect", metrics.getAllTemplates(), System.out);
    }

    private static void consumerMetrics() throws Exception {
        Set<String> tags = new HashSet<>();
        tags.add("client-id");
        ConsumerMetrics metricsRegistry = new ConsumerMetrics(tags, "consumer");

        Method method = ConsumerMetrics.class.getDeclaredMethod("getAllTemplates");
        method.setAccessible(true);
        documentMetrics("kafka.consumer",
                (Iterable<MetricNameTemplate>) method.invoke(metricsRegistry),
                System.out);
    }

    private static void producerMetrics() throws Exception {
        Map<String, String> metricTags = Collections.singletonMap("client-id", "client-id");
        MetricConfig metricConfig = new MetricConfig().tags(metricTags);
        Metrics metrics = new Metrics(metricConfig);

        ProducerMetrics metricsRegistry = new ProducerMetrics(metrics);
        Method method = ProducerMetrics.class.getDeclaredMethod("getAllTemplates");
        method.setAccessible(true);
        documentMetrics("kafka.producer",
                (Iterable<MetricNameTemplate>) method.invoke(metricsRegistry),
                System.out);
    }

    public static void documentMetrics(String domain, Iterable<MetricNameTemplate> allMetrics, Appendable out) throws Exception {
        Map<String, Map<String, String>> beansAndAttributes = new TreeMap<>();

        try (Metrics metrics = new Metrics()) {
            for (MetricNameTemplate template : allMetrics) {
                Map<String, String> tags = new LinkedHashMap<>();
                for (String s : template.tags()) {
                    tags.put(s, "{" + s + "}");
                }

                MetricName metricName = metrics.metricName(template.name(), template.group(), template.description(), tags);
                Method method = JmxReporter.class.getDeclaredMethod("getMBeanName", String.class, MetricName.class);
                method.setAccessible(true);
                String mBeanName = (String) method.invoke(null, domain, metricName);
                if (!beansAndAttributes.containsKey(mBeanName)) {
                    beansAndAttributes.put(mBeanName, new TreeMap<>());
                }
                Map<String, String> attrAndDesc = beansAndAttributes.get(mBeanName);
                if (!attrAndDesc.containsKey(template.name())) {
                    attrAndDesc.put(template.name(), template.description());
                } else {
                    throw new IllegalArgumentException("mBean '" + mBeanName + "' attribute '" + template.name() + "' is defined twice.");
                }
            }
        }



        for (Map.Entry<String, Map<String, String>> e : beansAndAttributes.entrySet()) {
            out.append("=== MBean `").append(e.getKey()).append("`\n\n");
            out.append("[options=\"header\"]\n");
            out.append("|=======\n");

            int pad = calcPad(e.getValue().keySet());
            out.append("| Attribute");
            pad(out, pad - "Attribute".length());
            out.append(" | Description\n");

            for (Map.Entry<String, String> attrAndDescription : e.getValue().entrySet()) {
                String attributeName = attrAndDescription.getKey();
                out.append("| ").append(attributeName);
                pad(out, pad - attributeName.length());
                out.append(" | ").append(attrAndDescription.getValue()).append("\n");
            }

            out.append("|=======\n\n");
        }
    }

    private static int calcPad(Iterable<String> stringsInColumn) {
        int pad = 0;
        for (String attributeName : stringsInColumn) {
            pad = Math.max(pad, attributeName.length());
        }
        return pad;
    }

    private static void pad(Appendable out, int pad) throws IOException {
        for (int i = 0; i < pad; i++) {
            out.append(' ');
        }
    }
}
