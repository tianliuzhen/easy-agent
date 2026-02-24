@echo off
cd ..
mvn org.mybatis.generator:mybatis-generator-maven-plugin:1.4.2:generate -f pom.xml
cd mbg
