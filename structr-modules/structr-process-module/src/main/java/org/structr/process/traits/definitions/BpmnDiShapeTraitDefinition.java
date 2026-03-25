/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

/**
 * Trait definition for BpmnDiShape -- DI shape data for a BPMN element.
 * Maps to &lt;bpmndi:BPMNShape&gt; in BPMN 2.0.2 XML.
 * Stores bounds (x, y, width, height), label bounds, and additional DI attributes.
 */
public class BpmnDiShapeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SHAPE_ID_PROPERTY         = "shapeId";
	public static final String BPMN_ELEMENT_REF_PROPERTY = "bpmnElementRef";
	public static final String BOUNDS_X_PROPERTY         = "boundsX";
	public static final String BOUNDS_Y_PROPERTY         = "boundsY";
	public static final String BOUNDS_WIDTH_PROPERTY     = "boundsWidth";
	public static final String BOUNDS_HEIGHT_PROPERTY    = "boundsHeight";
	public static final String LABEL_BOUNDS_PROPERTY     = "labelBounds";
	public static final String IS_MARKER_VISIBLE         = "isMarkerVisible";
	public static final String IS_EXPANDED               = "isExpanded";
	public static final String IS_HORIZONTAL             = "isHorizontal";
	public static final String HAS_LABEL_PROPERTY        = "hasLabel";
	public static final String DI_ATTRIBUTES_PROPERTY    = "diAttributes";
	public static final String DIAGRAM_PROPERTY          = "diagram";
	public static final String REFERENCES_ELEMENT        = "referencesElement";

	public BpmnDiShapeTraitDefinition() {
		super(StructrTraits.BPMN_DI_SHAPE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> shapeId          = new StringProperty(SHAPE_ID_PROPERTY);
		final Property<String> bpmnElementRef   = new StringProperty(BPMN_ELEMENT_REF_PROPERTY).indexed();
		final Property<Double> boundsX          = new DoubleProperty(BOUNDS_X_PROPERTY);
		final Property<Double> boundsY          = new DoubleProperty(BOUNDS_Y_PROPERTY);
		final Property<Double> boundsWidth      = new DoubleProperty(BOUNDS_WIDTH_PROPERTY);
		final Property<Double> boundsHeight     = new DoubleProperty(BOUNDS_HEIGHT_PROPERTY);
		final Property<String> labelBounds      = new StringProperty(LABEL_BOUNDS_PROPERTY);
		final Property<Boolean> isMarkerVisible = new BooleanProperty(IS_MARKER_VISIBLE);
		final Property<Boolean> isExpanded      = new BooleanProperty(IS_EXPANDED);
		final Property<Boolean> isHorizontal    = new BooleanProperty(IS_HORIZONTAL);
		final Property<Boolean> hasLabel        = new BooleanProperty(HAS_LABEL_PROPERTY);
		final Property<String> diAttributes     = new StringProperty(DI_ATTRIBUTES_PROPERTY);
		final Property<NodeInterface> diagram   = new StartNode(traitsInstance, DIAGRAM_PROPERTY, StructrTraits.BPMN_DI_DIAGRAM_HAS_SHAPE);
		final Property<NodeInterface> refElem   = new EndNode(traitsInstance, REFERENCES_ELEMENT, StructrTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT);

		return newSet(shapeId, bpmnElementRef, boundsX, boundsY, boundsWidth, boundsHeight, labelBounds, hasLabel, isMarkerVisible, isExpanded, isHorizontal, diAttributes, diagram, refElem);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(SHAPE_ID_PROPERTY, BPMN_ELEMENT_REF_PROPERTY, BOUNDS_X_PROPERTY, BOUNDS_Y_PROPERTY, BOUNDS_WIDTH_PROPERTY, BOUNDS_HEIGHT_PROPERTY),
			PropertyView.Ui, newSet(SHAPE_ID_PROPERTY, BPMN_ELEMENT_REF_PROPERTY, BOUNDS_X_PROPERTY, BOUNDS_Y_PROPERTY, BOUNDS_WIDTH_PROPERTY, BOUNDS_HEIGHT_PROPERTY, LABEL_BOUNDS_PROPERTY, IS_MARKER_VISIBLE, IS_EXPANDED, IS_HORIZONTAL, DI_ATTRIBUTES_PROPERTY, DIAGRAM_PROPERTY, REFERENCES_ELEMENT)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
