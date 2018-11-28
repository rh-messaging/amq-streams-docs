BUILD_DIR := build
CONFIG_DOCS=ref-broker-config.adoc ref-consumer-config.adoc ref-producer-config.adoc ref-admin-client-config.adoc ref-connect-config.adoc ref-streams-config.adoc ref-topic-config.adoc
METRICS_DOCS=ref-mbeans-producer-gen.adoc ref-mbeans-consumer-gen.adoc ref-mbeans-kafka-connect-gen.adoc ref-mbeans-kafka-streams-gen.adoc

include ./Makefile.os
-include ./Makefile.publish

clean: 
	rm -r html

check:
	./.travis/check_docs.sh books

$(CONFIG_DOCS):
	$(MAKE) -C generator $@
	cp generator/$@ books/$@

$(METRICS_DOCS): 
	$(MAKE) -C generator $@
	cp generator/$@ books/$@

books/master.html: $(CONFIG_DOCS) $(METRICS_DOCS)
# Convert the asciidoc to html
	mkdir -p html
	$(CP) -vrL books/images html/images
	asciidoctor -v --failure-level WARN -t -dbook -a ProductVersion=$(RELEASE_VERSION) -a GithubVersion=$(GITHUB_VERSION) books/master.adoc -o html/index.html

build: books/master.html

publish: build
	rsync -av html/ $(PUBLISH_DEST)


.PHONY: clean check generator build