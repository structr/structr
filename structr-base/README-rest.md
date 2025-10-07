## About structr-rest

The module structr-rest exposes the functionality of structr-core and Neo4j as
a REST web service.

With structr-rest you can easily build your own JSON/REST service backend based
on your specific domain objects (nodes/relationships/hyper-relationships).

It can be used in a stand-alone scenario like this:

```
[ Application  |                  ]
[ structr-rest | REST-API         ]
[ structr-core | java-API |       ]
[ neo4j        | java-API         ]
```

### Build & Run

Build with the usual maven commands (e.g. mvn clean install), with the options:
-DskipTests

You can run structr-core stand-alone, although at this point this functionality
is not included in the maven POM. As a reference, see how structr-ui is started. 

### Documentation

[rest-user-guide](http://docs.structr.org/rest-user-guide)

### Functionality

- Mapping of REST resources to domain model
- Serialization/deserialization between entities and JSON objects using GSON
- Sorting
- Paging
- Result count
- Timing information (query_time, serialization_time)
- Modular authentication
- Authorization (allow/deny access to specific resources)

## Example - REST Look&Feel

### A simple "city" entity in Java..

	public class City extends AbstractNode {

		// define a 1:n relationship between "City" and "Person"
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

## Repository

[structr-rest on Github] (https://github.com/structr/structr/tree/master/structr/structr-rest)
