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
package org.structr.core.traits.definitions;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.schema.ReloadSchema;
import org.structr.schema.action.Actions;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class SchemaReloadingNodeTraitDefinition extends AbstractTraitDefinition {

	public SchemaReloadingNodeTraitDefinition() {
		super("SchemaReloadingNode");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					Actions.clearCache();

					// register transaction post processing that recreates the schema information
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {

					Actions.clearCache();

					// register transaction post processing that recreates the schema information
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnNodeDeletion.class,
			new OnNodeDeletion() {

				@Override
				public void onNodeDeletion(NodeInterface nodeInterface, SecurityContext securityContext) throws FrameworkException {

					Actions.clearCache();

					// register transaction post processing that recreates the schema information
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
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
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	public boolean reloadSchemaOnCreate() {
	}

	public boolean reloadSchemaOnModify(final ModificationQueue modificationQueue) {
	}

	public boolean reloadSchemaOnDelete() {
	}

	public String getResourceSignature() {
		return getProperty(name);
	}

	public String getClassName() {
		return getProperty(name);
	}

	public String getSuperclassName() {

		final SchemaNode superclass = getProperty(SchemaNode.extendsClass);
		if (superclass != null) {

			return superclass.getName();
		}

		return AbstractNode.class.getSimpleName();
	}
	*/
}
