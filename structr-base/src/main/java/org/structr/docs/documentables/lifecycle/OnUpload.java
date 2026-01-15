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
package org.structr.docs.documentables.lifecycle;

import org.structr.docs.ontology.ConceptType;

import java.util.List;

public class OnUpload extends LifecycleBase {

	public OnUpload() {
		super("onUpload");
	}

	@Override
	public String getShortDescription() {
		return "Called after the upload of a File is complete.";
	}

	@Override
	public String getLongDescription() {
		return "The `onUpload()` lifecycle method is called after a File is uploaded.";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"This method must be defined on the type `File` or its descendants.",
			"See also: `onDownload()`."
		);
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> linkedConcepts = super.getLinkedConcepts();

		linkedConcepts.add(Link.to("ispartof", ConceptReference.of(ConceptType.SystemType, "File")));

		return linkedConcepts;
	}
}
