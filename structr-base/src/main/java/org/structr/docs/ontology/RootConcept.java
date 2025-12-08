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

import org.structr.docs.ontology.adminui.AdminUserInterfaceConcept;
import org.structr.docs.ontology.advancedtopics.AdvancedTopicsConcept;
import org.structr.docs.ontology.apireference.APIReferenceConcept;
import org.structr.docs.ontology.coreconcepts.CoreConcepts;
import org.structr.docs.ontology.introduction.IntroductionConcept;

public class RootConcept extends StructuralConcept {

	RootConcept() {

		super("Structr");

		addConcept(new IntroductionConcept());
		addConcept(new CoreConcepts());

		//addConcept(new BuildingApplications());

		addConcept(new AdminUserInterfaceConcept());
		addConcept(new AdvancedTopicsConcept());

		//addConcept(new TypesAndSchema());
		//addConcept(new DataManagement());
		//addConcept(new AccessControlAndPermissions());
		//addConcept(new DevelopmentAndCustomization());
		//addConcept(new FileAndResourceManagement());
		//addConcept(new APIsAndIntegration());
		//addConcept(new DeploymentAndConfiguration());
		//addConcept(new Internals());

		addConcept(new APIReferenceConcept());

	}
}
