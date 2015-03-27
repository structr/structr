package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import static org.structr.core.entity.Relation.Cardinality.OneToMany;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonRelationship;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrSchemaTest extends StructrTest {

//	public void testInheritance() {
//
//		try {
//
//			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
//
//			// a task
//			final JsonType contact  = sourceSchema.addType("Contact").setExtends(StructrApp.getSchemaId(AbstractUser.class));
//			final JsonType customer = sourceSchema.addType("Customer").setExtends(contact);
//
//			customer.addStringProperty("test");
//
//			compareSchemaRoundtrip(sourceSchema);
//
//		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {
//
//			ex.printStackTrace();
//			fail("Unexpected exception.");
//		}
//
//	}

	public void testSimpleSymmetricReferences() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			// a task
			final JsonType project = sourceSchema.addType("Project");
			final JsonType task    = sourceSchema.addType("Task");

			// create relation
			final JsonRelationship rel = project.relate(task, "has", OneToMany);
			rel.setName("ProjectTasks");

			// test
			compareSchemaRoundtrip(sourceSchema);

		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

//	public void testSymmetricReferencesWithRelationTypes() {
//
//		try {
//
//			// this test should create two symmetric relationships, one with
//			// the relationship type HAS, the other with the default type
//
//			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
//
//			// a task
//			final JsonType project = sourceSchema.addType("Project");
//			final JsonType task    = sourceSchema.addType("Task");
//
//			project.addArrayReference("tasks", "HAS", task);
//			task.addReference("project", "HAS", project);
//
//			compareSchemaRoundtrip(sourceSchema);
//
//		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {
//
//			ex.printStackTrace();
//			fail("Unexpected exception.");
//		}
//
//	}
//
//	public void testSchemaBuilder() {
//
//		try {
//
//			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
//
//			// a task
//			final JsonType task      = sourceSchema.addType("Task");
//			final JsonProperty title = task.addStringProperty("title", "public", "ui").setRequired(true);
//			final JsonProperty desc  = task.addStringProperty("description", "public", "ui").setRequired(true);
//
//			// test function property
//			task.addScriptProperty("displayName", "public", "ui").setSource("this.name").setContentType("text/structrscript");
//			task.addScriptProperty("javascript", "public", "ui").setSource("{ var x = 'test'; return x; }").setContentType("text/javascript");
//
//
//			// a project
//			final JsonType project = sourceSchema.addType("Project");
//			project.addStringProperty("name", "public", "ui").setRequired(true);
//
//			project.addArrayReference("tasks", "HAS", task).setDirection(out);
//			task.addReference("project", "HAS", project).setDirection(in);
//
//			// test enums
//			project.addEnumProperty("status", "ui").setEnums("active", "planned", "finished");
//
//			// a worker
//			final JsonType worker                      = sourceSchema.addType("Worker");
//			final JsonReferenceProperty tasksProperty  = worker.addArrayReference("tasks", "HAS", task, "public", "ui").setDirection(in);
//			final JsonReferenceProperty workerProperty = task.addReference("worker", "HAS", worker, "public", "ui").setDirection(out).setCascadingDelete(Cascade.sourceToTarget);
//
//			worker.addArrayReference("taskNames", tasksProperty, "public", "ui").setProperties("name");
//			worker.addArrayReference("taskInfos", tasksProperty, "public", "ui").setProperties("id", "name");
//			worker.addArrayReference("taskErrors", tasksProperty, "public", "ui");
//
//
//			task.addReference("workerName", workerProperty, "public", "ui").setProperties("name");
//			task.addReference("workerNotion", workerProperty, "public", "ui");
//
//
//			// test date properties..
//			project.addDateProperty("startDate", "public", "ui");
//
//			// methods
//			project.addMethod("onCreate", "set(this, 'name', 'wurst')");
//
//
//
//			// test URIs
//			assertEquals("Invalid schema URI", "https://structr.org/schema#", sourceSchema.getId().toString());
//			assertEquals("Invalid schema URI", "https://structr.org/definitions/Task", task.getId().toString());
//			assertEquals("Invalid schema URI", "https://structr.org/definitions/Task/properties/title", title.getId().toString());
//			assertEquals("Invalid schema URI", "https://structr.org/definitions/Task/properties/description", desc.getId().toString());
//
//
//
//
//			compareSchemaRoundtrip(sourceSchema);
//
//		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {
//
//			ex.printStackTrace();
//			fail("Unexpected exception.");
//		}
//	}

	private void compareSchemaRoundtrip(final JsonSchema sourceSchema) throws FrameworkException, InvalidSchemaException, URISyntaxException {

		final String source = sourceSchema.toString();

		System.out.println("##################### source");
		System.out.println(source);

		final JsonSchema targetSchema = StructrSchema.createFromSource(sourceSchema.toString());
		final String target = targetSchema.toString();

		System.out.println("##################### target");
		System.out.println(target);

		assertEquals("Invalid schema (de)serialization roundtrip result", source, target);



		// second part, test database roundtrip

		StructrSchema.replaceDatabaseSchema(app, targetSchema);

		final JsonSchema replacedSchema = StructrSchema.createFromDatabase(app);
		final String replaced = replacedSchema.toString();

		System.out.println("##################### replaced");
		System.out.println(replaced);

		final JsonSchema databaseSchema = StructrSchema.createFromDatabase(app);
		final String database = databaseSchema.toString();

		assertEquals("Invalid schema replacement result", replaced, database);
	}

	public void setUp() {

		final Map<String, Object> config = new HashMap<>();

//		config.put("NodeExtender.log", "true");

		super.setUp(config);
	}
}
