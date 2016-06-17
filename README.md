# The Structr Project

[![Build Status](http://ci.structr.org:59398/job/Structr/badge/icon)](http://ci.structr.org:59398/job/Structr)

Structr (pronounce it like 'structure') is a powerful application platform for **Enterprise Master Data Management**, **Product Data Management**, **Enterprise Content Management** and more.

Technically, it is a build and runtime environment for mobile and web applications based on the graph database Neo4j. It was designed to simplify the creation of complex graph database applications by providing a comprehensive set of features common to many use cases like **security**, **schema** enforcement, **JSON REST API** and integrated **search**, **CMS** and **DMS** functionality.

## Editions and Modules

Structr is free and open source software and dual licensed (GPLv3/AGPLv3). For an overview of the available editions and modules see [https://structr.com/editions](https://structr.com/editions).

## Download

Download the binary distributions package from [https://structr.org/download](https://structr.org/download).

## Documentation

The one-stop documentation and support site can be found at [https://support.structr.com/](https://support.structr.com/).

## Quick Start

Prerequisites are the Java JDK 1.8 (not JRE!), Maven 3.0.4+ and git.

Verify the Java/Maven version with `mvn -v`, then do:

```
git clone https://github.com/structr/structr.git

cd structr
mvn clean install -DskipTests
cd structr-ui

mvn validate exec:exec
```
Login with the credentials admin/admin at:

[http://localhost:8082/structr/](http://localhost:8082/structr/)

This help article describes how to build and run Structr from the source code: [https://support.structr.com/article/280](https://support.structr.com/article/280).

For a guide about the installation on Windows, see [https://support.structr.com/article/258](https://support.structr.com/article/258).

## Google Group / Mailing List

For non-technical questions about Structr, Structr's licensing, use-cases etc. please use the [Structr Google Group](https://groups.google.com/forum/#!forum/structr) or send an e-mail to [structr@googlegroups.com](structr@googlegroups.com).

## Report Bugs

You can submit issues (bugs, feature requests etc.) on the [issue-tracker](https://github.com/structr/structr/issues). Please don't ask questions or discuss general topics in the issue system.

## Contribute

For more information on how to contribute, please see [Contribute to Structr](http://docs.structr.org/contribute).

## Commercial Services

https://structr.com/services

## Structr Hosting

https://hosting.structr.com/

## Initial Authors

- Axel Morgner (axel.morgner@structr.com, @amorgner)
- Christian Morgner (christian.morgner@structr.com, @cmor_)

## Copyright and License

Copyright 2010-2016 Structr GmbH

Structr is dual licensed under the GPLv3 and AGPLv3. [Commercial editions](https://structr.com/editions) also available.
