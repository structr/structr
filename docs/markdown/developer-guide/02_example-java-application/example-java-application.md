Let's create a small HelloWorld application, featuring some persons and cities.

First, create a Maven project in your preferred IDE and make sure to use Java 1.7. Then add the a dependency to ``structr-ui`` so that your ``pom.xml`` looks similar to this:

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.structr.example</groupId>
        <artifactId>helloworld</artifactId>
        <version>1.0-SNAPSHOT</version>
        <packaging>jar</packaging>
        <properties>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            <maven.compiler.source>1.7</maven.compiler.source>
            <maven.compiler.target>1.7</maven.compiler.target>
        </properties>
    	<dependencies>
    		<dependency>
    			<groupId>org.structr.</groupId>
    			<artifactId>structr-ui</artifactId>
    			<version>1.0-SNAPSHOT</version>
    		</dependency>
    	</dependencies>
    </project>

We start with a custom class ``ExampleUser``, extending the built-in class ``Person``:

    package org.structr.example.helloworld;

    import java.util.List;
    import org.structr.common.PropertyView;
    import org.structr.common.View;
    import org.structr.core.entity.Person;
    import org.structr.core.property.EndNodes;
    import org.structr.core.property.Property;
    import org.structr.core.property.StringProperty;

    public class ExampleUser extends Person {

        // Simple String property
        public static final Property<String>                nickName = new StringProperty("nickName");

        // Define a many-to-many relation between ExampleUser nodes via the Likes relation class
        public static final Property<List<ExampleUser>> likedPersons = new EndNodes<>("likedPersons", Likes.class);

        // Make properties visible in the public REST view
        public static final View publicView = new View(ExampleUser.class, PropertyView.Public,
            nickName, likedPersons
        );


    }

Next, create a custom relation class ``Likes``:

    package org.structr.example.helloworld;

    import org.structr.core.entity.ManyToMany;

    /**
     * Custom class to define a many-to-many relation between instances
     */
    public class Likes extends ManyToMany<ExampleUser, ExampleUser> {

        @Override
        public Class<ExampleUser> getSourceType() {
            return ExampleUser.class;
        }

        @Override
        public Class<ExampleUser> getTargetType() {
            return ExampleUser.class;
        }

        @Override
        public String name() {
            return "LIKES";
        }
    }


Now we can write the app, in this example in a Main class:

    package org.structr.example.helloworld;

    import org.structr.common.error.FrameworkException;
    import org.structr.core.app.App;
    import org.structr.core.app.StructrApp;
    import org.structr.core.entity.Person;
    import org.structr.core.graph.Tx;

    public class Main {

        public static void main(final String[] arg) {

            // Create an application instance
            final App app = StructrApp.getInstance();

            // Start a transaction
            try (final Tx tx = app.tx()) {

                // Create two nodes of the custom type ExampleUser
                final ExampleUser alice = app.create(ExampleUser.class, "Alice");
                final ExampleUser bob = app.create(ExampleUser.class, "Bob");

                // Create a relationship of the custom relation type Likes
                app.create(alice, bob, Likes.class);

                // Finish transaction and commit data to Neo4j
                // This is needed only after write operations
                tx.success();

            } catch (FrameworkException e) {
                e.printStackTrace();
            }


            try (final Tx tx = app.tx()) {

                // List all persons
                for (final ExampleUser p : app.nodeQuery(ExampleUser.class).getAsList()) {
                    System.out.println(p.getProperty(Person.name));
                }

                final ExampleUser alice = app.nodeQuery(ExampleUser.class).andName("Alice").getFirst();

                // List all persons liked by Alice
                for (final ExampleUser p : alice.getProperty(ExampleUser.likedPersons)) {
                    System.out.println(p.getProperty(ExampleUser.name));
                }

                // Read transaction, doesn't need tx.success()

            } catch (FrameworkException e) {
                e.printStackTrace();
            }

        }

    }

We're ready to run the app, everything else is done by Structr internally, so this should give the following output:

    ------------------------------------------------------------------------
    Building helloworld 1.0-SNAPSHOT
    ------------------------------------------------------------------------

    --- exec-maven-plugin:1.2.1:exec (default-cli) @ helloworld ---
    Apr 14, 2014 11:54:42 AM org.structr.core.Services initialize
    INFO: Reading structr.conf..
    Apr 14, 2014 11:54:42 AM org.structr.core.Services initialize
    WARNING: Unable to read configuration file structr.conf: structr.conf (Datei oder Verzeichnis nicht gefunden)
    Apr 14, 2014 11:54:42 AM org.structr.core.Services getResources
    INFO: Found 0 possible resources: []
    Apr 14, 2014 11:54:43 AM org.structr.module.JarConfigurationProvider scanResources
    INFO: 4 JARs scanned
    Apr 14, 2014 11:54:43 AM org.structr.core.Services initialize
    INFO: Starting services
    Apr 14, 2014 11:54:43 AM org.structr.core.graph.NodeService initialize
    INFO: Initializing database (/home/axel/dev/helloworld/db) ...
    Apr 14, 2014 11:54:44 AM org.structr.core.graph.NodeService initialize
    INFO: Database ready.
    Apr 14, 2014 11:54:45 AM org.structr.agent.AgentService run
    INFO: AgentService started
    Apr 14, 2014 11:54:45 AM org.structr.cron.CronService initialize
    WARNING: No cron expression for task , ignoring.
    Apr 14, 2014 11:54:45 AM org.structr.core.Services initialize
    INFO: 3 service(s) processed
    Apr 14, 2014 11:54:45 AM org.structr.core.Services initialize
    INFO: Registering shutdown hook.
    Apr 14, 2014 11:54:45 AM org.structr.core.Services initialize
    INFO: Initialization complete
    Alice
    Bob
    Bob

