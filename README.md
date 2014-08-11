# The Structr Project

[![Build Status](https://secure.travis-ci.org/structr/structr.png)](http://travis-ci.org/structr/structr)

Structr (pronounce it like 'structure') is a Java framework for mobile and web applications based on the graph database Neo4j. It was designed to simplify the creation of complex graph database applications by providing a comprehensive Java API and a set of features common to most use cases. This enables developers to build a sophisticated web or mobile app based on Neo4j within hours.

### Main components
- Object-to-Graph mapper (OGM)
- REST server
- CMS frontend

For detailed feature lists of these components see

[OGM](Structr OGM.md)

[Backend Features](Backend.md)

[Frontend Features](Frontend.md)

### Awards
Structr was awarded with the Graphie Award for the Most Innovative Open Source Graph Application in 2012.

## Getting started
Structr uses [Apache Maven](http://maven.apache.org/), so you can use Maven to get a demo project up and running in 5 minutes with our simple example archetype.

- See the [screencast](http://vimeo.com/53235075) for a short introduction to the maven archetype and a small feature demo.
- See the [structr quick start guide](http://structr.org/quick-start-guide) for more information on the CMS module.
- See the [structr-android-client](https://github.com/structr/structr-android-client) project on github for more information on how to use structr as a mobile backend.

## Examples

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

For updates and news around Structr, follow @structr on Twitter.

## Contribute

For submitting feature requests or bug report, please use Github's issue tracking system.

In order to contribute to structr, you must sign the Structr Contributor’s License Agreement, which can be found [here](http://structr.org/cla).

## Authors

- Axel Morgner (axel@morgner.de, @amorgner)
- Christian Morgner (christian@morgner.de, @cmor_)

## Copyright and License

Copyright 2010-2013 Axel Morgner

Structr is licensed under the GPLv3 and AGPLv3 (structr UI).
