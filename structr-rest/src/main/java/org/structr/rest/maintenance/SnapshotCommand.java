package org.structr.rest.maintenance;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonSchema;

/**
 *
 * @author Christian Morgner
 */
public class SnapshotCommand extends NodeServiceCommand implements MaintenanceCommand {

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String mode = (String)attributes.get("mode");
		if (mode != null) {

			if ("export".equals(mode)) {

				createSnapshot(attributes);

			} else if ("restore".equals(mode)) {

				restoreSnapshot(attributes);
			}

		} else {

			throw new FrameworkException(500, "No snapshot mode supplied, aborting.");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	// ----- private methods -----
	private void createSnapshot(final Map<String, Object> attributes) throws FrameworkException {

		// we want to create a sorted, human-readble, diffable representation of the schema
		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			final String fileName = attributes.containsKey("name") ? (String)attributes.get("name") : "schema.json";

			try (final Writer writer = new FileWriter(fileName)) {

				final JsonSchema schema = StructrSchema.createFromDatabase(app);

				writer.append(schema.toString());
				writer.append("\n");    // useful newline

				writer.flush();
			}

			tx.success();

		} catch (IOException | URISyntaxException ioex) {
			ioex.printStackTrace();
		}
	}

	private void restoreSnapshot(final Map<String, Object> attributes) throws FrameworkException {

		// we want to create a sorted, human-readble, diffable representation of the schema
		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			final String fileName = (String)attributes.get("name");
			if (fileName != null) {

				try (final Reader reader = new FileReader(fileName)) {

					final JsonSchema schema = StructrSchema.createFromSource(reader);
					StructrSchema.replaceDatabaseSchema(app, schema);

				} catch (InvalidSchemaException iex) {

					throw new FrameworkException(422, iex.getMessage());
				}

			} else {

				throw new FrameworkException(422, "Please supply schema name to import.");
			}

			tx.success();

		} catch (IOException | URISyntaxException ioex) {
			ioex.printStackTrace();
		}
	}
}
