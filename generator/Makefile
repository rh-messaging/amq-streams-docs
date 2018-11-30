TOOL=target/amq-streams-docs-0.1.0-SNAPSHOT.jar
CONFIG_DOCS=$(wildcard ref-*-config.adoc)
METRICS_DOCS=$(wildcard ref-mbeans-*-gen.adoc)
JAVA_FILES:=$(shell find src/main/java/ -type f -name '*.java')

docs: configs metrics

clean:
	mvn clean
	rm $(CONFIG_DOCS)
	rm $(METRICS_DOCS)

$(TOOL): pom.xml $(JAVA_FILES)
	mvn install

ref-broker-config.adoc: configs.sh $(TOOL)
	./configs.sh "Broker configuration parameters" broker > ref-broker-config.adoc

ref-consumer-config.adoc: configs.sh $(TOOL)
	./configs.sh "Consumer configuration parameters" consumer > ref-consumer-config.adoc

ref-producer-config.adoc: configs.sh $(TOOL)
	./configs.sh "Producer configuration parameters" producer > ref-producer-config.adoc

ref-admin-client-config.adoc: configs.sh $(TOOL)
	./configs.sh "Admin Client configuration parameters" adminclient > ref-admin-client-config.adoc

ref-connect-config.adoc: configs.sh $(TOOL)
	./configs.sh "Kafka Connect configuration parameters" connect > ref-connect-config.adoc

ref-streams-config.adoc: configs.sh $(TOOL)
	./configs.sh "Kafka Streams configuration parameters" streams > ref-streams-config.adoc

ref-topic-config.adoc: configs.sh $(TOOL)
	./configs.sh "Topic configuration parameters" topic > ref-topic-config.adoc

configs: $(CONFIG_DOCS)

ref-mbeans-producer-gen.adoc: metrics.sh $(TOOL)
	./metrics.sh producer ref-mbeans-producer-gen.adoc

ref-mbeans-consumer-gen.adoc: metrics.sh $(TOOL)
	./metrics.sh consumer ref-mbeans-consumer-gen.adoc

ref-mbeans-kafka-connect-gen.adoc: metrics.sh $(TOOL)
	./metrics.sh connect ref-mbeans-kafka-connect-gen.adoc

ref-mbeans-kafka-streams-gen.adoc: metrics.sh $(TOOL)
	./metrics.sh streams ref-mbeans-kafka-streams-gen.adoc

metrics: $(METRICS_DOCS)

.PHONY: clean configs metrics docs