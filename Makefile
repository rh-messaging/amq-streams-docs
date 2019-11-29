BUILD_DIR := build
GUIDES := rhel-using
RHEL_GUIDES := $(filter rhel-%,$(GUIDES))
OCP_GUIDES := $(filter ocp-%,$(GUIDES))

include ./Makefile.os
-include ./Makefile.publish

build: $(GUIDES)

buildall: cpgenerated build

.PHONY: clean
clean: 
	rm -r html documentation/config documentation/mbean || true
	$(MAKE) -C generator $(MAKECMDGOALS)

.PHONY: check
check:
	./.travis/check_docs.sh documentation

.PHONY: mkgenerated
mkgenerated: 
	$(MAKE) -C generator docs

.PHONY: cpgenerated
cpgenerated: mkgenerated
	
	mkdir -p documentation/config && cp -p generator/*config.adoc documentation/config
	mkdir -p documentation/mbean && cp -p generator/ref-mbeans-*.adoc documentation/mbean
	ln -sfr -t documentation/rhel/modules/ documentation/config/* documentation/mbean/*
	ln -sfr -t documentation/ocp/modules/ documentation/config/* documentation/mbean/*

html/rhel/using/index.html: $(shell find -L documentation/rhel/using/ -type f)
	mkdir -p html/rhel/using/
	$(CP) -vrL documentation/rhel/using/images html/rhel/using/images
	asciidoctor -v --failure-level WARN -t -dbook -a ProductVersion=$(RELEASE_VERSION) -a GithubVersion=$(GITHUB_VERSION) ./documentation/rhel/using/master.adoc -o html/rhel/using/index.html

.PHONY: rhel-using
rhel-using: html/rhel/using/index.html

.PHONY: rhel
rhel: $(RHEL_GUIDES)

.PHONY: ocp
ocp: $(OCP_GUIDES)

.PHONY: publish
publish: build
	rsync -av html/ $(PUBLISH_DEST)

.PHONY: $(GUIDES)