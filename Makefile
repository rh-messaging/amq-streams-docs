BUILD_DIR := build
CONFIG_DOCS=ref-broker-config.adoc ref-consumer-config.adoc ref-producer-config.adoc ref-admin-client-config.adoc ref-connect-config.adoc ref-streams-config.adoc ref-topic-config.adoc
METRICS_DOCS=ref-mbeans-producer-gen.adoc ref-mbeans-consumer-gen.adoc ref-mbeans-kafka-connect-gen.adoc ref-mbeans-kafka-streams-gen.adoc

include ./Makefile.os
-include ./Makefile.publish

clean: 
	rm -r html || true
	$(MAKE) -C generator $(MAKECMDGOALS)

check:
	./.travis/check_docs.sh books

$(CONFIG_DOCS):
	$(MAKE) -C generator $@
	cp generator/$@ books/$@

$(METRICS_DOCS):
	$(MAKE) -C generator $@
	cp generator/$@ books/$@

generated: $(METRICS_DOCS) $(CONFIG_DOCS)

html/index.html: books/*.adoc
# Convert the asciidoc to html
	mkdir -p html
	$(CP) -vrL books/images html/images
	asciidoctor -v --failure-level WARN -t -dbook -a ProductVersion=$(RELEASE_VERSION) -a GithubVersion=$(GITHUB_VERSION) books/master.adoc -o html/index.html

build: html/index.html

buildall: generated build

publish: build
	rsync -av html/ $(PUBLISH_DEST)

.PHONY: clean check generated $(CONFIG_DOCS) $(METRICS_DOCS) build buildall publish