
clean:
	mvn clean

jar:
	mvn install

docs: jar
	./header.sh "Broker configuration parameters" broker > ../books/ref-broker-config.adoc
	./header.sh "Consumer configuration parameters" consumer > ../books/ref-consumer-config.adoc
	./header.sh "Producer configuration parameters" producer > ../books/ref-producer-config.adoc
	./header.sh "Admin Client configuration parameters" adminclient > ../books/ref-admin-client-config.adoc
	./header.sh "Kafka Connect configuration parameters" connect > ../books/ref-connect-config.adoc
	./header.sh "Kafka Streams configuration parameters" streams > ../books/ref-streams-config.adoc
	./header.sh "Topic configuration parameters" topic > ../books/ref-topic-config.adoc