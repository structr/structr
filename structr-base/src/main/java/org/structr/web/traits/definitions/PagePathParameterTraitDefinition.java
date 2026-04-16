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
package org.structr.web.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.traits.wrappers.PagePathParameterTraitWrapper;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PagePathParameterTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PATH_PROPERTY                    = "path";
	public static final String POSITION_PROPERTY                = "position";
	public static final String VALUE_TYPE_PROPERTY              = "valueType";
	public static final String FORMAT_PROPERTY                  = "format";
	public static final String DEFAULT_VALUE_PROPERTY           = "defaultValue";
	public static final String IS_MANDATORY_PROPERTY            = "isMandatory";
	public static final String USE_DEFAULT_IF_INVALID_PROPERTY  = "useDefaultIfInvalid";

	public PagePathParameterTraitDefinition() {
		super(StructrTraits.PAGE_PATH_PARAMETER);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

				OnCreation.class,
				new OnCreation() {
					@Override
					public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

						final PagePath pagePath = graphObject.as(PagePathParameter.class).getPagePath();
						if (pagePath != null) {

							pagePath.updatePathAndParameters(securityContext, Map.of("path", pagePath.getName()));
						}
					}
				},

				OnModification.class,
				new OnModification() {
					@Override
					public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

						final PagePath pagePath = graphObject.as(PagePathParameter.class).getPagePath();
						if (pagePath != null) {

							pagePath.updatePathAndParameters(securityContext, Map.of("path", pagePath.getName()));
						}
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

		return Map.of(
			PagePathParameter.class, (traits, node) -> new PagePathParameterTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> pathProperty          = new StartNode(traitsInstance, PATH_PROPERTY, StructrTraits.PAGE_PATH_HAS_PARAMETER_PAGE_PATH_PARAMETER);
		final Property<Integer> positionProperty            = new IntProperty(POSITION_PROPERTY).indexed();
		final Property<String> valueTypeProperty            = new EnumProperty(VALUE_TYPE_PROPERTY, PathParameterValueType.class).defaultValue(PathParameterValueType.String.name());
		final Property<String> formatProperty               = new StringProperty(FORMAT_PROPERTY);
		final Property<String> defaultValueProperty         = new StringProperty(DEFAULT_VALUE_PROPERTY);
		final Property<Boolean> isMandatoryProperty         = new BooleanProperty(IS_MANDATORY_PROPERTY);
		final Property<Boolean> useDefaultIfInvalidProperty = new BooleanProperty(USE_DEFAULT_IF_INVALID_PROPERTY);

		return Set.of(
			pathProperty,
			positionProperty,
			valueTypeProperty,
			formatProperty,
			defaultValueProperty,
			isMandatoryProperty,
			useDefaultIfInvalidProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
				PropertyView.Public,
				newSet(POSITION_PROPERTY, VALUE_TYPE_PROPERTY, FORMAT_PROPERTY, DEFAULT_VALUE_PROPERTY, IS_MANDATORY_PROPERTY, USE_DEFAULT_IF_INVALID_PROPERTY),

				PropertyView.Ui,
				newSet(POSITION_PROPERTY, VALUE_TYPE_PROPERTY, FORMAT_PROPERTY, DEFAULT_VALUE_PROPERTY, IS_MANDATORY_PROPERTY, USE_DEFAULT_IF_INVALID_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	public enum PathParameterValueType {

		String, Base64UrlString, Integer, Long, Double, Float, Date, Boolean, Node;

		private static final Map<String, PathParameterValueType> LOOKUP_TBL = Arrays.stream(values()).collect(Collectors.toMap(Enum::name, e -> e));

		public static PathParameterValueType fromString(final String value) {
			return LOOKUP_TBL.get(value);
		}

		public static boolean isValid(final String value) {
			return LOOKUP_TBL.containsKey(value);
		}

		public static boolean hasFormat(final String value) {

			return String.name().equals(value) || Base64UrlString.name().equals(value) || Date.name().equals(value);
		}
	}
}