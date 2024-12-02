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
package org.structr.web.entity.css;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.web.entity.css.relationship.CssRuleCONTAINSCssRule;
import org.structr.web.entity.css.relationship.CssRuleHAS_SELECTORCssSelector;

public class CssRule extends AbstractNode {

	public static final Property<Iterable<CssRule>> childRulesProperty = new EndNodes<>("childRules", CssRuleCONTAINSCssRule.class).partOfBuiltInSchema();
	public static final Property<CssRule> parentRuleProperty           = new StartNode<>("parentRule", CssRuleCONTAINSCssRule.class).partOfBuiltInSchema();

	public static final Property<Iterable<CssSelector>> selectorsProperty = new EndNodes<>("selectors", CssRuleHAS_SELECTORCssSelector.class).partOfBuiltInSchema();

	public static final Property<String> cssTextProperty    = new StringProperty("cssText").indexed().partOfBuiltInSchema();
	public static final Property<Integer>  ruleTypeProperty = new IntProperty("ruleType").indexed().partOfBuiltInSchema();

	public static final View uiView = new View(CssRule.class, PropertyView.Ui,
		cssTextProperty, ruleTypeProperty, childRulesProperty, parentRuleProperty, selectorsProperty
	);
}