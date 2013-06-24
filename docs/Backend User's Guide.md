# Backend User's Guide
This document contains a step-by-step guide to the structr backend. We will start with some information about the concepts and technologies and incrementally build a fully functional backend for a fictitious use case.

You should already know what a REST server is, and why you want to use such a server on top of a graph database, which has its own advantages and disadvantages compared to a relational database. You should also be familiar with Java, Apache Maven and git, as structr is hosted on github and we rely on Maven to manage dependencies and builds etc.

### Table of contents
- [About structr](#about-structr)
- [The Neo4j property graph](#the-neo4j-property-graph)
- [The first steps](#the-first-steps)
    - [Creating a new project](#creating-a-new-project)
    - [Server.java](#serverjava)
    - [Adding custom configuration elements](#adding-custom-configuration-elements)
    - [The configuration file (structr.conf)](#the-configuration-file-structrconf)
    - [Caution](#caution)
- [Starting the server](#starting-the-server)
    - [Compiling](#compiling)

### About structr
The structr REST server essentially is a graph-based JSON document store, where documents are automatically transformed into graph structures and back, according to a pre-defined schema. This schema consists of node and relationship entities, property definitions and configurable views which will be described in detail in the following chapters.

### The Neo4j property graph
Data in a Neo4j database is stored in what is called a property graph. This graph consists of nodes and relationships, which both can have an arbitrary number of primitive properties, including arrays. Properties are stored and retrieved using a String key, so you can think of such a property container as a kind of persistent map. Nodes are the basic building blocks of a property graph and can be connected to other nodes using relationships.

## The first steps
### Creating a new project
structr has several Maven archetypes available for you to start your project with. As we want to create a backend project from scratch, we will use the archetype named `structr-base-archetype`. So the first step would be to let Maven create a new project from the base archetype:

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
        .port(8080)
        .restUrl("/api")

        .start(true);
```

#### Adding custom configuration elements
The server class provides an easy method to add your own, custom configuration elements that you can later access from within your application code. Just use the `addCustomConfig()` method to bring your custom configuration element into the structr configuration.

```java
Structr.createServer(Server.class, "example-backend 0.1")

        .host("127.0.0.1")
        .port(8080)
        .restUrl("/api")

        .addCustomConfig("my.configuration.option = myConfigurationValue")

        .start(true);
```

and retrieve the value later using one of the two lines below.

```java
String value          = Services.getConfigurationValue("my.configuration.value");
String valueOrDefault = Services.getConfigurationValue("my.configuration.value", "myDefault");
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

#### Tips
When you move the structr working directory to another directory, make sure you delete the structr.conf because there are absolute paths stored in this file. We are currently working on improving the configuration, so this will hopefully be fixed soon.

## Building the data model
structr extends the Neo4j property graph, adding type safety, validation, automatic indexing and transparent relationship creation. The basic building block of a structr REST application is the class `AbstractNode`. All node entities must inherit from this class in order to be available in structr. 

### Entities
For our previously mentioned fictitious use case, we want to model the anatomy of structr's source code, so we can later create visualizations and statistics on top of that. This brings us to the first few node entities and relationship types.

#### Node entities
- Author
- Interface
- Class

#### Relationship types
- WROTE

### Author.java
The Author entity is the first entity we examine, because it illustrates some of the most basic features of structr. As mentioned before, all node entities must inherit from `AbstractNode` to be available to the structr REST server. So the simplest case of a class looks like this.

```java
public class Author extends AbstractNode {}
```

#### Properties and Views
A node like this of course if of very little use, because it has no properties and no output representation. A structr entity can have many output representations, depending on the REST path it is accessed from, the position in the JSON output document or the relationship over which it is accessed. Output representations in structr are called *Views* and must be specified explicitly for each entity. A view is a collection of properties that belong together semantically in the context of the current view.

##### Pre-defined views
structr has a pre-defined set of views, including the default view which will be used if no view was specified explicitly in the REST path. The following views are defined in structr core and can be used freely in any project.

- **PropertyView.Public (this is the default view)**
- PropertyView.Protected
- PropertyView.Private
- PropertyView.All

**Note:** you can set the default property view in the configuration builder of the Server class.

##### Inheritance
Property views are inherited from the superclass.

#### Properties
In order to populate the default view of our newly created Author entity, we need to define the properties an Author can have. To do this, we specify one or more public members of type `Property` like in the following lines. See Appendix A for the full list of available property types.

```java
public static final Property<String>  name     = new StringProperty("name");
public static final Property<String>  email    = new StringProperty("email");
public static final Property<Integer> age      = new IntProperty("age");
public static final Property<Date>    birthday = new DateProperty("birthday", "dd.MM.yyyy");
public static final Property<Date>    cakeday  = new ISO8601DateProperty("cakeday");
```

The set of default properties that each structr node inherits from `AbstractNode` includes a `name` property, so we do not need to declare it by ourselves. This leads us to the first version of our Author entity.

```java
public class Author extends AbstractNode {

    public static final Property<String>  email    = new StringProperty("email");
    public static final Property<Date>    birthday = new DateProperty("birthday", "dd.MM.yyyy");
    public static final Property<Date>    cakeday  = new ISO8601DateProperty("cakeday");
    
    public static final View defaultView = new View(Author.class, PropertyView.Public,
        name, email, birthday, cakeday
    );
}
```

### REST access
Now that we finished the first entity, we can compile the project and start the server (see the previous chapter on how to do that). When the server is up and running, we can run the first REST queries. For each node type, structr automatically creates a nestable REST collection resource, so that we can manage our newly created Author entities.

#### GET
Using `curl`, we can now access the REST server for the first time. Of course the resource is initially empty, but the HTTP response code 200 tells us that the resource exists.

```
dev:~$ curl -i http://localhost:1234/api/authors
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Vary: Accept-Encoding, User-Agent
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
   "query_time": "0.001438173",
   "result_count": 0,
   "result": [],
   "serialization_time": "0.000117178"
}
```

#### POST
Now its time to create the first Author entity in the database. Using REST, this translates to a POST request to the collection resource `/authors`, so we execute the following curl call.

```
dev:~$ curl -i http://localhost:1234/api/authors -XPOST -d '
{
    "name": "Christian Morgner",
    "email": "christian@morgner.de",
    "birthday": "01.01.1970",
    "cakeday": "2013-06-04T17:16:00+0200"
}'
```

and the server will respond with something like this:

```
HTTP/1.1 201 Created
Content-Type: application/json; charset=UTF-8
Location: http://localhost:1234/api/authors/168eb522cfdb460f87616cccac46d9bb
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)
```

Issuing a GET request again, we can now see that the authors collection resource contains one element.

```
dev:~$ curl -i http://localhost:1234/api/authors
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Vary: Accept-Encoding, User-Agent
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
   "query_time": "0.004049202",
   "result_count": 1,
   "result": [
      {
         "name": "Christian Morgner",
         "email": "christian@morgner.de",
         "birthday": "01.01.1970",
         "cakeday": "2013-06-04T17:16:00+0200",
         "id": "168eb522cfdb460f87616cccac46d9bb",
         "type": "Author"
      }
   ],
   "serialization_time": "0.000771747"
}
```

As you can see, the JSON document contains all the properties we put into the default view earlier, including the `id`, `type` and `name` properties from the superclass.

#### PUT
Now we can of course modify the entity as well, using the PUT verb on the element resource that contains the newly created author.

```
dev:~$ curl -i http://localhost:1234/api/authors/168eb522cfdb460f87616cccac46d9bb -XPUT -d '
{
    "cakeday":"2014-06-24T17:16:00+0200"
}'
```

and the server responds with

```
HTTP/1.1 200 OK
Content-Type: application/json; charset=UTF-8
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)
```

Now we access the **element resource** that contains only the modified element by addressing the nested resource directly.

```
dev:~$ curl -i http://localhost:1234/api/authors/168eb522cfdb460f87616cccac46d9bb
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Vary: Accept-Encoding, User-Agent
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
   "query_time": "0.001245593",
   "result_count": 1,
   "result": {
      "name": "Christian Morgner",
      "email": "christian@morgner.de",
      "birthday": "01.01.1970",
      "cakeday": "2014-06-24T17:16:00+0200",
      "id": "168eb522cfdb460f87616cccac46d9bb",
      "type": "Author"
   },
   "serialization_time": "0.000734344"
}
```

Note that the result in the JSON document is of type 'Object' now, whereas the result object in the collection resource  is of type 'Array'.

#### DELETE
And finally, to complete this tiny REST introduction, we use the DELETE verb to remove entity we just created.

```
dev:~$ curl -i http://localhost:1234/api/authors/168eb522cfdb460f87616cccac46d9bb -XDELETE
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)
```

And we can see that the collection resource again contains 0 elements, and direct access to the element resource results in a 404 Not Found error.

```
dev:~$ curl -i http://localhost:1234/api/authors
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Vary: Accept-Encoding, User-Agent
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
   "query_time": "0.002759945",
   "result_count": 0,
   "result": [],
   "serialization_time": "0.000121890"
}

dev:~$ curl -i http://localhost:1234/api/authors/168eb522cfdb460f87616cccac46d9bb
HTTP/1.1 404 Not Found
Content-Type: application/json; charset=utf-8
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
  "code": 404
}
```

### Validation and semantic error messages
Now that we have our first basic node entity in place, we can move on to the next step, which is data validation. In a typical business application, you will have to enforce business rules on your data, e.g. there can only be one instance of a certain type, e-mail addresses need to match given pattern, some strings have a minimum length etc.

#### Validation callbacks
There are two different ways to ensure validation of entities in structr. The first and more powerful way is *callback-based validation*, which means that you can override a certain method to implement validation of your nodes in a global context. The following code illustrates how a validation callback can be used to enforce non-emptyness of the `name` property of Author entities.

```java
public class Author extends AbstractNode {

	public static final Property<String> email = new StringProperty("email",
		new SimpleRegexValidator("[A-Za-z0-9!#$%&'*+-/=?^_`{|}~]+@[A-Za-z0-9-]+(.[A-Za-z0-9-]+)*"),
		new TypeUniquenessValidator(Author.class)
	);

	public static final Property<Date>   birthday = new DateProperty("birthday", "dd.MM.yyyy");
	public static final Property<Date>   cakeday  = new ISO8601DateProperty("cakeday");

	public static final View defaultView = new View(Author.class, PropertyView.Public,
		name, email, birthday, cakeday
	);

	static {

		// register a uniqueness validator on the 'name' property of the type 'Author'
		Author.name.addValidator(new TypeUniquenessValidator(Author.class));
	}
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		
		boolean valid = true;
		
		if (getProperty(name) == null) {
			
			errorBuffer.add("Author", new EmptyPropertyToken(name));
			
			valid = false;
		}
		
		return valid && super.isValid(errorBuffer);
	}
}
```

Note that the implementation of the `isValid` method as shown above can serve as a 'best practice' approach to global validation in structr. 

#### Validators
The second validation method is *validator-based validation*, which means that you can register validator instances for each property key, so that the validation takes place when the value of a given property key is changed. It is important to understand the difference to the callback-based validation above. A validator that is registered on a certain property key will **never** be evaluated if no value for the given property key is set. That means validators can not be used to enforce non-emptyness of property values, and you will have to rely on callback-based validation to ensure non-emtpyness of property values.

#### Uniqueness
However, validators can be used to enforce database-global constraints like uniqueness, that would otherwise have to be implemented manually by the user. In our particular case, we want to be sure that only one author with a given name can exist in the database, to avoid confusion and data inconsistency. Uniqueness can be enforced by registering a `TypeUniquenessValidator` on the `name` property of the Author entity. Since the `name` property is declared in the superclass of Author, we need to register the validator in the static constructor of our entity. The following code illustrates how that can be done.

```java
public class Author extends AbstractNode {

	public static final Property<String> email    = new StringProperty("email");
	public static final Property<Date>   birthday = new DateProperty("birthday", "dd.MM.yyyy");
	public static final Property<Date>   cakeday  = new ISO8601DateProperty("cakeday");

	public static final View defaultView = new View(Author.class, PropertyView.Public,
		name, email, birthday, cakeday
	);

	static {

		// register a uniqueness validator on the 'name' property of the type 'Author'
		Author.name.addValidator(new TypeUniquenessValidator(Author.class));
	}
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		
		boolean valid = true;
		
		if (getProperty(name) == null) {
			
			errorBuffer.add("Author", new EmptyPropertyToken(name));
			
			valid = false;
		}
		
		return valid && super.isValid(errorBuffer);
	}
}
```

#### Pattern matching
Another example for the use of a validator is the validation of an e-mail address using pattern matching. The following code illustrates how to register a regular expression validator and a TypeUniquenessValidator on the `email` property of our Author entity.

```java
public class Author extends AbstractNode {

	public static final Property<String> email    = new StringProperty("email",
		new SimpleRegexValidator("[A-Za-z0-9!#$%&'*+-/=?^_`{|}~]+@[A-Za-z0-9-]+(.[A-Za-z0-9-]+)*"),
		new TypeUniquenessValidator(Author.class)
	);

	public static final Property<Date>   birthday = new DateProperty("birthday", "dd.MM.yyyy");
	public static final Property<Date>   cakeday  = new ISO8601DateProperty("cakeday");

	public static final View defaultView = new View(Author.class, PropertyView.Public,
		name, email, birthday, cakeday
	);

	static {

		// register a uniqueness validator on the 'name' property of the type 'Author'
		Author.name.addValidator(new TypeUniquenessValidator(Author.class));
	}
	
	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		
		boolean valid = true;
		
		if (getProperty(name) == null) {
			
			errorBuffer.add("Author", new EmptyPropertyToken(name));
			
			valid = false;
		}
		
		return valid && super.isValid(errorBuffer);
	}
}
```

Note the difference in the registration of the two TypeUniquenessValidators. This is due to the fact that the `name` property is not declared in the Author entity, but in its superclass AbstractNode, so we need to add the validator afterwards.

#### Semantic error messages
With the above validators in place, the REST server now behaves differently than before. We can not for example create an author without a name.

```
dev:~$ curl -i http://localhost:1234/api/authors -XPOST
```

with a result of

```
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/json; charset=UTF-8
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
  "code": 422,
  "errors": {
    "Author": {
      "name": [
        "must_not_be_empty"
      ]
    }
  }
}
```

Likewise, we can not create an author with an invalid e-mail address:

```
dev:~$ curl -i http://localhost:1234/api/authors -XPOST -d '
{
    "name": "Christian Morgner",
    "email": "test"
}'
```

which will result in

```
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/json; charset=UTF-8
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
  "code": 422,
  "errors": {
    "Author": {
      "email": [
        {
          "must_match": "[A-Za-z0-9!#$%\u0026\u0027*+-/\u003d?^_`{|}~]+@[A-Za-z0-9-]+(.[A-Za-z0-9-]+)*"
        }
      ]
    }
  }
}
```

and finally, when an author with the given name or e-mail address already exists:

```
dev:~$ curl -i http://localhost:1234/api/authors -XPOST -d '
{
    "name": "Christian Morgner",
    "email": "christan@morgner.de"
}'
```

the result is

```
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/json; charset=UTF-8
Transfer-Encoding: chunked
Server: Jetty(8.1.10.v20130312)

{
  "code": 422,
  "errors": {
    "Author": {
      "name": [
        {
          "already_taken": "Christian Morgner",
          "id": "9548cc4837eb4d36a112b59a80b8d5d6"
        }
      ]
    }
  }
}
```

#### Note
We are aware of the redundancy that comes from having two different approaches to validation, but this is due to historical reasons and will be addressed in future releases.




## Appendix A - List of available property types
### StringProperty
### IntProperty
### LongProperty
### DoubleProperty
### FloatProperty
### BooleanProperty
### ArrayProperty
### DateProperty
### ISO8601DateProperty
### EnumProperty
### EntityProperty
### EntityIdProperty
### EntityNotionProperty
### CollectionProperty
### CollectionIdProperty
### CollectionNotionProperty
### AbstractReadOnlyProperty
### AbstractPrimitiveProperty
### GroupProperty
### CypherProperty
### ElementCounter


## Appendix B - List of error tokens
### EmptyPropertyToken






# Things to include in this document
- @Export annotation for REST RPC
- seed.zip
- Import/Export via SyncCommand
- Authentication, principals and the SecurityContext






