# The Structr Project

[![Build Status](https://secure.travis-ci.org/structr/structr.png)](http://travis-ci.org/structr/structr)

Structr (pronounce it like 'structure') is a Java framework for mobile and web applications based on the graph database Neo4j. It was designed to simplify the creation of complex graph database applications by providing a comprehensive Java API and a set of features common to most use cases. This enables developers to build a sophisticated web or mobile app based on Neo4j within hours.

Structr was awarded with the Graphie Award (by Neo4j maker Neo Technology) for the Most Innovative Open Source Graph Application in 2012.


## Quick Start

Build and run Structr from the source code.

Prerequisites are the Java JDK 1.7, Maven 3.0.4+ and git.

Verify the java/maven version with mvn -v, then do:

```
git clone https://github.com/structr/structr.git

cd structr
mvn clean install -DskipTests
cd structr-ui

mvn exec:exec
```

Login with the credentials admin/admin at:

[http://localhost:8082/structr/](http://localhost:8082/structr/)

For documentation, please take a look here:

http://docs.structr.org

## Getting Started

A very simple CMS demo app, hands-on tutorial: 

A Structr Demo Application in Less Than Ten Minutes: [https://structr.org/blog/structr-demo-in-ten-minutes](https://structr.org/blog/structr-demo-in-ten-minutes)

Using [Apache Maven](http://maven.apache.org/) archetypes, you get a demo project up and running in 5 minutes.

- See the [screencast](http://vimeo.com/53235075) for a short introduction to the maven archetype and a small feature demo.
- See the [structr-android-client](https://github.com/structr/structr-android-client) project on github for more information on how to use structr as a mobile backend.


## Components

To get an overview of the components, just browse the source code and review the READMEs.

Name             | Description                              | Documentation
---------------- | ---------------------------------------- | -----------------------------------------------------------------
**structr-core** | The Structr Server (Neo4j, OGM, ...)     | [docs](http://docs.structr.org/developer-guide#Object-to-graph mapping)
**structr-rest** | The REST server (add-on to structr-core) | [docs](http://docs.structr.org/REST-user-guide)
**structr-ui**   | The Structr UI (add-on to structr-rest)  | [docs](http://docs.structr.org/frontend-user-guide)

Structr UI is a browser based frontend for Visual Schema Design, CRUD operations and basic CMS functionalitiy, e.g. for handling pages, files, images, users/groups.

## Google Group / Mailing List

For non-technical questions about Structr, Structr's licensing, use-cases etc. please use the [Structr Google Group](https://groups.google.com/forum/#!forum/structr) or send an e-mail to [structr@googlegroups.com](structr@googlegroups.com).

## Report Bugs

You can submit issues (bugs, feature requests etc.) on the [issue-tracker](https://github.com/structr/structr/issues).

## Contribute

For more information on how to contribute, please see [Contribute to Structr](http://docs.structr.org/contribute).

## Commercial Services

https://structr.org/services

## Structr Hosting

https://structr.com

## Learn More

- [structr.org](http://structr.org)
- [Neo4j](http://neo4j.com)
- [@structr](https://twitter.com/structr)

## Authors

- Axel Morgner (axel@morgner.de, @amorgner)
- Christian Morgner (christian@morgner.de, @cmor_)

## Copyright and License

Copyright 2010-2014 Axel Morgner

Structr is licensed under the GPLv3 and AGPLv3 (structr UI).
