/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.css.CssRule;
import org.structr.web.traits.wrappers.CssRuleTraitWrapper;

import java.util.Map;
import java.util.Set;

public class CssRuleTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CHILD_RULES_PROPERTY = "childRules";
	public static final String PARENT_RULE_PROPERTY = "parentRule";
	public static final String SELECTORS_PROPERTY   = "selectors";
	public static final String CSS_TEXT_PROPERTY    = "cssText";
	public static final String RULE_TYPE_PROPERTY   = "ruleType";

	public CssRuleTraitDefinition() {
		super(StructrTraits.CSS_RULE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
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
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> childRulesProperty = new EndNodes(traitsInstance, CHILD_RULES_PROPERTY, StructrTraits.CSS_RULE_CONTAINS_CSS_RULE);
		final Property<NodeInterface> parentRuleProperty           = new StartNode(traitsInstance, PARENT_RULE_PROPERTY, StructrTraits.CSS_RULE_CONTAINS_CSS_RULE);
		final Property<Iterable<NodeInterface>> selectorsProperty  = new EndNodes(traitsInstance, SELECTORS_PROPERTY, StructrTraits.CSS_RULE_HAS_SELECTOR_CSS_SELECTOR);
		final Property<String> cssTextProperty                     = new StringProperty(CSS_TEXT_PROPERTY).indexed();
		final Property<Integer>  ruleTypeProperty                  = new IntProperty(RULE_TYPE_PROPERTY).indexed();

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