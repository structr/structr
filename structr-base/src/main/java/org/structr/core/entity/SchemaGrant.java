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
package org.structr.core.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.PrincipalSchemaGrantRelationship;
import org.structr.core.entity.relationship.SchemaGrantSchemaNodeRelationship;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;

/**
 *
 *
 */
public class SchemaGrant extends SchemaReloadingNode {

	private static final Logger logger                              = LoggerFactory.getLogger(SchemaGrant.class.getName());
	public static final Property<Principal>  principal              = new StartNode<>("principal", PrincipalSchemaGrantRelationship.class);
	public static final Property<SchemaNode> schemaNode             = new EndNode<>("schemaNode", SchemaGrantSchemaNodeRelationship.class);
	public static final Property<Boolean> allowRead                 = new BooleanProperty("allowRead");
	public static final Property<Boolean> allowWrite                = new BooleanProperty("allowWrite");
	public static final Property<Boolean> allowDelete               = new BooleanProperty("allowDelete");
	public static final Property<Boolean> allowAccessControl        = new BooleanProperty("allowAccessControl");

	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		principal, schemaNode, allowRead, allowWrite, allowDelete, allowAccessControl
	);

	public static final View uiView = new View(SchemaNode.class, PropertyView.Ui,
		principal, schemaNode, allowRead, allowWrite, allowDelete, allowAccessControl
	);

	public static final View schemaView = new View(SchemaNode.class, "schema",
		id, principal, schemaNode, allowRead, allowWrite, allowDelete, allowAccessControl
	);

	public static final View exportView = new View(SchemaNode.class, "export",
		principal, schemaNode, allowRead, allowWrite, allowDelete, allowAccessControl
	);

	public String getPrincipalId() {

		// When Structr starts, the schema is not yet compiled and the Principal class does not yet
		// exist, so we only get GenericNode instances. Relationships are filtered by node type, and
		// that filter removes GenericNode instances, so we must use the unfiltered relationship
		// collection and check manually.

		for (final AbstractRelationship rel : getIncomingRelationships()) {

			if (rel != null && rel instanceof PrincipalSchemaGrantRelationship) {

				return rel.getSourceNodeId();
			}
		}

		return null;
	}

	public String getPrincipalName() {

		// When Structr starts, the schema is not yet compiled and the Principal class does not yet
		// exist, so we only get GenericNode instances. Relationships are filtered by node type, and
		// that filter removes GenericNode instances, so we must use the unfiltered relationship
		// collection and check manually.

		for (final AbstractRelationship rel : getIncomingRelationships()) {

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
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		// only one SchemaGrant may exist for a given principal/schemaNode pair
		valid &= ValidationHelper.areValidCompoundUniqueProperties(this, errorBuffer, principal, schemaNode);

		return valid;
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		final Principal p = getProperty(principal);
		if (p == null) {

			// no principal => delete
			logger.warn("Deleting SchemaGrant {} because it is not linked to a principal.", getUuid());
			StructrApp.getInstance().delete(this);
		}

		// delete this node if principal or schema node are missing
		if (!TransactionCommand.isDeleted(dbNode) && getProperty(schemaNode) == null) {
			logger.warn("Deleting SchemaGrant {} because it is not linked to a schema node.", getUuid());
			StructrApp.getInstance().delete(this);
		}
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		final Principal p = getProperty(principal);
		if (p == null) {

			// no principal => delete
			logger.warn("Deleting SchemaGrant {} because it is not linked to a principal.", getUuid());
			StructrApp.getInstance().delete(this);
		}

		// delete this node if principal or schema node are missing
		if (!TransactionCommand.isDeleted(dbNode) && getProperty(schemaNode) == null) {
			logger.warn("Deleting SchemaGrant {} because it is not linked to a schema node.", getUuid());
			StructrApp.getInstance().delete(this);
		}

		// silently delete this node if all settings are set to false
		if (!TransactionCommand.isDeleted(dbNode) && (!getProperty(allowRead) && !getProperty(allowWrite) && !getProperty(allowDelete) && !getProperty(allowAccessControl))) {
			StructrApp.getInstance().delete(this);
		}
	}

	@Override
	public boolean reloadSchemaOnCreate() {
		return true;
	}

	@Override
	public boolean reloadSchemaOnModify(ModificationQueue modificationQueue) {
		return true;
	}

	@Override
	public boolean reloadSchemaOnDelete() {
		return true;
	}
}
