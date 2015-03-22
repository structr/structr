package org.structr.schema.export;

import java.net.URISyntaxException;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
public class StructrSchemaTest extends StructrTest {

	public void testSchemaBuilder() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonType task      = sourceSchema.addType("Task");
			final JsonProperty title = task.addStringProperty("title", "public", "ui").setRequired(true);
			final JsonProperty desc  = task.addStringProperty("description", "public", "ui").setRequired(true);

			final JsonType project = sourceSchema.addType("Project");
			project.addStringProperty("name", "public", "ui").setRequired(true);

			project.addArrayReference("tasks", task).setDirection("out");
			task.addReference("project", project).setDirection("in");

			// test function property
			task.addScriptProperty("displayName", "public", "ui").setSource("this.name").setContentType("text/structrscript");
			task.addScriptProperty("javascript", "public", "ui").setSource("{ var x = 'test'; return x; }").setContentType("text/javascript");

			// test enums
			project.addEnumProperty("status", "ui").setEnums("active", "planned", "finished");

			



			// test URIs
			assertEquals("Invalid schema URI", "https://structr.org/schema#", sourceSchema.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/definitions/Task", task.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/definitions/Task/properties/title", title.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/definitions/Task/properties/description", desc.getId().toString());




			compareSchemaRoundtrip(sourceSchema);

		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {

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
}
