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
package org.structr.core.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Node;
import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.SchemaGrantTraitWrapper;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public final class SchemaGrantTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SCHEMA_NODE_PROPERTY                = "schemaNode";
	public static final String STATIC_SCHEMA_NODE_NAME_PROPERTY    = "staticSchemaNodeName";
	public static final String PRINCIPAL_PROPERTY                  = "principal";
	public static final String ALLOW_READ_PROPERTY                 = "allowRead";
	public static final String ALLOW_WRITE_PROPERTY                = "allowWrite";
	public static final String ALLOW_DELETE_PROPERTY               = "allowDelete";
	public static final String ALLOW_ACCESS_CONTROL_PROPERTY       = "allowAccessControl";

	private static final Logger logger = LoggerFactory.getLogger(SchemaGrantTraitDefinition.class);

	public SchemaGrantTraitDefinition() {
		super(StructrTraits.SCHEMA_GRANT);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Set<PropertyKey> keys = new LinkedHashSet<>();
					final Traits traits         = obj.getTraits();

					// preserve order (for testing)
					keys.add(traits.key(PRINCIPAL_PROPERTY));
					keys.add(traits.key(SCHEMA_NODE_PROPERTY));
					keys.add(traits.key(STATIC_SCHEMA_NODE_NAME_PROPERTY));

					return ValidationHelper.areValidCompoundUniqueProperties(obj, errorBuffer, keys);
				}
			},

			OnCreation.class, (OnCreation) (graphObject, securityContext, errorBuffer) -> checkRelationshipsAndDeleteSelf(graphObject.as(SchemaGrant.class), true),
			OnModification.class, (OnModification) (graphObject, securityContext, errorBuffer, modificationQueue) -> checkRelationshipsAndDeleteSelf(graphObject.as(SchemaGrant.class), false)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaGrant.class, (traits, node) -> new SchemaGrantTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface>  principal    = new StartNode(traitsInstance, PRINCIPAL_PROPERTY, StructrTraits.PRINCIPAL_SCHEMA_GRANT_RELATIONSHIP);
		final Property<NodeInterface> schemaNode    = new EndNode(traitsInstance, SCHEMA_NODE_PROPERTY, StructrTraits.SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP);
		final Property<String> staticSchemaNodeName = new StringProperty(STATIC_SCHEMA_NODE_NAME_PROPERTY);
		final Property<Boolean> allowRead           = new BooleanProperty(ALLOW_READ_PROPERTY);
		final Property<Boolean> allowWrite          = new BooleanProperty(ALLOW_WRITE_PROPERTY);
		final Property<Boolean> allowDelete         = new BooleanProperty(ALLOW_DELETE_PROPERTY);
		final Property<Boolean> allowAccessControl  = new BooleanProperty(ALLOW_ACCESS_CONTROL_PROPERTY);

		return newSet(
			principal,
			schemaNode,
			staticSchemaNodeName,
			allowAccessControl,
			allowDelete,
			allowRead,
			allowWrite
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					PRINCIPAL_PROPERTY, SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY, ALLOW_READ_PROPERTY, ALLOW_WRITE_PROPERTY, ALLOW_DELETE_PROPERTY, ALLOW_ACCESS_CONTROL_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					PRINCIPAL_PROPERTY, SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY, ALLOW_READ_PROPERTY, ALLOW_WRITE_PROPERTY, ALLOW_DELETE_PROPERTY, ALLOW_ACCESS_CONTROL_PROPERTY
			),

			PropertyView.Schema,
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY,
					PRINCIPAL_PROPERTY, SCHEMA_NODE_PROPERTY, STATIC_SCHEMA_NODE_NAME_PROPERTY, ALLOW_READ_PROPERTY, ALLOW_WRITE_PROPERTY, ALLOW_DELETE_PROPERTY, ALLOW_ACCESS_CONTROL_PROPERTY
			)
		);
	}

	/*
	*/

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private methods -----
	private void checkRelationshipsAndDeleteSelf(final SchemaGrant grant, final boolean isCreation) throws FrameworkException {

		final NodeInterface node = grant;
		final Node dbNode        = node.getNode();
		final Principal p        = grant.getPrincipal();

		if (!TransactionCommand.isDeleted(dbNode)) {

			if (p == null) {

				// no principal => delete
				SchemaGrantTraitDefinition.logger.warn("Deleting SchemaGrant {} because it is not linked to a principal.", grant.getUuid());
				StructrApp.getInstance().delete(node);
			}

			// silently delete this node if all settings are set to false
			if (!isCreation && !grant.allowRead() && !grant.allowWrite() && !grant.allowDelete() && !grant.allowAccessControl()) {

				SchemaGrantTraitDefinition.logger.warn("Deleting SchemaGrant {} because it doesn't allow anything.", grant.getUuid());
				StructrApp.getInstance().delete(node);
			}

			// delete this node if schema node is missing
			if (grant.getSchemaNode() == null) {

				// schema grant can be associated with a static type as well
				final String fqcn = grant.getStaticSchemaNodeName();
				if (fqcn == null) {

					SchemaGrantTraitDefinition.logger.warn("Deleting SchemaGrant {} because it is not linked to a schema node.", node.getUuid());
					StructrApp.getInstance().delete(node);
				}
			}
		}
	}
}
