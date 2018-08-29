BUILD_DIR := build

CCUTIL_ENABLED := \
	$(shell which ccutil 1> /dev/null 2>&1 && echo yes || echo no)

BOOK_SOURCES := $(shell find . -maxdepth 2 -type f -name master.adoc)
//BOOK_SOURCES := $(filter-out ./broker-ocp/master.adoc,${BOOK_SOURCES})
BOOK_TARGETS := \
	${BOOK_SOURCES:%/master.adoc=${BUILD_DIR}/%/index.html} \
	${BOOK_SOURCES:%/master.adoc=${BUILD_DIR}/%/images}

IMAGE_SOURCES := $(shell find images -type f)
IMAGE_TARGETS := \
	${IMAGE_SOURCES:images/%=${BUILD_DIR}/images/%}

ifeq (${CCUTIL_ENABLED},yes)
CCUTIL_TARGETS := \
	${BOOK_SOURCES:%/master.adoc=${BUILD_DIR}/ccutil/%/index.html}
else
CCUTIL_TARGETS :=
endif

EXTRA_SOURCES := $(shell find internal -type f -name \*.adoc)
EXTRA_TARGETS := \
	${EXTRA_SOURCES:%.adoc=${BUILD_DIR}/%.html} \
	${BUILD_DIR}/index.html

PREVIEW_ATTRIBUTES := $(shell sed 's/^/-a /' .preview-attributes)

.PHONY: default
default: preview

.PHONY: help
help:
	@echo "[default]      Equivalent to 'make preview'"
	@echo "preview        Generate asciidoctor output (faster)"
	@echo "build          Generate ccutil and asciidoctor output (slower)"
	@echo "test           Run all build scripts and output checks"
	@echo "clean          Removes ${BUILD_DIR}/ and other build artifacts"
	@echo "publish        Copy output to http://file.rdu.redhat.com/~<user>/amq-docs/"
	@echo
	@echo "Preview and build render the output to ${BUILD_DIR}/"

.PHONY: preview
preview: ${BOOK_TARGETS} ${IMAGE_TARGETS} ${EXTRA_TARGETS}
	@echo "See the output in your browser at file:${PWD}/${BUILD_DIR}/welcome/index.html"

.PHONY: build
build: preview ${CCUTIL_TARGETS}

.PHONY: test
test: clean build test-ccs-build

.PHONY: clean
clean: ${BOOK_SOURCES:%/master.adoc=clean-%}
	rm -rf ${BUILD_DIR}

.PHONY: publish
publish: PUBLISH_DIR := amq-docs
publish: build
	rsync -av ${BUILD_DIR}/ file.rdu.redhat.com:public_html/${PUBLISH_DIR}

.PHONY: test-ccs-build
test-ccs-build:
	scripts/buildGuides.sh

define BOOK_TEMPLATE =
$${BUILD_DIR}/${1}/index.html: $$(shell find -L ${1} -type f -name \*.adoc) common/attributes.adoc
	@mkdir -p $${@D}
	asciidoctor ${PREVIEW_ATTRIBUTES} -o $$@ ${1}/master.adoc

$${BUILD_DIR}/${1}/images:
	@mkdir -p $${@D}
	ln -s ../images $$@

$${BUILD_DIR}/ccutil/${1}/index.html:
	@mkdir -p $${BUILD_DIR}/ccutil
	cd ${1} && ccutil compile --lang en_US --main-file master.adoc
	mv ${1}/build/tmp/en-US/html-single $${@D}
	rm -rf ${1}/build

.PHONY: clean-${1}
clean-${1}:
	rm -rf ${1}/build ${1}/html
endef

$(foreach dir,${BOOK_SOURCES:%/master.adoc=%},$(eval $(call BOOK_TEMPLATE,${dir})))

${BUILD_DIR}/internal/%.html: internal/%.adoc common/attributes.adoc
	@mkdir -p ${@D}
	asciidoctor -o $@ $<

${BUILD_DIR}/images/%.png: images/%.png
	@mkdir -p ${@D}
	cp $< $@

${BUILD_DIR}/docs:
	ln -s . $@

.PHONY: ${BUILD_DIR}/index.html
${BUILD_DIR}/index.html: scripts/redirect-to-welcome.html
	cp $< $@

.PHONY: refresh-upstream-%
refresh-upstream-%: BRANCH := master
refresh-upstream-%:
	rm -rf upstream/$* upstream/$*.revision
	scripts/git-export "https://github.com/strimzi/strimzi-kafka-operator.git" ${BRANCH} upstream/$*
