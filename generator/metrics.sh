#!/usr/bin/env bash

ID=$1
CONFIG=$2


TABLE=$(java -cp "${HOME}/.m2/repository/com/github/rh-messaging/amq-streams-docs/0.1.0-SNAPSHOT/amq-streams-docs-0.1.0-SNAPSHOT.jar" \
    "com.github.rhmessaging.amqstreamsdoc.MetricsDocGenerator" \
    "$CONFIG")

cat <<EOF
// Module included in the following assemblies:
//
// assembly-monitoring.adoc

[id='$ID']

$TABLE
EOF