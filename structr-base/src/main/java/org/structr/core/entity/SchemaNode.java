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
package org.structr.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TypeInfo;

import java.util.Set;

public interface SchemaNode extends AbstractSchemaNode, TypeInfo {

	String schemaNodeNamePattern = "[A-Z][a-zA-Z0-9_]*";

	String getMultiplicity(final String propertyNameToCheck);
	String getRelatedType(final String propertyNameToCheck);

	boolean defaultVisibleToPublic();
	boolean defaultVisibleToAuth();

	Iterable<SchemaRelationshipNode> getRelatedTo();
	Iterable<SchemaRelationshipNode> getRelatedFrom();

	Set<String> getInheritedTraits();
	void setInheritedTraits(final Set<String> setOfTraits) throws FrameworkException;

	TraitDefinition getTraitDefinition(final TraitsInstance traitsInstance);

	void handleMigration() throws FrameworkException;
}
