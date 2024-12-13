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
package org.structr.web.traits.definitions;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.web.entity.path.PagePath;
import org.structr.web.traits.wrappers.PagePathTraitWrapper;

import java.util.*;

/**
 *
 */
public class PagePathTraitDefinition extends AbstractTraitDefinition {

	private static Property<NodeInterface> pageProperty                 = new StartNode("page", "PageHAS_PATHPagePath").partOfBuiltInSchema();
	private static Property<Iterable<NodeInterface>> parametersProperty = new EndNodes("parameters", "PagePathHAS_PARAMETERPagePathParameter").partOfBuiltInSchema();
	private static Property<String> nameProperty                        = new StringProperty("name").notNull().partOfBuiltInSchema();
	private static Property<Integer> priorityProperty                   = new IntProperty("priority").partOfBuiltInSchema();

	/*
	public static final View defaultView = new View(PagePath.class, PropertyView.Public,
		nameProperty, priorityProperty, parametersProperty
	);

	public static final View uiView = new View(PagePath.class, PropertyView.Ui,
		nameProperty, priorityProperty, parametersProperty
	);
	*/

	public PagePathTraitDefinition() {
		super("PagePath");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
					return ValidationHelper.isValidPropertyNotNull(obj, Traits.nameProperty(), errorBuffer);
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
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			pageProperty,
			parametersProperty,
			nameProperty,
			priorityProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
