/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package com.github.rhmessaging.amqstreamsdoc;

import com.github.rhmessaging.amqstreamsdoc.model.BooleanParameter;
import com.github.rhmessaging.amqstreamsdoc.model.ClassParameter;
import com.github.rhmessaging.amqstreamsdoc.model.DoubleParameter;
import com.github.rhmessaging.amqstreamsdoc.model.IntParameter;
import com.github.rhmessaging.amqstreamsdoc.model.Parameter;
import com.github.rhmessaging.amqstreamsdoc.model.StringParameter;
import kafka.Kafka;
import kafka.log.LogConfig$;
import kafka.server.DynamicBrokerConfig$;
import kafka.server.KafkaConfig$;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.apache.kafka.streams.StreamsConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            generateTable(def.get(), "broker".equals(arg) ? brokerDynamicUpdates() : null);
        }
    }

    static Map<String, String> brokerDynamicUpdates() {
        return DynamicBrokerConfig$.MODULE$.dynamicConfigUpdateModes();
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
            //parameter(key);

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

    private static String kafkaVersion() {
        Properties p = new Properties();
        try (InputStream resourceAsStream = Kafka.class.getClassLoader().getResourceAsStream("kafka/kafka-version.properties")) {
            p.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't find Kafka version from kafka/kafka-version.properties classpath resource in clients jar", e);
        }
        return p.getProperty("version");
    }

    private static String docsUrlForVersion() {
        String kafkaVersion = kafkaVersion();
        String docsVersion = kafkaVersion.replaceAll("([0-9]+)\\.([0-9]+).*", "\1\2");
        return "https://kafka.apache.org/" + docsVersion + "/documentation.html";
    }

    private static Parameter parameter(ConfigDef.ConfigKey key) {
        if (key.validator != null) {
            if (key.validator instanceof ConfigDef.NonNullValidator) {

            } else if (key.validator instanceof ConfigDef.Range) {
                String s = key.validator.toString();
                Pattern p = Pattern.compile("\\[([^,]*,)?...(,[^,]*)?\\]");
                Matcher matcher = p.matcher(s);
                if (matcher.matches()) {
                    matcher.group(1);
                    matcher.group(2);
                }
            } else if (key.validator instanceof ConfigDef.ValidList) {

            } else if (key.validator instanceof ConfigDef.ValidString) {

            } else {
                throw new RuntimeException("Unsupported validator " + key.validator.getClass());
            }
        }

        switch (key.type) {
            case INT:
            case LONG:
            case SHORT:
                return new IntParameter(key.name, key.type.name(), key.documentation, String.valueOf(key.defaultValue), null, null, null);
            case STRING:
            case PASSWORD:// TODO??
                return new StringParameter(key.name, key.type.name(), key.documentation, String.valueOf(key.defaultValue), null, null);
            case BOOLEAN:
                return new BooleanParameter(key.name, key.type.name(), key.documentation, String.valueOf(key.defaultValue), null);
            case DOUBLE:
                return new DoubleParameter(key.name, key.type.name(), key.documentation, String.valueOf(key.defaultValue), null, null, null);
            case LIST:
                return null;// TODO
            case CLASS:
                return new ClassParameter(key.name, key.type.name(), key.documentation, String.valueOf(key.defaultValue), null);
            default:
                throw new RuntimeException("Unsupported type " + key.type);

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
            String url = m.group(1);
            if (url.startsWith("#")) {
                // fragment, so resolve it against the relevant version
                url = "https://kafka.apache.org/23/documentation.html" + url;
            } else if (url.startsWith("/")) {
                // absolute path
                url = "https://kafka.apache.org" + url;
            }
            sb.append(url);
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

    static ConfigDef brokerConfigs() {
        try {
            Field instance = KafkaConfig$.class.getDeclaredField("MODULE$");
            instance.setAccessible(true);
            KafkaConfig$ x = (KafkaConfig$) instance.get(null);
            Field config = KafkaConfig$.class.getDeclaredField("configDef");
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
