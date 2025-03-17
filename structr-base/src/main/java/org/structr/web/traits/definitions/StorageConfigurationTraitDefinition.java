/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.wrappers.StorageConfigurationTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Storage container for mount configuration entries.
 */

public class StorageConfigurationTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String ENTRIES_PROPERTY  = "entries";
	public static final String FOLDERS_PROPERTY  = "folders";
	public static final String NAME_PROPERTY     = "name";
	public static final String PROVIDER_PROPERTY = "provider";

	public StorageConfigurationTraitDefinition() {
		super(StructrTraits.STORAGE_CONFIGURATION);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					boolean valid = true;

					final Traits traits                = obj.getTraits();
					final PropertyKey nameProperty     = traits.key(NAME_PROPERTY);
					final PropertyKey providerProperty = traits.key(PROVIDER_PROPERTY);

					valid &= ValidationHelper.isValidPropertyNotNull(obj, nameProperty, errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj,  nameProperty, errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(obj, providerProperty, errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(

			StorageConfiguration.class, (traits, node) -> new StorageConfigurationTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> entriesProperty = new EndNodes(ENTRIES_PROPERTY, StructrTraits.STORAGE_CONFIGURATION_CONFIG_ENTRY_STORAGE_CONFIGURATION_ENTRY);
		final Property<Iterable<NodeInterface>> foldersProperty = new StartNodes(FOLDERS_PROPERTY, StructrTraits.ABSTRACT_FILE_CONFIGURED_BY_STORAGE_CONFIGURATION);
		final Property<String> nameProperty                     = new StringProperty(NAME_PROPERTY).indexed().unique().notNull();
		final Property<String> providerProperty                 = new StringProperty(PROVIDER_PROPERTY).indexed().notNull();

		return Set.of(
			entriesProperty,
			foldersProperty,
			nameProperty,
			providerProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
				PropertyView.Ui,
				newSet(NAME_PROPERTY, PROVIDER_PROPERTY, ENTRIES_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}