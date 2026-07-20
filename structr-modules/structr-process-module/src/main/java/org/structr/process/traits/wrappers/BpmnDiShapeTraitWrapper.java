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
import org.structr.process.entity.BpmnDiShape;
import org.structr.process.traits.definitions.BpmnDiShapeTraitDefinition;

public class BpmnDiShapeTraitWrapper extends AbstractNodeTraitWrapper implements BpmnDiShape {

	public BpmnDiShapeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getShapeId() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.SHAPE_ID_PROPERTY));
	}

	@Override
	public String getBpmnElementRef() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.BPMN_ELEMENT_REF_PROPERTY));
	}

	@Override
	public Double getBoundsX() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_X_PROPERTY));
	}

	@Override
	public Double getBoundsY() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_Y_PROPERTY));
	}

	@Override
	public Double getBoundsWidth() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_WIDTH_PROPERTY));
	}

	@Override
	public Double getBoundsHeight() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_HEIGHT_PROPERTY));
	}

	@Override
	public boolean isMarkerVisible() {
		return Boolean.TRUE.equals(wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.IS_MARKER_VISIBLE)));
	}

	@Override
	public boolean isExpanded() {
		return Boolean.TRUE.equals(wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.IS_EXPANDED)));
	}

	@Override
	public boolean isHorizontal() {
		return Boolean.TRUE.equals(wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.IS_HORIZONTAL)));
	}

	@Override
	public boolean hasLabel() {
		return Boolean.TRUE.equals(wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.HAS_LABEL_PROPERTY)));
	}

	@Override
	public String getLabelBounds() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.LABEL_BOUNDS_PROPERTY));
	}

	@Override
	public String getDiAttributes() {
		return wrappedObject.getProperty(traits.key(BpmnDiShapeTraitDefinition.DI_ATTRIBUTES_PROPERTY));
	}
}
