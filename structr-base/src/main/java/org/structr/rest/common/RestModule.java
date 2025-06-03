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
package org.structr.rest.common;

import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.traits.StructrTraits;
import org.structr.module.StructrModule;
import org.structr.rest.traits.definitions.LogEventTraitDefinition;
import org.structr.rest.traits.definitions.LogObjectTraitDefinition;
import org.structr.rest.traits.definitions.LogSubjectTraitDefinition;
import org.structr.rest.traits.relationships.ObjectEventRelationship;
import org.structr.rest.traits.relationships.SubjectEventRelationship;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.util.Set;

/**
 */
public class RestModule implements StructrModule {

	@Override
	public void onLoad() {

		StructrTraits.registerRelationshipType(StructrTraits.OBJECT_EVENT_RELATIONSHIP,  new ObjectEventRelationship());
		StructrTraits.registerRelationshipType(StructrTraits.SUBJECT_EVENT_RELATIONSHIP, new SubjectEventRelationship());

		StructrTraits.registerNodeType(StructrTraits.LOG_EVENT,   new LogEventTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.LOG_OBJECT,  new LogObjectTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.LOG_SUBJECT, new LogSubjectTraitDefinition());
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

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}
}
