# TaintAnalysisProject

## Introduction

This application enables the automatic modification of a Java application by tracing the data that travels within it, using the Taint Analysis technique, so that data from trusted or untrusted sources can be identified in the execution flow. Once references are obtained, checks are made in the wells to ensure that data labeled as untrusted is sanitized.

## Technologies Used

- **Maven**: It is a Java-based software project management and build automation tool.
- **IntelliJ IDEA**: It is an integrated development environment (IDE) for the Java programming language.
- **JavaParser**: It is a library that provides an easy way to parse, analyze, and manipulate Java source code.
- **Java 16**: Programming language used for creating and structuring the various logics of the application. 
- **JavaDoc**: It is used for automatic generation of documentation of source code written in Java language.
- **AspectJ**: It represents the primary tool, in Java, for taking advantage of aspect-oriented programming.

## Build and Execution

Run the following commands from the project root:
```bash
mvn clean compile install exec:java
```

## Test

There is already a Java file in the project to test the operation of the application, placed inside the /data/source path. Just start the application and then go to the /data/destination path to observe the result.

## Javadoc
To view the Javadoc documentation for the project follow these steps:
- mvn install
- Open the index.html file in the following path: {projectPath}/target/apidocs

## Author
Marco Raciti (GitHub reference: www.github.com/Mark1096)
