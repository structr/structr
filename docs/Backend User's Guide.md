# Backend User's Guide
This document contains a step-by-step guide to the structr backend. We will start with some information about the concepts and technologies and iteratively build a fully functional backend for a fictional use case.

You should already know what a REST server is, and why you want to use such a server on top of a graph database, which has its own advantages and disadvantages compared to a relational database. You should also be familiar with Java, Apache Maven and git, as structr is hosted on github.

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
The Server class is the main class of every structr project. It is responsible for both configuration and startup of the REST backend. The following code fragment shows the how a typical Server.java looks like when created from the base archetype without additional configuration.

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

We use the builder pattern here to allow easy configuration, as you can simply add more configuration elements beween the `createServer()` and the `start()` lines. Let's say we want to modify the IP address and port the server binds to, and the REST base path, so we modify the server class to look like this:

            Structr.createServer(Server.class, "example-backend 0.1", 1234)

                .host("127.0.0.1")
                .restUrl("/backend")
            
                .start(true);
 














