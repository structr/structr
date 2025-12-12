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
package org.structr.docs.formatter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Details;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats the contents of an external markdown source as plaintext,
 * but only the name. This is mainly used to build the navigation index.
 */
public class PlaintextMarkdownFileFormatter extends Formatter {

	private final Resource baseResource;

	public PlaintextMarkdownFileFormatter(final Resource baseResource) {
		this.baseResource = baseResource;
	}

	@Override
	public void format(final List<String> lines, final Concept concept, final OutputSettings settings, final int level) {

		if (settings.getDetails().contains(Details.name)) {

			lines.add(concept.getName());
		}
	}
}
