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
package org.structr.docs.ontology.apireference;

import org.structr.docs.ontology.StructuralConcept;

public class APIReferenceConcept extends StructuralConcept {

	public APIReferenceConcept() {

		super("API Reference");

		addConcept(new KeywordsConcept());
		addConcept(new FunctionsConcept());
		addConcept(new LifecycleMethodsConcept());
		addConcept(new SystemTypesConcept());
		addConcept(new ServicesConcept());
		addConcept(new MaintenanceCommandsConcept());
		addConcept(new SettingsConcept());

		/*
		final Concept r01 = createConcept(references, "Built-in Functions");
		final Concept r02 = createConcept(references, "Configuration Parameters");
		final Concept r03 = createConcept(references, "REST API Endpoints");
		final Concept r04 = createConcept(references, "Type Properties Reference");
		final Concept r05 = createConcept(references, "StructrScript Keywords");
		final Concept r06 = createConcept(references, "Error Codes");
		 */
	}
}
