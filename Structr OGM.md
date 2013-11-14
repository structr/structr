#Structr OGM

Structr OGM is a type-safe object-to-graph mapping library for Neo4j, written in Java. It makes extensive use of Java generics, so if you like them, Structr OGM is the right tool for you.

## Dependencies

To use Structr OGM in your Java application, all you need is to include the following maven dependency:

    <dependency>
        <groupId>org.structr</groupId>
        <artifactId>structr-core</artifactId>
        <version>0.9-SNAPSHOT</version>
    </dependency>


## Model

Let's assume that we created a maven-based Java app with the namespace `com.example.app`.

Please make sure to set the Java source version to at least 1.7.

### Nodes

First, we create a node entity class `com.example.app.model.Friend`:

    public class Friend extends AbstractNode {
	
	    public static final Property<List<Friend>>	friends = new EndNodes<>("friends",	FriendsOfFriends.class);
    	public static final Property<City>		    city    = new StartNode<>("city",	Citizen.class);
	
    }

### Relationships

To connect friends, we need a relationship class:

    public class FriendsOfFriends extends ManyToMany<Friend, Friend> {

	  	@Override
    	public String name() {
	    	return "FRIEND_OF";
    	}

	    @Override
    	public Class<Friend> getSourceType() {
	    	return Friend.class;
    	}

    	@Override
    	public Class<Friend> getTargetType() {
	    	return Friend.class;
    	}

    }

The methods `getSourceType()` and `getTargetType()` provide the types of start and end node at runtime which is not available at runtime from the class definition due to Java's type erasure.

## A Simple Example Application

Now we need a Java app to run, so we create a `com.example.app.App` with the following code in the `main` method to initialize Structr:

    public class App {

	    public static void main(String[] args) {
		
		try {
			
			StructrApp app = StructrApp.getInstance("/tmp/structr");
			
			[...]
	
		} catch (FrameworkException ex) {
			Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}

The `StructrApp` object is a facade for the most common methods of the Structr OGM layer.

## Create Objects

Creating objects with Structr OGM is as simple as that:

    Friend me = app.create(Friend.class, "Axel");
    
With this single method call, an object `me` is created and persisted with its type `Friend` and the name "Axel". Note that the `name` property is inherited from Structr's `AbstractNode` class.

## Find Objects

To get objects back from the graph store, there are some convenience methods:

    // Returns Michael
    Friend aFriend = app.get(Friend.class, michael.getUuid());
    
    // Returns Alice, Axel, Bob, Christian
    List<Friend> friends = app.get(Friend.class);
    
    // Returns Axel and Alice
    List<Friend> friends = app.search(Friend.class, "A*");
    
    // Returns Christian
    Friend aFriend = app.searchFirst(Friend.class, "Christian"));

## Read and Write Objects

Data objects in Structr can be read and written by using the `setProperty/getProperty` pattern:

    Friend michael = app.create(Friend.class);
    String name = michael.getProperty(Friend.name);    
    name == null; // true
    
    michael.setProperty(Friend.name, "Michael");   
    name = michael.getProperty(Friend.name);
    name.equals("Michael"); // true
    
Creating relationships between objects is completely wrapped in the above methods:

    me.setProperty(Friend.city, app.create(City.class, "Frankfurt));
    
creates a new city named "Frankfurt" and connects it to the `me`.

    Friend aFriend = app.searchFirst(City.class, "Frankfurt).getProperty(City.citizen);
    aFriend.equals(me); // true
    
## Transactions    
    
As the underlying database Neo4j is transactional, all operations have to be wrapped in transactions.

A transaction is started with `beginTx()` and committed with `commitTx()`. Validation happens in the `commitTx()` method which can throw a `FrameworkException` we should catch:


		try {
			
			StructrApp app = StructrApp.getInstance("/tmp/structr");
			
            app.beginTx();

            [...]
            
            app.commitTx();
		
		} catch (FrameworkException ex) {
			Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
		}



    
    
