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

import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
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

public class StorageConfigurationTraitDefinition extends AbstractTraitDefinition {

	/*
	public static final View uiView = new View(StorageConfiguration.class, PropertyView.Ui,
		nameProperty, providerProperty, entriesProperty
	);
	*/

	public StorageConfigurationTraitDefinition() {
		super("StorageConfiguration");
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
					final PropertyKey nameProperty     = traits.key("name");
					final PropertyKey providerProperty = traits.key("provider");

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

		final Property<Iterable<NodeInterface>> entriesProperty = new EndNodes("entries", "StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry").partOfBuiltInSchema();
		final Property<Iterable<NodeInterface>> foldersProperty = new StartNodes("folders", "AbstractFileCONFIGURED_BYStorageConfiguration").partOfBuiltInSchema();
		final Property<String> nameProperty                     = new StringProperty("name").indexed().unique().notNull().partOfBuiltInSchema();
		final Property<String> providerProperty                 = new StringProperty("provider").indexed().notNull().partOfBuiltInSchema();

		return Set.of(
			entriesProperty,
			foldersProperty,
			nameProperty,
			providerProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}