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
package org.structr.rest.common;

import org.structr.api.service.LicenseManager;
import org.structr.core.traits.StructrTraits;
import org.structr.module.StructrModule;
import org.structr.rest.traits.definitions.LogEventTraitDefinition;
import org.structr.rest.traits.definitions.LogObjectTraitDefinition;
import org.structr.rest.traits.definitions.LogSubjectTraitDefinition;
import org.structr.rest.traits.relationships.ObjectEventRelationship;
import org.structr.rest.traits.relationships.SubjectEventRelationship;

import java.util.Set;

/**
 */
public class RestModule implements StructrModule {

	@Override
	public void onLoad() {

		StructrTraits.registerTrait(new ObjectEventRelationship());
		StructrTraits.registerTrait(new SubjectEventRelationship());

		StructrTraits.registerRelationshipType(StructrTraits.OBJECT_EVENT_RELATIONSHIP,  StructrTraits.OBJECT_EVENT_RELATIONSHIP);
		StructrTraits.registerRelationshipType(StructrTraits.SUBJECT_EVENT_RELATIONSHIP, StructrTraits.SUBJECT_EVENT_RELATIONSHIP);

		StructrTraits.registerTrait(new LogEventTraitDefinition());
		StructrTraits.registerTrait(new LogObjectTraitDefinition());
		StructrTraits.registerTrait(new LogSubjectTraitDefinition());

		StructrTraits.registerNodeType(StructrTraits.LOG_EVENT,   StructrTraits.LOG_EVENT);
		StructrTraits.registerNodeType(StructrTraits.LOG_OBJECT,  StructrTraits.LOG_OBJECT);
		StructrTraits.registerNodeType(StructrTraits.LOG_SUBJECT, StructrTraits.LOG_SUBJECT);
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
	}

	@Override
	public String getName() {
		return "rest";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("core");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
