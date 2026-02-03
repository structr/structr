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
package org.structr.docs.documentables.service;

import org.structr.docs.*;
import org.structr.docs.ontology.ConceptType;

import java.util.List;

public class SSHServiceDocumentable extends AbstractServiceDocumentable {

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.Service;
	}

	@Override
	public String getName() {
		return "SSHService";
	}

	@Override
	public String getShortDescription() {
		return "A service that provides access to Structr via SSH.";
	}

	@Override
	public String getLongDescription() {
		return null;
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> concepts = super.getLinkedConcepts();

		concepts.add(Link.to("provides", ConceptReference.of(ConceptType.Topic, "SSH server")));

		return concepts;
	}
}
