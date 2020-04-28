/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.schema.export;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.api.schema.JsonSchema;

public class StructrGlobalSchemaMethods {

	private List<Map<String, Object>> globalMethods   = new LinkedList<>();

	void deserialize(final App app) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			for (final SchemaMethod schemaMethod : app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null).sort(SchemaMethod.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				globalMethods.add(entry);

				entry.put("name",                        schemaMethod.getProperty(SchemaMethod.name));
				entry.put("comment",                     schemaMethod.getProperty(SchemaMethod.comment));
				entry.put("source",                      schemaMethod.getProperty(SchemaMethod.source));
				entry.put("virtualFileName",             schemaMethod.getProperty(SchemaMethod.virtualFileName));
				entry.put("visibleToAuthenticatedUsers", schemaMethod.getProperty(SchemaMethod.visibleToAuthenticatedUsers));
				entry.put("visibleToPublicUsers",        schemaMethod.getProperty(SchemaMethod.visibleToPublicUsers));
			}

			tx.success();
		}
	}

	List<Map<String, Object>> serialize() {

		return globalMethods;
	}

	void deserialize(final List<Map<String, Object>> source) {

		globalMethods = source;
	}

	public void createDatabaseSchema(final App app, final JsonSchema.ImportMode importMode) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);

		if (JsonSchema.ImportMode.replace.equals(importMode)) {
			// completely delete all global schema methods and import the methods from file

			for (final SchemaMethod method : app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null).getAsList()) {
				app.delete(method);
			}

			for (final Map<String, Object> entry : globalMethods) {

				app.create(SchemaMethod.class, PropertyMap.inputTypeToJavaType(context, SchemaMethod.class, entry));

			}

		} else if (JsonSchema.ImportMode.extend.equals(importMode)) {
			// import the methods from file and delete pre-existing global schema methods present in the file
			// Note: this can only happen if a complete snapshot is used to extend another database

			for (final Map<String, Object> entry : globalMethods) {

				final PropertyMap schemaMethodProperties = PropertyMap.inputTypeToJavaType(context, SchemaMethod.class, entry);

				for (final SchemaMethod method : app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, null).andName(schemaMethodProperties.get(SchemaMethod.name)).getAsList()) {
					app.delete(method);
				}

				app.create(SchemaMethod.class, schemaMethodProperties);

			}
		}
	}

	public void clear() {
		globalMethods.clear();
	}
}
