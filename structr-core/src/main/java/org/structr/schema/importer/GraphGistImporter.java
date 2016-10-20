/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MaintenanceCommand;

/**
 *
 *
 */
public class GraphGistImporter extends SchemaImporter implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(GraphGistImporter.class.getName());

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String fileName = (String)attributes.get("file");
		final String source   = (String)attributes.get("source");
		final String url      = (String)attributes.get("url");

		if (fileName == null && source == null && url == null) {
			throw new FrameworkException(422, "Please supply file, url or source parameter.");
		}

		if (fileName != null && source != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}

		if (fileName != null && url != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}

		if (url != null && source != null) {
			throw new FrameworkException(422, "Please supply only one of file, url or source.");
		}

		try {

			if (fileName != null) {

				importCypher(extractSources(new FileInputStream(fileName)));

			} else if (url != null) {

				importCypher(extractSources(new URL(url).openStream()));

			} else if (source != null) {

				importCypher(extractSources(new ByteArrayInputStream(source.getBytes())));
			}

		} catch (IOException ioex) {
			//iologger.warn("", ex);
			logger.debug("Filename: " + fileName + ", URL: " + url + ", source: " + source, ioex);
		}

		analyzeSchema();
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
