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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

public class MetricsDocGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: objectName [file...]");
        }
        HashMap<String, Supplier<Map<ObjectName, Map<String, String>>>> c = new HashMap<>();
        c.put("kafka.producer", MetricsDocGenerator::producerMetrics);
        c.put("kafka.consumer", MetricsDocGenerator::consumerMetrics);
        c.put("kafka.connect", MetricsDocGenerator::connectMetrics);

        ObjectName name = objectName(args[0]);

        TreeMap<String, String> attributes = new TreeMap<>();
        for (int i = 1; i < args.length; i++) {
            File file = new File(args[i]);
            Map<String, String> map = loadCommon(file);
            System.err.println("Loaded " + map.keySet() + " from " + file);
            attributes.putAll(map);
            // TODO handle multi files
        }

        Supplier<Map<ObjectName, Map<String, String>>> fn = c.get(name.getDomain());
        if (fn != null) {
            Map<ObjectName, Map<String, String>> objectNameMapMap = fn.get();
            System.err.println("Introspectable names: " + objectNameMapMap.keySet());
            Map<String, String> map = objectNameMapMap.get(name);
            if (map != null) {
                System.err.println("Introspected " + map.keySet() + " from " + fn);
                attributes.putAll(map);
            } else {
                System.err.println("Introspected nothing from " + fn);
            }
        } else {
            System.err.println("Introspection not supported for " + name.getDomain());
        }

        formatTable(System.out, name, attributes);
    }

    private static ObjectName objectName(String objectName) {
        ObjectName name;
        try {
            name = ObjectName.getInstance(objectName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Malformed ObjectName: " + objectName, e);
        }
        return name;
    }

    private static Map<String, String> loadCommon(File file) throws IOException {
        Map<String, String> result = new TreeMap<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line = r.readLine();
            while (line != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] attrDescRubbish = line.split("\t");
                    String attr = attrDescRubbish[0].trim();
                    String desc = attrDescRubbish[1].trim();
                    result.put(attr, desc);
                }
                line = r.readLine();
            }
        }
        return result;
    }

    private static Map<ObjectName, Map<String, String>> connectMetrics() {
        try {
            ConnectMetricsRegistry metrics = new ConnectMetricsRegistry();
            return documentMetrics("kafka.connect", metrics.getAllTemplates());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<ObjectName, Map<String, String>> consumerMetrics() {
        try {
            Set<String> tags = new HashSet<>();
            tags.add("client-id");
            ConsumerMetrics metricsRegistry = new ConsumerMetrics(tags, "consumer");

            Method method = ConsumerMetrics.class.getDeclaredMethod("getAllTemplates");
            method.setAccessible(true);
            return documentMetrics("kafka.consumer",
                    (Iterable<MetricNameTemplate>) method.invoke(metricsRegistry));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<ObjectName, Map<String, String>> producerMetrics() {
        try {
            Map<String, String> metricTags = Collections.singletonMap("client-id", "client-id");
            MetricConfig metricConfig = new MetricConfig().tags(metricTags);
            Metrics metrics = new Metrics(metricConfig);

            ProducerMetrics metricsRegistry = new ProducerMetrics(metrics);
            Method method = ProducerMetrics.class.getDeclaredMethod("getAllTemplates");
            method.setAccessible(true);
            return documentMetrics("kafka.producer",
                    (Iterable<MetricNameTemplate>) method.invoke(metricsRegistry));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<ObjectName, Map<String, String>> documentMetrics(String domain,
                                       Iterable<MetricNameTemplate> allMetrics) throws Exception {
        Map<String, Map<String, String>> beansAndAttributes = new TreeMap<>();

        try (Metrics metrics = new Metrics()) {
            for (MetricNameTemplate template : allMetrics) {
                Map<String, String> tags = new LinkedHashMap<>();
                for (String s : template.tags()) {
                    tags.put(s, quoteParameter(s));
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

        TreeMap<ObjectName, Map<String, String>> objectNames = new TreeMap<>(new Comparator<ObjectName>() {
            @Override
            public int compare(ObjectName o1, ObjectName o2) {
                // Horribly inefficient
                return prettyPrint(o1).compareTo(prettyPrint(o2));
            }
        });
        for (Map.Entry<String, Map<String, String>> e : beansAndAttributes.entrySet()) {
            objectNames.put(objectName(parameterNameToWildcard(e.getKey())), e.getValue());
        }

        return objectNames;
    }

    private static void formatTable(Appendable out,
                                    ObjectName name,
                                    Map<String, String> objectNames) throws IOException {
        out.append("//").append(prettyPrint(name)).append("\n");
        out.append("[options=\"header\"]\n");
        out.append("|=======\n");

        int pad = calcPad(objectNames.keySet());
        out.append("| Attribute");
        pad(out, pad - "Attribute".length());
        out.append(" | Description\n");

        for (Map.Entry<String, String> attrAndDescription : objectNames.entrySet()) {
            String attributeName = attrAndDescription.getKey();
            out.append("| ").append(attributeName);
            pad(out, pad - attributeName.length());
            String description = attrAndDescription.getValue().trim();
            if (!description.matches(".*[.?!]$")) {
                description = description + ".";
            }
            out.append(" | ").append(description).append("\n");
        }

        out.append("|=======\n\n");
    }

    private static String parameterNameToWildcard(String name) {
        return name.replaceAll("%%([^%]+)%%", "*");
    }

    private static String quoteParameter(String s) {
        return "%%" + s + "%%";
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

    private static String prettyPrint(ObjectName name) {
        StringBuilder sb = new StringBuilder(name.getDomain()).append(":");
        TreeSet<String> literal = new TreeSet<String>();
        TreeSet<String> wildcards = new TreeSet<String>();
        for (String prop : name.getKeyPropertyList().keySet()) {
            if (name.isPropertyValuePattern(prop)) {
                wildcards.add(prop);
            } else {
                literal.add(prop);
            }
        }
        boolean f = false;
        for (String prop : literal) {
            if (f) {
                sb.append(",");
            } else {
                f = true;
            }
            sb.append(prop).append("=").append(name.getKeyProperty(prop));
        }
        for (String prop : wildcards) {
            if (f) {
                sb.append(",");
            } else {
                f = true;
            }
            sb.append(prop).append("=").append(name.getKeyProperty(prop));
        }
        return sb.toString();
    }
}
