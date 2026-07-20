/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.BpmnDiEdge;
import org.structr.process.traits.definitions.BpmnDiEdgeTraitDefinition;

public class BpmnDiEdgeTraitWrapper extends AbstractNodeTraitWrapper implements BpmnDiEdge {

	public BpmnDiEdgeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getEdgeId() {
		return wrappedObject.getProperty(traits.key(BpmnDiEdgeTraitDefinition.EDGE_ID_PROPERTY));
	}

	@Override
	public String getBpmnElementRef() {
		return wrappedObject.getProperty(traits.key(BpmnDiEdgeTraitDefinition.BPMN_ELEMENT_REF_PROPERTY));
	}

	@Override
	public String getWaypoints() {
		return wrappedObject.getProperty(traits.key(BpmnDiEdgeTraitDefinition.WAYPOINTS_PROPERTY));
	}

	@Override
	public String getLabelBounds() {
		return wrappedObject.getProperty(traits.key(BpmnDiEdgeTraitDefinition.LABEL_BOUNDS_PROPERTY));
	}

	@Override
	public String getDiAttributes() {
		return wrappedObject.getProperty(traits.key(BpmnDiEdgeTraitDefinition.DI_ATTRIBUTES_PROPERTY));
	}
}
