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
package org.structr.docs.formatter.text;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Link;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

public class RawMarkdownFileFormatter extends Formatter {

	private final Resource baseResource;

	public RawMarkdownFileFormatter(final Resource baseResource) {
		this.baseResource = baseResource;
	}

	@Override
	public boolean format(final List<String> lines, final Link link, final OutputSettings settings, final int level, final Set<Concept> visited) {

		try {

			final Concept concept = link.getTarget();
			final String path       = (String) concept.getMetadata().get("path");
			final String fileName   = StringUtils.substringAfterLast(path, "/");
			final String folderName = StringUtils.substringBeforeLast(path, "/");
			final Resource resource = baseResource.resolve("docs/" + folderName);

			lines.addAll(Files.readAllLines(resource.resolve(fileName).getPath()));

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
}
