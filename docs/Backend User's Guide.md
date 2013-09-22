# Backend User's Guide
This document contains a step-by-step guide to the Structr backend. We will start with some information about the concepts and technologies and incrementally build a fully functional backend for a real-life use case.

You should already know what a REST server is, and why you want to use such a server on top of a graph database, which has its own advantages and disadvantages compared to a relational database. You should also be familiar with Java, Apache Maven and git, as Structr is hosted on github and we rely on Maven to manage dependencies and builds etc.

## Table of contents
- [About Structr](#about-Structr)
- [The Neo4j property graph](#the-neo4j-property-graph)
- [The first steps](#the-first-steps)
    - [Creating a new project](#creating-a-new-project)
        - [Server.java](#serverjava)
        - [Adding custom configuration elements](#adding-custom-configuration-elements)
        - [The configuration file (Structr.conf)](#the-configuration-file-Structrconf)
        - [Caution](#caution)
    - [Starting the server](#starting-the-server)
        - [Compiling](#compiling)
        - [Using Maven to start the server](#using-maven-to-start-the-server)
        - [Starting the server from the command line (or a script)](#starting-the-server-from-the-command-line-or-a-script)    
        - [Debugging](#debugging)
        - [Tips](#tips)
    - [Building the data model](#building-the-data-model)
        - [Entities](#entities)
        - [Node entities](#node-entities)
        - [Relationship types](#relationship-types)
        - [Author.java](#authorjava)
        - [Properties and Views](#properties-and-views)
            - [Pre-defined views](#pre-defined-views)
            - [Inheritance](#inheritance)
        - [Properties](#properties)
    - [REST access](#rest-access)
        - [GET](#get)
        - [POST](#post)
        - [PUT](#put) 
        - [DELETE](#delete)
    - [Validation and semantic error messages](#validation-and-semantic-error-messages)
        - [Validation callbacks](#validation-callbacks)
        - [Validators](#validators)
        - [Uniqueness](#uniqueness)
        - [Pattern matching](#pattern-matching)
        - [Semantic error messages](#semantic-error-messages)
        - [Note](#note)
- [Indexing, searching, sorting and paging](#indexing-searching-sorting-and-paging)
    - [Fulltext and keyword index](#fulltext-and-keyword-index)
    - [Setup](#setup)
    - [Exact search](#ordinary-search)
    - [Fulltext search](#fulltext-search)
    - [Range queries](#range-queries)
        - [Example range query terms](#example-range-query-terms)
    - [Sorting](#sorting)
    - [Paging](#paging)
        - [Simple Paging](#simple-paging)
        - [Paging on a given element](#paging-on-a-given-element)
    
- [Adding relationships](#adding-relationships)
- [Appendix A - List of available property types](#appendix-a---list-of-available-property-types)
    - [StringProperty](#stringproperty)
    - [IntProperty](#intproperty)
    - [LongProperty](#longproperty)
    - [DoubleProperty](#doubleproperty)
    - [FloatProperty](#floatproperty)
    - [BooleanProperty](#booleanproperty)
    - [ArrayProperty](#arrayproperty)
    - [DateProperty](#dateproperty)
    - [ISO8601DateProperty](#iso8601dateproperty)
    - [EnumProperty](#enumproperty)
    - [EntityProperty](#entityproperty)
    - [EntityIdProperty](#entityidproperty)
    - [EntityNotionProperty](#entitynotionproperty)
    - [CollectionProperty](#collectionproperty)
    - [CollectionIdProperty](#collectionidproperty)
    - [CollectionNotionProperty](#collectionnotionproperty)
    - [AbstractReadOnlyProperty](#abstractreadonlyproperty)
    - [AbstractPrimitiveProperty](#abstractprimitiveproperty)
    - [GroupProperty](#groupproperty)
    - [CypherProperty](#cypherproperty)
    - [ElementCounter](#elementcounter)
- [Appendix B - List of error tokens](#appendix-b---list-of-error-tokens)
    - [EmptyPropertyToken](#emptypropertytoken)
- [Things to include in this document](#-things-to-include-in-this-document)

## About Structr
The Structr REST server essentially is a graph-based JSON document store, where documents are automatically transformed into graph structures and back, according to a pre-defined schema. This schema consists of node and relationship entities, property definitions and configurable views which will be described in detail in the following chapters.

## The Neo4j property graph
Data in a Neo4j database is stored in what is called a property graph. This graph consists of nodes and relationships, which both can have an arbitrary number of primitive properties, including arrays. Properties are stored and retrieved using a String key, so you can think of such a property container as a kind of persistent map. Nodes are the basic building blocks of a property graph and can be connected to other nodes using relationships.

## The first steps
### Creating a new project
Structr has several Maven archetypes available for you to start your project with. As we want to create a backend project from scratch, we will use the archetype named `Structr-base-archetype`. So the first step would be to let Maven create a new project from the base archetype:

    mvn archetype:generate \
        -DarchetypeRepository=http://maven.Structr.org/artifactory/snapshot \
        -DarchetypeGroupId=org.Structr \
        -DarchetypeArtifactId=Structr-base-archetype \
        -DarchetypeVersion=0.8-SNAPSHOT \
        -DgroupId=org.Structr \
        -DartifactId=example-backend \
        -Dversion=0.1 \
        -Dpackage=org.Structr.example

When you execute this command, Maven will ask you to confirm the choices you made and will create a project named `example-backend` in the current directory. We assume you are already familiar with the layout of a Maven project, so let's jump directly into the code.

#### Server.java
The Server class is the main class of every Structr project. It is responsible for both configuration and startup of the REST backend. The following code fragment shows the how a typical Server.java looks like when it is first created from the base archetype.

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
The server class provides an easy method to add your own, custom configuration elements that you can later access from within your application code. Just use the `addCustomConfig()` method to bring your custom configuration element into the Structr configuration.

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

#### The configuration file (Structr.conf)
When Structr is started for the first time, it creates a new configuration file from the values that are encoded in the server class. (*this behaviour needs to be discussed further as it might lead to confusion*)

#### Caution
Please note that the values in the Structr.conf configuration file will override the values specified in the Server.java file, so be sure to remove the Structr.conf file before you start Structr after you added new configuration elements to Server.java

### Starting the server
#### Compiling
To start the server, you must of course first compile the project. Use Maven to run the `install` target, and do a `clean` before that when you compile repeatedly.

    mvn clean install
    
#### Using Maven to start the server
The Structr base archetype includes a Maven exec:exec goal that you can use to start the server. Please note that this is not the preferred way of starting Structr.

    mvn exec:exec
    
#### Starting the server from the command line (or a script)
The preferred way of starting a Structr REST server is by using the following call, which can be used to start from a script (i.e. when running the server as a system service) or from the command line.

    java -Xms1g -Xmx1g -server -classpath target/lib/*:target/example-backend-0.1.jar org.Structr.example.Server

You can of course modify the above JVM switches to better suit your hardware configuration and/or performance needs etc.

#### Debugging
To be able to attach a debugger to a running Structr REST server, you can use the following JVM switches when starting the server.

    -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n
    
Replace the port 5005 in the example above by your preferred debugging port

#### Tips
When you move the Structr working directory to another directory, make sure you delete the Structr.conf because there are absolute paths stored in this file. We are currently working on improving the configuration, so this will hopefully be fixed soon.

## Building the data model
Structr extends the Neo4j property graph, adding type safety, validation, automatic indexing and transparent relationship creation. The basic building block of a Structr REST application is the class `AbstractNode`. All node entities must inherit from this class in order to be available in Structr. 

### Entities
For our previously mentioned use case, we want to model the anatomy of Structr's source code, so we can later create visualizations and statistics on top of that. This brings us to the first few node entities and relationship types.

#### Node entities
- Author
- Interface
- Class

#### Relationship types
- WROTE

### Author.java
The Author entity is the first entity we examine, because it illustrates some of the most basic features of Structr. As mentioned before, all node entities must inherit from `AbstractNode` to be available to the Structr REST server. So the simplest case of a class looks like this.

```java
public class Author extends AbstractNode {}
```

#### Properties and Views
A node like this of course if of very little use, because it has no properties and no output representation. A Structr entity can have many output representations, depending on the REST path it is accessed from, the position in the JSON output document or the relationship over which it is accessed. Output representations in Structr are called *Views* and must be specified explicitly for each entity. A view is a collection of properties that belong together semantically in a given context.

##### Pre-defined views
Structr has a pre-defined set of views, including the default view which will be used if no view was specified explicitly in the REST path. The following views are defined in Structr core and can be used freely in any project.

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

The set of default properties that each Structr node inherits from `AbstractNode` includes a `name` property, so we do not need to declare it by ourselves. This leads us to the first version of our Author entity.

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
Now that we finished the first entity, we can compile the project and start the server (see the previous chapter on how to do that). When the server is up and running, we can run the first REST queries. For each node type, Structr automatically creates a nestable REST collection resource, so that we can manage our newly created Author entities.

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
There are two different ways to ensure validation of entities in Structr. The first and more powerful way is *callback-based validation*, which means that you can override a certain method to implement validation of your nodes in a global context. The following code illustrates how a validation callback can be used to enforce non-emptyness of the `name` property of Author entities.

```java
public class Author extends AbstractNode {

	public static final Property<String> email    = new StringProperty("email");
	public static final Property<Date>   birthday = new DateProperty("birthday", "dd.MM.yyyy");
	public static final Property<Date>   cakeday  = new ISO8601DateProperty("cakeday");

	public static final View defaultView = new View(Author.class, PropertyView.Public,
		name, email, birthday, cakeday
	);

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

Note that the implementation of the `isValid` method as shown above can serve as a 'best practice' approach to global validation in Structr. 

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

## Indexing, searching, sorting and paging
Before we advance to related entities and relationships, there is one feature that can be illustrated with the current project setup. Structr provides automatic indexing, sorting and sophisticated query abilities using the Lucene search engine. In order to use these abilities, we must register properties in an entity to be indexed.

### Fulltext and keyword index
The Neo4j implementation of the Lucene index provider supports fulltext and keyword indexing, which differ in the Analyzer implementation that is used to analyze the values that are indexed for a given entity. Since it is possible to combine more than one search field and sorting in a single REST query, indexed properties are registered in both the *fulltext* and the *keyword* index per default. **Note that Lucene queries will only work correctly if the properties used for searching and sorting are stored in the same index.**

### Setup
Registering a property for indexing can be done using one of the `indexed()` methods of the `PropertyKey` interface. For an explanation of the semantics of each method, please refer to the Structr JavaDocs.

```java
public Property<T> indexed();
public Property<T> indexed(NodeIndex nodeIndex);
public Property<T> indexed(RelationshipIndex relIndex);
public Property<T> passivelyIndexed();
public Property<T> passivelyIndexed(NodeIndex nodeIndex);
public Property<T> passivelyIndexed(RelationshipIndex relIndex);
public Property<T> indexedWhenEmpty();
```

Now with all the properties, views, validation and indexing in place, the final Author entity looks like this.

```java
public class Author extends AbstractNode {

	public static final Property<String> email    = new StringProperty("email",
		new SimpleRegexValidator("[A-Za-z0-9!#$%&'*+-/=?^_`{|}~]+@[A-Za-z0-9-]+(.[A-Za-z0-9-]+)*"),
		new TypeUniquenessValidator(Author.class)
	).indexed();

	public static final Property<Date>   birthday = new DateProperty("birthday", "dd.MM.yyyy").indexed();
	public static final Property<Date>   cakeday  = new ISO8601DateProperty("cakeday").indexed();

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

For the following examples, we created a list of authors in the database so we can search, query, sort and page the list. You can find the script to create the test data in the `example-backend` source code repository.

Note that the `name` property of every node is indexed per default.

### Exact search
Searching the Structr REST server is done by appending the desired search key and the value to the REST URL like in the following examples. Please note that you need to encode the search value according to RFC3986 ("percent-encoding"), which is the standard for URL encoding.

#### Search for an author by name
```
dev:~$ curl -i "http://localhost:1234/api/authors?name=Author%20A"
```

#### Result
```
{
   "query_time": "0.001750355",
   "result_count": 1,
   "result": [
      {
         "name": "Author A",
         "email": "z@Structr.org",
         "birthday": "01.01.1970",
         "cakeday": "2020-01-01T10:00:00+0100",
         "id": "46020bbb748a41859bc8e79661a4c203",
         "type": "Author"
      }
   ],
   "serialization_time": "0.000392451"
}
```

#### Search for an author by birthday
```
dev:~$ curl -i "http://localhost:1234/api/authors?birthday=01.01.1989"
```

#### Result
```
{
   "query_time": "0.001522012",
   "result_count": 1,
   "result": [
      {
         "name": "Author T",
         "email": "g@Structr.org",
         "birthday": "01.01.1989",
         "cakeday": "2001-01-01T10:00:00+0100",
         "id": "f99755edefed4704ab998820fb41e71a",
         "type": "Author"
      }
   ],
   "serialization_time": "0.000377199"
}
```

### Fulltext search
If you want to do a fulltext search on the given fields, you can enable it with the request parameter **loose=1**. The result set is the intersection of the results for each search field, so it is the same as combining the single queries using *AND*.

#### Query
```
dev:~$ curl -i "http://localhost:1234/api/authors?loose=1&name=z&email=a"
```

#### Result
```
{
   "query_time": "0.004597483",
   "result_count": 1,
   "result": [
      {
         "name": "Author Z",
         "email": "a@Structr.org",
         "birthday": "01.01.1995",
         "cakeday": "1995-01-01T10:00:00+0100",
         "id": "15c07deca1c94b3ab0aa60a3b02e2e5e",
         "type": "Author"
      }
   ],
   "serialization_time": "0.000467224"
}
```

### Range queries
Structr supports range queries for both numeric and text fields. The syntax is the same as the one that is used by Lucene, the only difference is that you have to encode the square brackets and spaces in URL encoding.

#### Example range query terms
```
[300 TO 400]
[01.01.1980 TO 01.01.1990]
[A TO Z]
```

#### Query
```
dev:~$ curl -i "http://localhost:1234/api/authors?birthday=%5B01.01.1980%20TO%2001.01.1990%5D"
```

#### Result
```
{
   "query_time": "0.005176975",
   "result_count": 11,
   "result": [
      {
         "name": "Author K",
         "email": "p@Structr.org",
         "birthday": "01.01.1980",
         "cakeday": "2010-01-01T10:00:00+0100",
         "id": "5cb83f7e01404534bfa0ad0db9191912",
         "type": "Author"
      },
      {
         "name": "Author L",
         "email": "o@Structr.org",
         "birthday": "01.01.1981",
         "cakeday": "2009-01-01T10:00:00+0100",
         "id": "a427880cc9ce4e20a0eb023eb8c8ac56",
         "type": "Author"
      },
      {
         "name": "Author M",
         "email": "n@Structr.org",
         "birthday": "01.01.1982",
         "cakeday": "2008-01-01T10:00:00+0100",
         "id": "0aed6335d2f940a590a4ac6cacbd3886",
         "type": "Author"
      },
      {
         "name": "Author N",
         "email": "m@Structr.org",
         "birthday": "01.01.1983",
         "cakeday": "2007-01-01T10:00:00+0100",
         "id": "391ed290e7ef4bbcbb61137efdf9f14f",
         "type": "Author"
      },
      {
         "name": "Author O",
         "email": "l@Structr.org",
         "birthday": "01.01.1984",
         "cakeday": "2006-01-01T10:00:00+0100",
         "id": "ee3f914867704472bad6adf458debb5c",
         "type": "Author"
      },
      {
         "name": "Author P",
         "email": "k@Structr.org",
         "birthday": "01.01.1985",
         "cakeday": "2005-01-01T10:00:00+0100",
         "id": "9847ce91a8154758a762fdbe5937f549",
         "type": "Author"
      },
      {
         "name": "Author Q",
         "email": "j@Structr.org",
         "birthday": "01.01.1986",
         "cakeday": "2004-01-01T10:00:00+0100",
         "id": "43148d2f446742dc95a11112ff48aeb8",
         "type": "Author"
      },
      {
         "name": "Author R",
         "email": "i@Structr.org",
         "birthday": "01.01.1987",
         "cakeday": "2003-01-01T10:00:00+0100",
         "id": "c2aa4d415afa4d9cad75bdacd8c6b8bc",
         "type": "Author"
      },
      {
         "name": "Author S",
         "email": "h@Structr.org",
         "birthday": "01.01.1988",
         "cakeday": "2002-01-01T10:00:00+0100",
         "id": "cda04be3cce849ec8c0bfded5bc62e28",
         "type": "Author"
      },
      {
         "name": "Author T",
         "email": "g@Structr.org",
         "birthday": "01.01.1989",
         "cakeday": "2001-01-01T10:00:00+0100",
         "id": "90a7323b4ae24a16a86cf651b702311e",
         "type": "Author"
      },
      {
         "name": "Author U",
         "email": "f@Structr.org",
         "birthday": "01.01.1990",
         "cakeday": "2000-01-01T10:00:00+0100",
         "id": "c18808619efb4af7a5de6794138afce9",
         "type": "Author"
      }
   ],
   "serialization_time": "0.005467457"
}

```

As you can see, the result set contains the 11 authors whose birthdays lie between 01.01.1980 and 01.01.1990, inclusively.

### Sorting
Sorting a result set in Structr can be done by supplying one or two additional request parameters to the REST query: `sort` and/or `order`. With the `sort` request parameter, you can specify the property key by which the result set will be sorted. The `order` request parameter accepts the values `asc`, which is the default, and `desc`, which of course causes the result set to be returned in descending order.

#### Indexing and Sorting
Please note that you need to mark a property key as being indexed in order to be able to sort a collection by the given key.

#### Query
```
dev:~$ curl -i "http://localhost:1234/api/authors?birthday=%5B01.01.1980%20TO%2001.01.1990%5D&sort=birthday&order=desc"
```

#### Result
```
{
   "query_time": "0.003858155",
   "result_count": 11,
   "result": [
      {
         "name": "Author U",
         "email": "f@Structr.org",
         "birthday": "01.01.1990",
         "cakeday": "2000-01-01T10:00:00+0100",
         "id": "ca747d849a3448e6b755e7ea08040055",
         "type": "Author"
      },
      {
         "name": "Author T",
         "email": "g@Structr.org",
         "birthday": "01.01.1989",
         "cakeday": "2001-01-01T10:00:00+0100",
         "id": "edb98057eb3747a3bfb6837f1c87ee29",
         "type": "Author"
      },
      {
         "name": "Author S",
         "email": "h@Structr.org",
         "birthday": "01.01.1988",
         "cakeday": "2002-01-01T10:00:00+0100",
         "id": "442b3223ff7f48519600cd19babf4acf",
         "type": "Author"
      },
      {
         "name": "Author R",
         "email": "i@Structr.org",
         "birthday": "01.01.1987",
         "cakeday": "2003-01-01T10:00:00+0100",
         "id": "e3889f3133a448ecb0a89606b1b07d48",
         "type": "Author"
      },
      {
         "name": "Author Q",
         "email": "j@Structr.org",
         "birthday": "01.01.1986",
         "cakeday": "2004-01-01T10:00:00+0100",
         "id": "7e67e0cc095d4855937a23e506df264e",
         "type": "Author"
      },
      {
         "name": "Author P",
         "email": "k@Structr.org",
         "birthday": "01.01.1985",
         "cakeday": "2005-01-01T10:00:00+0100",
         "id": "9e2aed5255c841a7be38121b07bf0ed6",
         "type": "Author"
      },
      {
         "name": "Author O",
         "email": "l@Structr.org",
         "birthday": "01.01.1984",
         "cakeday": "2006-01-01T10:00:00+0100",
         "id": "742e60011e5c4c29b6b1a686ed6fef3e",
         "type": "Author"
      },
      {
         "name": "Author N",
         "email": "m@Structr.org",
         "birthday": "01.01.1983",
         "cakeday": "2007-01-01T10:00:00+0100",
         "id": "293c30c464a2410c967e630bd8686c11",
         "type": "Author"
      },
      {
         "name": "Author M",
         "email": "n@Structr.org",
         "birthday": "01.01.1982",
         "cakeday": "2008-01-01T10:00:00+0100",
         "id": "443e4179bdb4427990c6dfbbd0da916b",
         "type": "Author"
      },
      {
         "name": "Author L",
         "email": "o@Structr.org",
         "birthday": "01.01.1981",
         "cakeday": "2009-01-01T10:00:00+0100",
         "id": "27a558a90db34506b4061a2534dff436",
         "type": "Author"
      },
      {
         "name": "Author K",
         "email": "p@Structr.org",
         "birthday": "01.01.1980",
         "cakeday": "2010-01-01T10:00:00+0100",
         "id": "dc1776a068f84ffcb634d5445ed13965",
         "type": "Author"
      }
   ],
   "serialization_time": "0.004278493"
}
```

### Paging
In a database, you often have to deal with large amounts of data. You will most certainly never want to display several thousand objects at once, so you need paging to reduce the size of the result set. In Structr, this can again be done by supplying one to three additional request parameters: `pageSize`, `page` and `offsetId`

#### Simple paging

#### Paging on a given element


## Adding relationships
The next step in our use case is to add the two additional classes that we need to model the source code of Structr. So 




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
The ```EntityProperty``` is used to define a single node of the given type which is connected by a single relationship of the given type.

You can define the JSON name, the class of the connected node and the cardinality.

The following example is a relation between files and a parent folder:

    File (n) <--[:CONTAINS]-- (1) Folder
which can be written as
    
```java    
    public static final EntityProperty<Folder> parent = new EntityProperty<Folder>("parent", Folder.class, RelType.CONTAINS, Direction.INCOMING, true);
```
The boolean parameter defines whether the cardinality is many-to-one (```true```) or one-to-one (```true```). 

Note that the direction of the relationship has nothing to do with the cardinality but is only related to the semantic of the relationship type. To illustrate, here's is another example which is logically equivialent to the above:

    File (n) --[:CONTAINED_IN]--> (1) Folder

which leads to

```java    
    public static final EntityProperty<Folder> parent = new EntityProperty<Folder>("parent", Folder.class, RelType.CONTAINED_ID, Direction.OUTGOING, true);
```

### EntityIdProperty
### EntityNotionProperty
### CollectionProperty
The ```CollectionProperty``` is used to define a collection of nodes of the given type which are connected by relationships of the given type, to 

You can define the JSON name, the class of the connected nodes and the cardinality.

The following example is a relation between a folder and its files:

    Folder (1)  --[:CONTAINS]--> (n) File
which can be written as
    
```java    
    public static final CollectionProperty<File>   files        = new CollectionProperty<File>("files", File.class, RelType.CONTAINS, Direction.OUTGOING, new PropertySetNotion(uuid, name), true);
```
The boolean parameter defines whether the cardinality is many-to-one (```true```) or one-to-one (```true```). 

Note that the direction of the relationship has nothing to do with the cardinality but is only related to the semantic of the relationship type. To illustrate, here's is another example which is logically equivialent to the above:

    Folder (1) <--[:CONTAINED_ID]-- (n) File

which leads to

```java    
    public static final CollectionProperty<File>   files        = new CollectionProperty<File>("files", File.class, RelType.CONTAINED_IN, Direction.INCOMING, new PropertySetNotion(uuid, name), true);
    
```

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
- Notions






