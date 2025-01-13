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

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.css.CssRule;
import org.structr.web.traits.wrappers.CssRuleTraitWrapper;

import java.util.Map;
import java.util.Set;

public class CssRuleTraitDefinition extends AbstractTraitDefinition {

	/*
	public static final View uiView = new View(CssRule.class, PropertyView.Ui,
		cssTextProperty, ruleTypeProperty, childRulesProperty, parentRuleProperty, selectorsProperty
	);
	*/

	public CssRuleTraitDefinition() {
		super("CssRule");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
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
			CssRule.class, (traits, node) -> new CssRuleTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> childRulesProperty = new EndNodes("childRules", "CssRuleCONTAINSCssRule");
		final Property<NodeInterface> parentRuleProperty           = new StartNode("parentRule", "CssRuleCONTAINSCssRule");
		final Property<Iterable<NodeInterface>> selectorsProperty  = new EndNodes("selectors", "CssRuleHAS_SELECTORCssSelector");
		final Property<String> cssTextProperty                     = new StringProperty("cssText").indexed();
		final Property<Integer>  ruleTypeProperty                  = new IntProperty("ruleType").indexed();

		return Set.of(
			childRulesProperty,
			parentRuleProperty,
			selectorsProperty,
			cssTextProperty,
			ruleTypeProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}