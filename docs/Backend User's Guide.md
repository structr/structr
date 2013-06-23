# Backend User's Guide
This document contains a step-by-step guide to the structr backend. We will start with some information about the concepts and technologies and iteratively build a fully functional backend for a fictional use case.

You should already know what a REST server is, and why you want to use such a server on top of a graph database, which has its own advantages and disadvantages compared to a relational database. You should also be familiar with Java, Apache Maven and git, as structr is hosted on github and we rely on Maven to manage dependencies and builds etc.

### About structr
The structr REST server essentially is a graph-based JSON document store, where documents are automatically transformed into graph structures and back, according to a pre-defined schema. This schema consists of node and relationship entities, property definitions and configurable views which will be described in detail in the following chapters.

### The Neo4j property graph
Data in a Neo4j database is stored in what is called a property graph. This graph consists of nodes and relationships, which both can have an arbitrary number of primitive properties, including arrays. Properties are stored and retrieved using a String key, so you can think of such a property container as a kind of persistent map. Nodes are the basic building blocks of a property graph and can be connected to other nodes using relationships.

### structr’s data model
structr extends the Neo4j property graph, adding type safety, validation, automatic indexing and transparent relationship creation. When you access structr entities using Java code, you can use `setProperty()` and `getProperty()` to work with collections of nodes and structr will automatically create relationships for the defined relation.

## The first steps
### Creating a new project
structr has several Maven archetypes available for you to start you project with. As we want to create a backend project from scratch, we will use the archetype named `structr-base-archetype`. So the first step would be to let Maven create a new project from the base archetype:

    mvn archetype:generate \
        -DarchetypeRepository=http://maven.structr.org/artifactory/snapshot \
        -DarchetypeGroupId=org.structr \
        -DarchetypeArtifactId=structr-base-archetype \
        -DarchetypeVersion=0.8-SNAPSHOT \
        -DgroupId=org.structr \
        -DartifactId=example-backend \
        -Dversion=0.1 \
        -Dpackage=org.structr.example

When you execute this command, Maven will ask you to confirm the choices you made and will create a project named `example-backend` in the current directory. We assume you are already familiar with the layout of a Maven project, so let's jump directly into the code.

### Server.java
The Server class is the main class of every structr project. It is responsible for both configuration and startup of the REST backend. The following code fragment shows the how a typical Server.java looks like when it is first created from the base archetype.

```java
public class Server implements StructrServer {

    public static void main(String[] args) {

            try {

                    Structr.createServer(Server.class, "example-backend 0.1")

                            .start(true);


            } catch(Exception ex) {

                    ex.printStackTrace();
            }
    }
}
```

We use the builder pattern here to allow easy configuration, as you can simply add more configuration elements beween the `createServer()` and the `start()` lines. Let's say we want to modify the IP address and port the server binds to, and the REST base path, so we modify the code to look like this:

```java
Structr.createServer(Server.class, "example-backend 0.1")

        .host("127.0.0.1")
        .port("8080)
        .restUrl("/api")

        .start(true);
```

#### Adding custom configuration elements
The server class provides an easy method to add your own, custom configuration elements that you can later access from within your application code. Just use the `addCustomConfig()` method to bring your custom configuration element into the structr configuration.

```java
Structr.createServer(Server.class, "example-backend 0.1")

        .host("127.0.0.1")
        .port("8080)
        .restUrl("/api")

        .addCustomConfig("my.configuration.option = myConfigurationValue")

        .start(true);
```

and retrieve the value later using one of the two lines below.

```java
	String value          = Services.getConfigurationValue("my.configuration.value");
	String valueOrDefault = Services.getConfigurationValue("my.configuration.value", "myDefaultConfigurationValue");
```

#### The configuration file (structr.conf)
When structr is started for the first time, it creates a new configuration file from the values that are encoded in the server class. (*this behaviour needs to be discussed further as it might lead to confusion*)

#### Caution
Please note that the values in the structr.conf configuration file will override the values specified in the Server.java file, so be sure to remove the structr.conf file before you start structr after you added new configuration elements to Server.java

### Starting the server
#### Compiling
To start the server, you must of course first compile the project. Use Maven to run the `install` target, and do a `clean` before that when you compile repeatedly.

    mvn clean install
    
#### Using Maven to start the server
The structr base archetype includes a Maven exec:exec goal that you can use to start the server. Please note that this is not the preferred way of starting structr.

    mvn exec:exec
    
#### Starting the server from the command line (or a script)
The preferred way of starting a structr REST server is by using the following call, which can be used to start from a script (i.e. when running the server as a system service) or from the command line.

    java -Xms1g -Xmx1g -server -classpath target/lib/*:target/example-backend-0.1.jar org.structr.example.Server

You can of course modify the above JVM switches to better suit your hardware configuration and/or performance needs etc.

#### Debugging
To be able to attach a debugger to a running structr REST server, you can use the following JVM switches when starting the server.

    -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n
    
Replace the port 5005 in the example above by your preferred debugging port




# Things to include in this document
- @Export annotation for REST RPC






