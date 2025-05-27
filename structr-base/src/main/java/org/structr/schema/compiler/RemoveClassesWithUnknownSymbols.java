/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.DiagnosticErrorToken;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A migration handler that removes schema nodes for classes that have
 * a symbol defined that is unknown in the new schema definition.
 */
public class RemoveClassesWithUnknownSymbols implements MigrationHandler {

	private static final Pattern PATTERN = Pattern.compile("cannot find symbol.*", Pattern.DOTALL);
	private static final Logger logger   = LoggerFactory.getLogger(RemoveClassesWithUnknownSymbols.class);

	@Override
	public void handleMigration(final ErrorToken errorToken) throws FrameworkException {

		final String type   = errorToken.getType();
		final String token  = errorToken.getToken();
		final String detail = (String)errorToken.getDetail();

		if ("compiler_error".equals(token)) {

			// check error detail
			final Matcher matcher = PATTERN.matcher(detail);
			if (matcher.matches()) {

				logger.warn(errorToken.toJSON().toString());

				try {

					final App app = StructrApp.getInstance();

					try (final Tx tx = app.tx()) {

						boolean handled = false;

						if (errorToken instanceof DiagnosticErrorToken) {

							final String nodeUuid = ((DiagnosticErrorToken)errorToken).getNodeUuid();

							if (nodeUuid != null) {

								final NodeInterface schemaNode = app.getNodeById(nodeUuid);

								if (schemaNode != null) {

									logger.info("Removing {} named '{}'", schemaNode.getType(), schemaNode.getName());
									app.delete(schemaNode);

									handled = true;
								}
							}
						}

						if (handled == false) {

							final NodeInterface schemaNode = app.nodeQuery(StructrTraits.SCHEMA_RELOADING_NODE).name(type).getFirst();
							if (schemaNode != null) {

								app.delete(schemaNode);
							}
						}

						tx.success();

					} catch (FrameworkException fex) {
						logger.warn("Unable to correct schema compilation error: {}", fex.getMessage());
					}

				} catch (ArrayIndexOutOfBoundsException ibex) {
					logger.warn("Unable to extract error information from {}: {}", detail, ibex.getMessage());
				}
			}
		}
	}
}