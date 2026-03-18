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
package org.structr.core.traits.definitions;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.DataAdapterField;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.wrappers.DataAdapterFieldTraitWrapper;
import org.structr.web.entity.dom.DOMNode;

import java.util.Map;
import java.util.Set;

public class DataAdapterFieldTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_ADAPTER_PROPERTY    = "dataAdapter";
	public static final String RENDER_TEMPLATE_PROPERTY = "renderTemplate";
	public static final String EDIT_TEMPLATE_PROPERTY   = "editTemplate";

	public DataAdapterFieldTraitDefinition() {
		super(StructrTraits.DATA_ADAPTER_FIELD);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataAdapterField.class, (traits, node) -> new DataAdapterFieldTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

		return Map.of(
			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> dataAdapterProperty = new StartNode(traitsInstance, DATA_ADAPTER_PROPERTY, StructrTraits.DATA_ADAPTER_HAS_FIELD_DATA_ADAPTER_FIELD).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> renderTemplateProperty     = new StringProperty(RENDER_TEMPLATE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> editTemplateProperty       = new StringProperty(EDIT_TEMPLATE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);

		return newSet(
			dataAdapterProperty,
			renderTemplateProperty,
			editTemplateProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"adapter", newSet(
				GraphObjectTraitDefinition.ID_PROPERTY,
				NodeInterfaceTraitDefinition.NAME_PROPERTY,
				DATA_ADAPTER_PROPERTY,
				RENDER_TEMPLATE_PROPERTY,
				EDIT_TEMPLATE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
