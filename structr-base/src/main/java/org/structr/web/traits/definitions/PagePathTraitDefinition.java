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
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.entity.path.PagePath;
import org.structr.web.traits.wrappers.PagePathTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PagePathTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PAGE_PROPERTY       = "page";
	public static final String PARAMETERS_PROPERTY = "parameters";
	public static final String NAME_PROPERTY       = "name";
	public static final String PRIORITY_PROPERTY   = "priority";

	public PagePathTraitDefinition() {
		super(StructrTraits.PAGE_PATH);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
					return ValidationHelper.isValidPropertyNotNull(obj, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), errorBuffer);
				}
			}
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(
			new JavaMethod("updatePathAndParameters", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(PagePath.class).updatePathAndParameters(securityContext, arguments.toMap());
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

			PagePath.class, (traits, node) -> new PagePathTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> pageProperty                 = new StartNode(traitsInstance, PAGE_PROPERTY, StructrTraits.PAGE_HAS_PATH_PAGE_PATH);
		final Property<Iterable<NodeInterface>> parametersProperty = new EndNodes(traitsInstance, PARAMETERS_PROPERTY, StructrTraits.PAGE_PATH_HAS_PARAMETER_PAGE_PATH_PARAMETER);
		final Property<String> nameProperty                        = new StringProperty(NAME_PROPERTY).notNull();		// Custom name property because we need a not null constraint on the name.
		final Property<Integer> priorityProperty                   = new IntProperty(PRIORITY_PROPERTY);

		return Set.of(
			pageProperty,
			parametersProperty,
			nameProperty,
			priorityProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(PRIORITY_PROPERTY, PARAMETERS_PROPERTY),

			PropertyView.Ui,
			newSet(PRIORITY_PROPERTY, PARAMETERS_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
