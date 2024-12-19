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

import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.wrappers.SchemaGrantTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public final class SchemaGrantTraitDefinition extends AbstractTraitDefinition {

	private static final Property<NodeInterface>  principal    = new StartNode("principal", "PrincipalSchemaGrantRelationship").partOfBuiltInSchema();
	private static final Property<NodeInterface> schemaNode    = new EndNode("schemaNode", "SchemaGrantSchemaNodeRelationship").partOfBuiltInSchema();
	private static final Property<String> staticSchemaNodeName = new StringProperty("staticSchemaNodeName").partOfBuiltInSchema();
	private static final Property<Boolean> allowRead           = new BooleanProperty("allowRead").partOfBuiltInSchema();
	private static final Property<Boolean> allowWrite          = new BooleanProperty("allowWrite").partOfBuiltInSchema();
	private static final Property<Boolean> allowDelete         = new BooleanProperty("allowDelete").partOfBuiltInSchema();
	private static final Property<Boolean> allowAccessControl  = new BooleanProperty("allowAccessControl").partOfBuiltInSchema();

	/*
	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		principal, schemaNode, staticSchemaNodeName, allowRead, allowWrite, allowDelete, allowAccessControl
	);

	public static final View uiView = new View(SchemaNode.class, PropertyView.Ui,
		principal, schemaNode, staticSchemaNodeName, allowRead, allowWrite, allowDelete, allowAccessControl
	);

	public static final View schemaView = new View(SchemaNode.class, "schema",
		id, principal, schemaNode, staticSchemaNodeName, allowRead, allowWrite, allowDelete, allowAccessControl
	);

	public static final View exportView = new View(SchemaNode.class, "export",
		principal, schemaNode, staticSchemaNodeName, allowRead, allowWrite, allowDelete, allowAccessControl
	);
	*/

	public SchemaGrantTraitDefinition() {
		super("SchemaGrant");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					return ValidationHelper.areValidCompoundUniqueProperties(obj, errorBuffer, principal, schemaNode, staticSchemaNodeName);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaGrant.class, (traits, node) -> new SchemaGrantTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			principal,
			schemaNode,
			staticSchemaNodeName,
			allowAccessControl,
			allowDelete,
			allowRead,
			allowWrite
		);
	}

	/*
	@Override
	public String getPrincipalId() {

		// When Structr starts, the schema is not yet compiled and the Principal class does not yet
		// exist, so we only get GenericNode instances. Relationships are filtered by node type, and
		// that filter removes GenericNode instances, so we must use the unfiltered relationship
		// collection and check manually.

		for (final RelationshipInterface rel : getIncomingRelationships()) {

			if (rel != null && rel instanceof PrincipalSchemaGrantRelationship) {

				return rel.getSourceNodeId();
			}
		}

		return null;
	}

	@Override
	public String getPrincipalName() {

		// When Structr starts, the schema is not yet compiled and the Principal class does not yet
		// exist, so we only get GenericNode instances. Relationships are filtered by node type, and
		// that filter removes GenericNode instances, so we must use the unfiltered relationship
		// collection and check manually.

		for (final RelationshipInterface rel : getIncomingRelationships()) {

			if (rel != null && rel instanceof PrincipalSchemaGrantRelationship) {

				final NodeInterface _principal = rel.getSourceNode();
				if (_principal != null) {

					return _principal.getProperty(AbstractNode.name);
				}
			}
		}

		return null;
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		createDynamicTypeOrDeleteSelf(true);
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		createDynamicTypeOrDeleteSelf(false);
	}

	// ----- private methods -----
	default void createDynamicTypeOrDeleteSelf(boolean allowAllFalse) throws FrameworkException {

		final Node dbNode = getNode();
		final Principal p = getProperty(SchemaGrantTraitDefinition.principal);
		if (p == null) {

			// no principal => delete
			SchemaGrantTraitDefinition.logger.warn("Deleting SchemaGrant {} because it is not linked to a principal.", getUuid());
			StructrApp.getInstance().delete(this);
		}


		// silently delete this node if all settings are set to false
		if (!TransactionCommand.isDeleted(dbNode) && (!getProperty(allowRead) && !getProperty(allowWrite) && !getProperty(allowDelete) && !getProperty(allowAccessControl))) {

			if (!allowAllFalse) {

				StructrApp.getInstance().delete(this);

			} else {

				// delete this node if principal or schema node are missing
				if (!TransactionCommand.isDeleted(dbNode) && getProperty(SchemaGrantTraitDefinition.schemaNode) == null) {

					// schema grant can be associated with a static type as well
					if (getProperty(SchemaGrantTraitDefinition.staticSchemaNodeName) != null) {

						final String fqcn = getProperty(SchemaGrant.staticSchemaNodeName);
						if (fqcn != null) {

							SchemaGrantTraitDefinition.logger.info("Creating dynamic schema node for {}", fqcn);
							setProperty(SchemaGrant.schemaNode, SchemaHelper.getOrCreateDynamicSchemaNodeForFQCN(fqcn));
						}

					} else {

						SchemaGrantTraitDefinition.logger.warn("Deleting SchemaGrant {} because it is not linked to a schema node.", getUuid());
						StructrApp.getInstance().delete(this);
					}
				}
			}
		}
	}
	*/
}
