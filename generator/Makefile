TOOL=target/amq-streams-docs-0.1.0-SNAPSHOT.jar
CONFIG_DOCS=ref-broker-config.adoc ref-consumer-config.adoc ref-producer-config.adoc ref-admin-client-config.adoc ref-connect-config.adoc ref-streams-config.adoc ref-topic-config.adoc
METRICS_DOCS=ref-mbeans-producer-gen.adoc ref-mbeans-consumer-gen.adoc ref-mbeans-kafka-connect-gen.adoc ref-mbeans-kafka-streams-gen.adoc

clean:
	mvn clean
	rm *.adoc

$(TOOL):
	mvn install

jar: $(TOOL)

ref-broker-config.adoc: jar
	./configs.sh "Broker configuration parameters" broker > ref-broker-config.adoc

ref-consumer-config.adoc: jar
	./configs.sh "Consumer configuration parameters" consumer > ref-consumer-config.adoc

ref-producer-config.adoc: jar
	./configs.sh "Producer configuration parameters" producer > ref-producer-config.adoc

ref-admin-client-config.adoc: jar
	./configs.sh "Admin Client configuration parameters" adminclient > ref-admin-client-config.adoc

ref-connect-config.adoc: jar
	./configs.sh "Kafka Connect configuration parameters" connect > ref-connect-config.adoc

ref-streams-config.adoc: jar
	./configs.sh "Kafka Streams configuration parameters" streams > ref-streams-config.adoc

ref-topic-config.adoc: jar
	./configs.sh "Topic configuration parameters" topic > ref-topic-config.adoc

configs: $(CONFIG_DOCS)

ref-mbeans-producer-gen.adoc: jar
	./metrics.sh producer ref-mbeans-producer-gen.adoc

ref-mbeans-consumer-gen.adoc: jar
	./metrics.sh consumer ref-mbeans-consumer-gen.adoc

ref-mbeans-kafka-connect-gen.adoc: jar
	./metrics.sh connect ref-mbeans-kafka-connect-gen.adoc

ref-mbeans-kafka-streams-gen.adoc: jar
	./metrics.sh streams ref-mbeans-kafka-streams-gen.adoc

metrics: $(METRICS_DOCS)

docs: configs metrics

.PHONY: clean jar configs docs