/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.importer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.InvalidSchemaException;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.util.StreamUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * This class can handle Schema JSON documents
 */
public class SchemaJsonImporter {

	public static void importSchemaJson(final String source) throws FrameworkException {

		// nothing to do
		if (StringUtils.isBlank(source)) {
			return;
		}

		final App app = StructrApp.getInstance();

		// isolate write output
		try (final Tx tx = app.tx()) {

			final JsonSchema schema;

			try {
				schema = StructrSchema.createFromSource(source);

			} catch (InvalidSchemaException | URISyntaxException ex) {
				throw new FrameworkException(422, ex.getMessage());
			}

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}
	}
}
