/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.impl.rels;

import org.structr.common.PermissionPropagation;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.flow.impl.FlowCondition;
import org.structr.flow.impl.FlowDecision;

/**
 *
 */
public class FlowDecisionCondition extends OneToMany<FlowCondition, FlowDecision> implements PermissionPropagation {

	@Override
	public Class<FlowCondition> getSourceType() {
		return FlowCondition.class;
	}

	@Override
	public Class<FlowDecision> getTargetType() {
		return FlowDecision.class;
	}

	@Override
	public String name() {
		return "CONDITION";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}

	@Override
	public SchemaRelationshipNode.Direction getPropagationDirection() {
		return SchemaRelationshipNode.Direction.Both;
	}

	@Override
	public SchemaRelationshipNode.Propagation getReadPropagation() {
		return SchemaRelationshipNode.Propagation.Add;
	}

	@Override
	public SchemaRelationshipNode.Propagation getWritePropagation() {
		return SchemaRelationshipNode.Propagation.Keep;
	}

	@Override
	public SchemaRelationshipNode.Propagation getDeletePropagation() {
		return SchemaRelationshipNode.Propagation.Keep;
	}

	@Override
	public SchemaRelationshipNode.Propagation getAccessControlPropagation() {
		return SchemaRelationshipNode.Propagation.Keep;
	}

	@Override
	public String getDeltaProperties() {
		return null;
	}
}
