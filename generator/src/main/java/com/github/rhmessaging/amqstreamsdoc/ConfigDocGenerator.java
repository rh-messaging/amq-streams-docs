/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc;

import kafka.log.LogConfig$;
import kafka.server.DynamicBrokerConfig;
import kafka.server.DynamicBrokerConfig$;
import kafka.server.KafkaConfig$;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.apache.kafka.streams.StreamsConfig;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;

public class ConfigDocGenerator {

    private static final Pattern SENTENCE_TERMINATION = Pattern.compile("^.*[.!?]\\s*$", Pattern.MULTILINE|Pattern.DOTALL);

    public static void main(String[] args) throws Exception {
        HashMap<String, Supplier<ConfigDef>> c = new HashMap<>();
        c.put("consumer", ConfigDocGenerator::consumerConfigs);
        c.put("producer", ConfigDocGenerator::producerConfigs);
        c.put("adminclient", ConfigDocGenerator::adminClientConfigs);
        c.put("streams", ConfigDocGenerator::streamsConfigs);
        c.put("connect", ConfigDocGenerator::connectConfigs);
        c.put("broker", ConfigDocGenerator::brokerConfigs);
        c.put("topic", ConfigDocGenerator::topicConfigs);

        if (args.length == 0) {
            throw new IllegalArgumentException("Specify a config to generate docs for, one of " + c.keySet());
        }
        for (String arg : args) {
            Supplier<ConfigDef> def = c.get(arg);
            if (def == null) {
                throw new IllegalArgumentException("Unknown config " + arg);
            }

            generateTable(def.get(), "broker".equals(arg) ? DynamicBrokerConfig$.MODULE$.dynamicConfigUpdateModes() : null);
        }
    }

    private static void generateTable(ConfigDef def, Map<String, String> dynamicUpdates) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {

        Appendable out = System.out;
        Method headersMethod = def.getClass().getDeclaredMethod("headers");
        headersMethod.setAccessible(true);
        List<String> headers = new ArrayList<>((List<String>) headersMethod.invoke(def));
        headers.remove("Name");
        headers.remove("Description");
        headers.add("Description");
        headers.add(0, "Name");

        Method getConfigValueMethod = def.getClass().getDeclaredMethod("getConfigValue", ConfigDef.ConfigKey.class, String.class);
        getConfigValueMethod.setAccessible(true);

        Method sortedConfigs = ConfigDef.class.getDeclaredMethod("sortedConfigs");
        sortedConfigs.setAccessible(true);

        List<ConfigDef.ConfigKey> keys = (List) sortedConfigs.invoke(def);
        for (ConfigDef.ConfigKey key : keys) {
            if (key.internalConfig) {
                continue;
            }
            for (String header : headers) {
                String cellContent = String.valueOf(getConfigValueMethod.invoke(def, key, header));

                // In Kafka 2.0 NonNullValidator didn't override toString()
                if ("Valid Values".equals(header) &&
                        cellContent.matches("org\\.apache\\.kafka\\.common\\.config\\.ConfigDef\\$NonNullValidator@[0-9a-f]+")) {
                    cellContent = "non-null value";
                }


                if ("Name".equals(header)) {
                    cellContent = '`' + cellContent + "`::";
                } else if ("Description".equals(header)) {
                    if (dynamicUpdates != null) {
                        String dynamicUpdate = dynamicUpdates.getOrDefault(key.name, "read-only");
                        out.append("*Dynamic update:* ").append(dynamicUpdate).append(" +\n");
                    }

                    cellContent = convertToAsciidoc(cellContent.trim());
                    if (!SENTENCE_TERMINATION.matcher(cellContent).matches()) {
                        cellContent = cellContent + ".";
                    }
                    out.append("+\n");
                } else {
                    if (cellContent.isEmpty()) {
                        continue;
                    }
                    cellContent = "*" + header + ":* " + cellContent + " +";
                }
                out.append(cellContent).append('\n');
                // TODO Dynamic broker configs
            }
            out.append('\n');
        }
    }

    private static String convertToAsciidoc(String html) {
        String adoc = html.replaceAll("</?code>", "`").replace("¦", "\\¦");
        adoc = adoc.replaceAll("(</p>\\s*)?<p>", "\n+\n")
            .replace("<ul>", "\n")
            .replace("<li>", "\n* ")
            .replace("</li>", "")
            .replace("</ul>", "")
            .replaceAll("</?p>", "\n")
            .replaceAll("<br/?>", "\n");
        Pattern aOpen = Pattern.compile("<a\\s+.*?href=[\\'\"]?([^\\s>'\"]+).*?>");
        Pattern aClose = Pattern.compile("(.*?)<\\s*/a\\s*>");
        Matcher m = aOpen.matcher(adoc);
        StringBuffer sb = new StringBuffer();
        int ii = 0;
        while (m.find()) {
            sb.append(adoc, ii, m.start());
            sb.append(m.group(1));
            Matcher m2 = aClose.matcher(adoc);
            if (m2.find(m.end())) {
                sb.append('[');
                sb.append(adoc, m.end(), m2.end(1));
                sb.append(']');
                ii = m2.end();
            } else {
                throw new RuntimeException();
            }
        }
        sb.append(adoc, ii, adoc.length());
        adoc = sb.toString();
        Pattern p3 = Pattern.compile("</?[a-zA-Z0-9 '=]*>");
        Matcher m2 = p3.matcher(adoc);
        while (m2.find()) {
            System.err.println("Warning: possible unconverted HTML: "+ m2.group());
        }
        return adoc;
    }


    private static ConfigDef consumerConfigs() {
        try {
            Field config = ConsumerConfig.class.getDeclaredField("CONFIG");
            config.setAccessible(true);
            return (ConfigDef) config.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigDef producerConfigs() {
        try{
            Field config = ProducerConfig.class.getDeclaredField("CONFIG");
            config.setAccessible(true);
            return (ConfigDef) config.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigDef adminClientConfigs() {
        try {
            Field config = AdminClientConfig.class.getDeclaredField("CONFIG");
            config.setAccessible(true);
            return (ConfigDef) config.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigDef streamsConfigs() {
        try {
            Field config = StreamsConfig.class.getDeclaredField("CONFIG");
            config.setAccessible(true);
            return (ConfigDef) config.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigDef connectConfigs() {
        try {
            Field config = DistributedConfig.class.getDeclaredField("CONFIG");
            config.setAccessible(true);
            return (ConfigDef) config.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigDef brokerConfigs() {
        try {
            Field instance = KafkaConfig$.class.getDeclaredField("MODULE$");
            instance.setAccessible(true);
            KafkaConfig$ x = (KafkaConfig$) instance.get(null);
            Field config = KafkaConfig$.class.getDeclaredField("kafka$server$KafkaConfig$$configDef");
            config.setAccessible(true);
            return (ConfigDef) config.get(x);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigDef topicConfigs() {
        try {
            Field instance = LogConfig$.class.getDeclaredField("MODULE$");
            instance.setAccessible(true);
            LogConfig$ x = (LogConfig$) instance.get(null);
            Field config = LogConfig$.class.getDeclaredField("kafka$log$LogConfig$$configDef");
            config.setAccessible(true);
            return (ConfigDef) config.get(x);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
