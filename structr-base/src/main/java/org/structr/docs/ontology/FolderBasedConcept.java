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
package org.structr.docs.ontology;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class FolderBasedConcept extends DocumentationConcept {

	private final String folderPath;

	public FolderBasedConcept(final String name, final String folderPath) {

		super(name);

		this.folderPath = folderPath;
	}

	@Override
	public List<String> getFilteredDocumentationLines(final Set<Details> details, final int level) {

		Path p = Path.of(folderPath);

		System.out.println(p);

		return List.of();
	}
}
