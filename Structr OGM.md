#Structr OGM

Structr OGM is an advanced object-to-graph mapping (OGM) library for Neo4j, written in Java.

Structr OGM makes heavy use of Java generics. If you like Java generics, Structr OGM is the right tool for you.

The main strengths of Structr OGM are:

- Fast: Neo4j core API
- Lightweight
- Type-safety, even for relationships
- Lazy evaluation (everything are Iterables)

## Dependencies

To use Structr OGM in your Java app, all you need is to include the following maven dependency:

    <dependency>
        <groupId>org.structr</groupId>
        <artifactId>structr-core</artifactId>
        <version>0.9-SNAPSHOT</version>
    </dependency>


## Model

Let's assume that we created a maven-based Java app with the namespace `com.example.app`.

Please make sure to set the Java source version to 1.7.

### Nodes

First, we create a node entity class `com.example.app.model.Friend`:


    public class Friend extends Person {
	
    	public static final Property<List<Friend>> friends
    	    = new EndNodes<>("friends", FriendsOfFriends.class);
    
    }

### Relationships

To connect friends, we need a relationship class:


    public class FriendsOfFriends extends ManyToMany<Friend, Friend> {

	    @Override
    	public Class<Friend> getSourceType() {
	    	return Friend.class;
    	}

    	@Override
    	public Class<Friend> getTargetType() {
	    	return Friend.class;
    	}

	  	@Override
    	public String name() {
	    	return "FRIEND_OF";
    	}

    }

The methods `getSourceType()` and `getTargetType()` are necessary to provide the types of start and end node which are not available at runtime from the class definition due to Java's type erasure.