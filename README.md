# The Structr Project

[![Build Status](https://secure.travis-ci.org/structr/structr.png)](http://travis-ci.org/structr/structr)

Structr (pronounce it like 'structure') is a Java framework for mobile and web applications based on the graph database Neo4j. It was designed to simplify the creation of complex graph database applications by providing a comprehensive Java API and a set of features common to most use cases. This enables developers to build a sophisticated web or mobile app based on Neo4j within hours.

### Main components
- OGM (Object-to-Graph Μapper) - [docu](http://docs.structr.org/dev-guide#Object-to-graph mapping)
- REST server - [docu](http://docs.structr.org/rest-user-guide)
- CMS frontend - [docu](http://docs.structr.org/frontend-user-guide)

### Awards
Structr was awarded with the Graphie Award for the Most Innovative Open Source Graph Application in 2012.

## Quick Start

Build and run Structr from the source code.

Prerequisites are the Java JDK 1.7+, Maven 3.0.4+ and git.

Verify the java/maven version with mvn -v, then do:

```
git clone https://github.com/structr/structr.git

cd structr
mvn clean install -DskipTests
cd structr-ui

mvn exec:exec
```

Login with the credentials admin/admin at:

http://localhost:8082/structr

For documentation, please take a look here:

http://docs.structr.org


## Getting Started
Using [Apache Maven](http://maven.apache.org/) archetypes, you get a demo project up and running in 5 minutes.

- See the [screencast](http://vimeo.com/53235075) for a short introduction to the maven archetype and a small feature demo.
- See the [structr-android-client](https://github.com/structr/structr-android-client) project on github for more information on how to use structr as a mobile backend.

## Example - REST Look&Feel

### A simple “city” entity in Java..

	public class City extends AbstractNode {

		// define a 1:n relationship between “City” and “Person”
		public static final Property<List<Person>> persons =

			new CollectionProperty<Person>(
				"persons",               // the name of the property
				Person.class,            // the end node type
				RelType.LIVES_IN,        // the relationship type
				Direction.INCOMING,      // the direction
				true                     // 1:n instead of n:m
			);
	
		// define the public view of a City to contain name and persons
		public static final View publicView = new View(Person.class, PropertyView.Public,
			name, persons
		);
	}


### .. its creation via REST using the demo server
	curl -si http://0.0.0.0:8082/structr/rest/cities -XPOST -d '{"name":"Berlin"}'

	HTTP/1.1 201 Created                                                                                                                                                                                                                                         
	Content-Type: application/json; charset=UTF-8                                                                                                                                                                                                                
	Location: http://0.0.0.0:8082/structr/rest/cities/2fe1180289db49f59827f9a88aefa707                                                                                                                                                                           
	Transfer-Encoding: chunked                                                                                                                                                                                                                                   
	Server: Jetty(8.1.0.RC5)                                                                                                                                                                                                                                     

### ..and its JSON output
	curl -si http://0.0.0.0:8082/structr/rest/cities

	HTTP/1.1 200 OK                                                                                                                                                                                                                                              
	Content-Type: application/json; charset=utf-8                                                                                                                                                                                                                
	Transfer-Encoding: chunked                                                                                                                                                                                                                                   
	Server: Jetty(8.1.0.RC5)                                                                                                                                                                                                                                                                                                                                               

	{                                                                                                                                                                                                                                                            
		"query_time": "0.071098661",                                                                                                                                                                                                                               
		"result_count": 1,                                                                                                                                                                                                                                         
		"result": [                                                                                                                                                                                                                                                
			{
				"id": "2fe1180289db49f59827f9a88aefa707",
				"name": "Berlin",
				"persons": []
			}
		],
		"serialization_time": "0.000401979"
	}

## Components

There are these two main components: Structr Server and Structr UI.

Structr Server provides libraries and modules building the backend.

Structr UI is a frontend for CRUD operations and basic CMS functionalitiy, e.g. for handling pages, files, images, users/groups.

## Learn more

- [structr.org](http://structr.org)
- [Neo4j](http://neo4j.org)
- [@structr](https://twitter.com/structr)

## Contribute

For submitting feature requests or bug report, please use Github's issue tracking system.

In order to contribute to structr, you must sign the Structr Contributor’s License Agreement, which can be found [here](http://structr.org/cla).

## Commercial Services

https://structr.org/services

## Hosting Services

https://structr.com/

## Authors

- Axel Morgner (axel@morgner.de, @amorgner)
- Christian Morgner (christian@morgner.de, @cmor_)

## Copyright and License

Copyright 2010-2014 Axel Morgner

Structr is licensed under the GPLv3 and AGPLv3 (structr UI).
