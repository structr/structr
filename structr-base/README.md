## About structr-core

The Java library structr-core is the core of the structr framework.
It encapsulates an embedded Neo4j database and provides means
to access and manipulate data in it.

It can be used in a stand-alone scenario like this:

```
[ Application  |                  ]
[ structr-core | java-API |       ]
[ neo4j        | java-API         ]
```

### Build & Run

Build with the usual maven commands (e.g. mvn clean install).

You cannot run structr-core stand-alone (it's a library).

### Documentation

Currently, there is no dedicated documentation available. Some documentation
can be found in the [dev-guide](http://docs.structr.org/dev-guide).

Additionally, you can take a look at the standard javadoc documentation here:

structr-core/target/apidocs/index.html

### Functionality

- Service infrastructure (using the [command pattern](http://en.wikipedia.org/wiki/Command_pattern))
- Entity context to define domain model
- Automatic relationship handling with regard to cardinality
- Access control
- Search
- Validation
- Complex constraints
- Error handling
- Cascading delete
- Spatial functions (using [neo4-spatial](https://github.com/neo4j/spatial))
- Event cascades
- Cron jobs
- Asynchronous agents
- Administration/migration tools
- Counters

### Code Repository

[structr-core on Github] (https://github.com/structr/structr/tree/master/structr/structr-core)
