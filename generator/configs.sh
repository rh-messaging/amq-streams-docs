#!/usr/bin/env bash

TITLE=$1
CONFIG=$2

DOC_ID=$(echo $TITLE | tr 'ABCDEFGHIJKLMNOPQRSTUVWXYZ ' \
                          'abcdefghijklmnopqrstuvwxyz-')

TABLE=$(java -jar "${HOME}/.m2/repository/com/github/rh-messaging/amq-streams-docs/0.1.0-SNAPSHOT/amq-streams-docs-0.1.0-SNAPSHOT.jar" "$CONFIG")

cat <<EOF
// Module included in the following assemblies:
//
// assembly-overview.adoc
//
// THIS FILE IS AUTO-GENERATED. DO NOT EDIT BY HAND
// Run "make clean buildall" to regenerate.

[id='$DOC_ID-{context}']
= ${TITLE}

$TABLE
EOF