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
import org.structr.core.graph.FlushCachesCommand;
import org.structr.schema.SchemaService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class BlacklistSchemaNodeWhenMissingPackage implements MigrationHandler {

	private static final Logger logger   = LoggerFactory.getLogger(BlacklistSchemaNodeWhenMissingPackage.class);
	private static final Pattern PATTERN = Pattern.compile("package ([a-zA-Z0-9\\.]+) does not exist");

	@Override
	public void handleMigration(final ErrorToken errorToken) throws FrameworkException {

		final Object messageObject = errorToken.getDetail();
		if (messageObject != null) {

			final String message  = (String)messageObject;
			final Matcher matcher = PATTERN.matcher(message);

			if (matcher.matches()) {

				logger.info("Identified missing package {}, blacklisting entity {}", matcher.group(1), errorToken.getType());

				SchemaService.blacklist(errorToken.getType());
				FlushCachesCommand.flushAll();
			}
		}
	}
}
