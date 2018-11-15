
clean:
	mvn clean

jar:
	mvn install

docs: jar
	./configs.sh "Broker configuration parameters" broker > ../books/ref-broker-config.adoc
	./configs.sh "Consumer configuration parameters" consumer > ../books/ref-consumer-config.adoc
	./configs.sh "Producer configuration parameters" producer > ../books/ref-producer-config.adoc
	./configs.sh "Admin Client configuration parameters" adminclient > ../books/ref-admin-client-config.adoc
	./configs.sh "Kafka Connect configuration parameters" connect > ../books/ref-connect-config.adoc
	./configs.sh "Kafka Streams configuration parameters" streams > ../books/ref-streams-config.adoc
	./configs.sh "Topic configuration parameters" topic > ../books/ref-topic-config.adoc

	./metrics.sh 'MBeans in the `kafka.producer` domain' producer > ../books/ref-mbeans-producer-gen.adoc
	./metrics.sh 'MBeans in the `kafka.consumer` domain' consumer > ../books/ref-mbeans-consumer-gen.adoc
	./metrics.sh 'MBeans in the `kafka.connect` domain' connect > ../books/ref-mbeans-kafka-connect-gen.adoc