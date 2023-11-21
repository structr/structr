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

import org.structr.api.graph.Relationship;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A generic relationship entity that will be instantiated when an anonymous
 * relationship is encountered.
 */
public class GenericRelationship extends ManyToMany<NodeInterface, NodeInterface> {

	public static final SourceId startNodeId = new SourceId("startNodeId");
	public static final TargetId endNodeId   = new TargetId("endNodeId");

	public static final View uiView = new View(GenericRelationship.class, PropertyView.Ui,
		startNodeId, endNodeId, sourceId, targetId
	);

	public GenericRelationship() {}

	public GenericRelationship(final SecurityContext securityContext, final Relationship dbRelationship, final long transactionId) {
		init(securityContext, dbRelationship, GenericRelationship.class, transactionId);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {

		Set<PropertyKey> keys = new LinkedHashSet<>();

		keys.addAll((Set<PropertyKey>) super.getPropertyKeys(propertyView));

		keys.add(startNodeId);
		keys.add(endNodeId);

		if (dbRelationship != null) {

			for (String key : dbRelationship.getPropertyKeys()) {
				keys.add(StructrApp.getConfiguration().getPropertyKeyForDatabaseName(entityType, key));
			}
		}

		return keys;
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {
		return true;
	}

	@Override
	public Class<NodeInterface> getSourceType() {
		return NodeInterface.class;
	}

	@Override
	public Class<NodeInterface> getTargetType() {
		return NodeInterface.class;
	}

	@Override
	public String name() {
		return "GENERIC";
	}

	@Override
	public boolean isInternal() {
		return false;
	}
}
