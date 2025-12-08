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
package org.structr.docs.ontology.coreconcepts;

import org.structr.docs.ontology.StructuralConcept;

public class CoreConcepts extends StructuralConcept {

	public CoreConcepts() {

		super("CoreConcepts");

		addConcept(new DatabaseConcept());
		addConcept(new WebStackConcept());
		addConcept(new FileSystemConcept());
		addConcept(new MiddlewareConcept());
		addConcept(new SecurityConcept());

		/*
		final Concept cc01 = createConcept(coreConcepts, "Node & Relationship Model");
		final Concept cc02 = createConcept(coreConcepts, "Type System Overview");
		final Concept cc03 = createConcept(coreConcepts, "Inheritance");
		final Concept cc04 = createConcept(coreConcepts, "Lifecycle & Transactions");
		final Concept cc05 = createConcept(coreConcepts, "Structr Architecture");
		 */
	}
}
