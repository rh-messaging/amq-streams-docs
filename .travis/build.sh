#!/bin/sh

SOURCES=books
RELEASE_VERSION=1.0

./.travis/check_docs.sh "$SOURCES"
CHECK=$?

mkdir -p out/html
cp -vrL "$SOURCES/images" out/html/images
asciidoctor -v --failure-level WARN -t -dbook -a ProductVersion=${RELEASE_VERSION} "$SOURCES/master.adoc" -o out/html/index.html
GEN=$?

if [ "$CHECK" != "0" -o "$GEN" != "0" ]; then
  echo "Documentation errors: check output above"
  exit 1
fi

