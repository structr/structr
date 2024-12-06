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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.definitions.SchemaReloadingNodeTraitDefinition;
import org.structr.core.graph.Tx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveTypesWithNonExistentPackages implements MigrationHandler {

	private static final Pattern PATTERN = Pattern.compile("package .* does not exist.*", Pattern.DOTALL);
	private static final Logger logger   = LoggerFactory.getLogger(RemoveTypesWithNonExistentPackages.class);

	@Override
	public void handleMigration(final ErrorToken errorToken) throws FrameworkException {

		final String type   = errorToken.getType();
		final String token  = errorToken.getToken();
		final String detail = (String)errorToken.getDetail();

		if ("compiler_error".equals(token)) {

			// check error detail
			final Matcher matcher = PATTERN.matcher(detail);
			if (matcher.matches()) {

				logger.warn("Trying to remove incompatible type {}", type);

				try {

					final App app = StructrApp.getInstance();

					try (final Tx tx = app.tx()) {

						final SchemaReloadingNodeTraitDefinition schemaNode = app.nodeQuery("SchemaReloadingNodeTraitDefinition").andName(type).getFirst();
						if (schemaNode != null) {

							app.delete(schemaNode);
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