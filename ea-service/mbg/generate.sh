#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
mvn org.mybatis.generator:mybatis-generator-maven-plugin:1.4.2:generate -f pom.xml -Dmybatis.generator.overwrite=true
