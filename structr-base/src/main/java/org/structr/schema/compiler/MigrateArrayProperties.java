/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.schema.compiler;

import org.slf4j.LoggerFactory;
import org.structr.common.error.DiagnosticErrorToken;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;

/**
 */
public class MigrateArrayProperties implements MigrationHandler {

	@Override
	public void handleMigration(final ErrorToken rawErrorToken) throws FrameworkException {

		if (rawErrorToken instanceof DiagnosticErrorToken errorToken) {

			final String detail  = errorToken.getDetail().toString();
			final boolean error1 = detail.startsWith("no suitable method found for setProperty(org.structr.core.property.Property<java.util.List<java.lang.String>>,java.lang.String[])");
			final boolean error2 = detail.endsWith("return type java.lang.String[] is not compatible with java.util.List<java.lang.String>");

			if (error1 || error2) {

				final String typeName   = errorToken.getType().substring(1, errorToken.getType().length() - 5);
				final String methodName = error1 ? errorToken.getNodeName() : detail.substring(0, detail.indexOf(" ") - 2);
				final App app           = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					final SchemaNode type = app.nodeQuery(SchemaNode.class).andName(typeName).getFirst();
					if (type != null) {


						for (final SchemaMethod method : app.nodeQuery(SchemaMethod.class).and(SchemaMethod.schemaNode, type).and(SchemaMethod.name, methodName).getResultStream()) {

							boolean deleteMethod = false;

							final String returnType = method.getProperty(SchemaMethod.returnType);
							if ("String[]".equals(returnType)) {

								// only delete method with wrong return type
								deleteMethod = true;

							} else {

								for (final SchemaMethodParameter param : method.getProperty(SchemaMethod.parameters)) {

									if ("String[]".equals(param.getProperty(SchemaMethodParameter.parameterType))) {

										deleteMethod = true;
									}

								}
							}

							if (deleteMethod || error1) {

								LoggerFactory.getLogger(MigrateArrayProperties.class).info("Removing {}.{} to migrate from array to list-based properties.", typeName, methodName);
								app.delete(method);
							}
						}
					}

					tx.success();
				}
			}
		}
	}
}
