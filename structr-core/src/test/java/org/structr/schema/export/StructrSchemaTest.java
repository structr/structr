package org.structr.schema.export;

import com.google.gson.GsonBuilder;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonSchema.Cascade;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrSchemaTest extends StructrTest {

	public void testSimpleProperties() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			// a task
			final JsonType customer = sourceSchema.addType("Customer");

			customer.addStringProperty("name", "public", "ui").setRequired(true).setUnique(true);
			customer.addStringProperty("street", "public", "ui");
			customer.addStringProperty("city", "public", "ui");
			customer.addDateProperty("birthday", "public", "ui");
			customer.addEnumProperty("status", "public", "ui").setEnums("active", "retired", "none");
			customer.addIntegerProperty("count", "public", "ui").setMinimum(1).setMaximum(10, true);
			customer.addNumberProperty("number", "public", "ui").setMinimum(2.0, true).setMaximum(5.0, true);
			customer.addLongProperty("loong", "public", "ui").setMinimum(20, true).setMaximum(50);
			customer.addBooleanProperty("isCustomer", "public", "ui");
			customer.addScriptProperty("displayName", "public", "ui").setSource("concat(this.name, '.', this.id)");

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			testMapPathValue(map, "definitions.Customer.type",                                   "object");
			testMapPathValue(map, "definitions.Customer.required.0",                             "name");
			testMapPathValue(map, "definitions.Customer.properties.city.unique",                 null);
			testMapPathValue(map, "definitions.Customer.properties.count.type",                  "integer");
			testMapPathValue(map, "definitions.Customer.properties.count.minimum",               1.0);
			testMapPathValue(map, "definitions.Customer.properties.count.maximum",               10.0);
			testMapPathValue(map, "definitions.Customer.properties.count.exclusiveMaximum",      true);
			testMapPathValue(map, "definitions.Customer.properties.number.type",                 "number");
			testMapPathValue(map, "definitions.Customer.properties.number.minimum",              2.0);
			testMapPathValue(map, "definitions.Customer.properties.number.maximum",              5.0);
			testMapPathValue(map, "definitions.Customer.properties.number.exclusiveMinimum",     true);
			testMapPathValue(map, "definitions.Customer.properties.number.exclusiveMaximum",     true);
			testMapPathValue(map, "definitions.Customer.properties.loong.type",                  "long");
			testMapPathValue(map, "definitions.Customer.properties.loong.minimum",               20.0);
			testMapPathValue(map, "definitions.Customer.properties.loong.maximum",               50.0);
			testMapPathValue(map, "definitions.Customer.properties.loong.exclusiveMinimum",      true);
			testMapPathValue(map, "definitions.Customer.properties.isCustomer.type",             "boolean");
			testMapPathValue(map, "definitions.Customer.properties.displayName.type",            "script");
			testMapPathValue(map, "definitions.Customer.properties.displayName.source",          "concat(this.name, '.', this.id)");
			testMapPathValue(map, "definitions.Customer.properties.name.type",                   "string");
			testMapPathValue(map, "definitions.Customer.properties.name.unique",                 true);
			testMapPathValue(map, "definitions.Customer.properties.street.type",                 "string");
			testMapPathValue(map, "definitions.Customer.properties.street.type",                 "string");
			testMapPathValue(map, "definitions.Customer.properties.status.type",                 "string");
			testMapPathValue(map, "definitions.Customer.properties.status.enum.0",               "active");
			testMapPathValue(map, "definitions.Customer.properties.status.enum.1",               "none");
			testMapPathValue(map, "definitions.Customer.properties.status.enum.2",               "retired");
			testMapPathValue(map, "definitions.Customer.views.public.0",                         "birthday");
			testMapPathValue(map, "definitions.Customer.views.public.1",                         "city");
			testMapPathValue(map, "definitions.Customer.views.public.2",                         "count");
			testMapPathValue(map, "definitions.Customer.views.public.3",                         "displayName");
			testMapPathValue(map, "definitions.Customer.views.public.4",                         "isCustomer");
			testMapPathValue(map, "definitions.Customer.views.public.5",                         "loong");
			testMapPathValue(map, "definitions.Customer.views.public.6",                         "name");
			testMapPathValue(map, "definitions.Customer.views.public.7",                         "number");
			testMapPathValue(map, "definitions.Customer.views.public.8",                         "status");
			testMapPathValue(map, "definitions.Customer.views.public.9",                         "street");
			testMapPathValue(map, "definitions.Customer.views.ui.0",                             "birthday");
			testMapPathValue(map, "definitions.Customer.views.ui.1",                             "city");
			testMapPathValue(map, "definitions.Customer.views.ui.2",                             "count");
			testMapPathValue(map, "definitions.Customer.views.ui.3",                             "displayName");
			testMapPathValue(map, "definitions.Customer.views.ui.4",                             "isCustomer");
			testMapPathValue(map, "definitions.Customer.views.ui.5",                             "loong");
			testMapPathValue(map, "definitions.Customer.views.ui.6",                             "name");
			testMapPathValue(map, "definitions.Customer.views.ui.7",                             "number");
			testMapPathValue(map, "definitions.Customer.views.ui.8",                             "status");
			testMapPathValue(map, "definitions.Customer.views.ui.9",                             "street");

			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	public void testInheritance() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			// a task
			final JsonType contact  = sourceSchema.addType("Contact").setExtends(StructrApp.getSchemaId(AbstractUser.class));
			final JsonType customer = sourceSchema.addType("Customer").setExtends(contact);

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			testMapPathValue(map, "definitions.Contact.type",      "object");
			testMapPathValue(map, "definitions.Contact.$extends",  "https://structr.org/v1.1/definitions/AbstractUser");

			testMapPathValue(map, "definitions.Customer.type",      "object");
			testMapPathValue(map, "definitions.Customer.$extends",  "#/definitions/Contact");


			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	public void testSimpleSymmetricReferences() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			// a task
			final JsonObjectType project = sourceSchema.addType("Project");
			final JsonObjectType task    = sourceSchema.addType("Task");

			// create relation
			final JsonReferenceType rel = project.relate(task, "has", Cardinality.OneToMany, "project", "tasks");
			rel.setName("ProjectTasks");

			final String schema = sourceSchema.toString();

			System.out.println(schema);

			// test map paths
			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			testMapPathValue(map, "definitions.Project.type",                        "object");
			testMapPathValue(map, "definitions.Project.properties.tasks.$link",      "#/definitions/ProjectTasks");
			testMapPathValue(map, "definitions.Project.properties.tasks.items.$ref", "#/definitions/Task");
			testMapPathValue(map, "definitions.Project.properties.tasks.type",       "array");

			testMapPathValue(map, "definitions.ProjectTasks.$source",                "#/definitions/Project");
			testMapPathValue(map, "definitions.ProjectTasks.$target",                "#/definitions/Task");
			testMapPathValue(map, "definitions.ProjectTasks.cardinality",            "OneToMany");
			testMapPathValue(map, "definitions.ProjectTasks.rel",                    "has");
			testMapPathValue(map, "definitions.ProjectTasks.sourceName",             "project");
			testMapPathValue(map, "definitions.ProjectTasks.targetName",             "tasks");
			testMapPathValue(map, "definitions.ProjectTasks.type",                   "object");

			testMapPathValue(map, "definitions.Task.type",                           "object");
			testMapPathValue(map, "definitions.Task.properties.project.$link",       "#/definitions/ProjectTasks");
			testMapPathValue(map, "definitions.Task.properties.project.$ref",        "#/definitions/Project");
			testMapPathValue(map, "definitions.Task.properties.project.type",        "object");

			// test
			compareSchemaRoundtrip(sourceSchema);

		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	public void testSchemaBuilder() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final String instanceId       = app.getInstanceId();

			// a task
			final JsonObjectType task = sourceSchema.addType("Task");
			final JsonProperty title  = task.addStringProperty("title", "public", "ui").setRequired(true);
			final JsonProperty desc   = task.addStringProperty("description", "public", "ui").setRequired(true);

			// test function property
			task.addScriptProperty("displayName", "public", "ui").setSource("this.name");
			task.addScriptProperty("javascript", "public", "ui").setSource("{ var x = 'test'; return x; }").setContentType("text/javascript");


			// a project
			final JsonObjectType project = sourceSchema.addType("Project");
			project.addStringProperty("name", "public", "ui").setRequired(true);

			final JsonReferenceType projectTasks = project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");
			projectTasks.setCascadingCreate(Cascade.targetToSource);

			project.getViewPropertyNames("public").add("tasks");
			task.getViewPropertyNames("public").add("project");


			// test enums
			project.addEnumProperty("status", "ui").setEnums("active", "planned", "finished");


			// a worker
			final JsonObjectType worker = sourceSchema.addType("Worker");
			final JsonReferenceType workerTasks = worker.relate(task, "HAS", Cardinality.OneToMany, "worker", "tasks");
			workerTasks.setCascadingDelete(Cascade.sourceToTarget);


			// reference Worker -> Task
			final JsonReferenceProperty workerProperty = workerTasks.getSourceProperty();
			final JsonReferenceProperty tasksProperty  = workerTasks.getTargetProperty();
			tasksProperty.setName("renamedTasks");


			worker.addReferenceProperty("taskNames",  tasksProperty, "public", "ui").setProperties("name");
			worker.addReferenceProperty("taskInfos",  tasksProperty, "public", "ui").setProperties("id", "name");
			worker.addReferenceProperty("taskErrors", tasksProperty, "public", "ui");


			task.addReferenceProperty("workerName",   workerProperty, "public", "ui").setProperties("name");
			task.addReferenceProperty("workerNotion", workerProperty, "public", "ui");




			// test date properties..
			project.addDateProperty("startDate", "public", "ui");

			// methods
			project.addMethod("onCreate", "set(this, 'name', 'wurst')");



			// test URIs
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/#", sourceSchema.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task", task.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/title", title.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/description", desc.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Worker/properties/renamedTasks", tasksProperty.getId().toString());





			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	private void compareSchemaRoundtrip(final JsonSchema sourceSchema) throws FrameworkException, InvalidSchemaException, URISyntaxException {

		final String source = sourceSchema.toString();

		System.out.println("##################### source");
		System.out.println(source);

		final JsonSchema targetSchema = StructrSchema.createFromSource(sourceSchema.toString());
		final String target = targetSchema.toString();

		System.out.println("##################### target");
		System.out.println(target);

		assertEquals("Invalid schema (de)serialization roundtrip result", source, target);

		StructrSchema.replaceDatabaseSchema(app, targetSchema);

		final JsonSchema replacedSchema = StructrSchema.createFromDatabase(app);
		final String replaced = replacedSchema.toString();

		System.out.println("##################### replaced");
		System.out.println(replaced);

		assertEquals("Invalid schema replacement result", source, replaced);
	}

	@Override
	public void setUp() {

		final Map<String, Object> config = new HashMap<>();

		config.put("NodeExtender.log", "true");

		super.setUp(config);
	}

	// ----- private methods -----
	private void testMapPathValue(final Map<String, Object> map, final String mapPath, final Object value) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					current = ((List)current).get(index);
				}

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		assertEquals("Invalid map path result for " + mapPath, value, current);
	}
}
