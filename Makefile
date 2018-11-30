BUILD_DIR := build

include ./Makefile.os
-include ./Makefile.publish

build: html/index.html

buildall: generated build

clean: 
	rm -r html || true
	$(MAKE) -C generator $(MAKECMDGOALS)

check:
	./.travis/check_docs.sh books

generated: 
	$(MAKE) -C generator docs
	cp -p generator/*.adoc books/

html/index.html: books/*.adoc
# Convert the asciidoc to html
	mkdir -p html
	$(CP) -vrL books/images html/images
	asciidoctor -v --failure-level WARN -t -dbook -a ProductVersion=$(RELEASE_VERSION) -a GithubVersion=$(GITHUB_VERSION) books/master.adoc -o html/index.html



publish: build
	rsync -av html/ $(PUBLISH_DEST)

.PHONY: clean check generated build buildall publish