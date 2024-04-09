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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;

public interface CssRule extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema          = SchemaService.getDynamicSchema();
		final JsonObjectType rule        = schema.addType("CssRule");
		final JsonObjectType declaration = schema.addType("CssDeclaration");
		final JsonObjectType selector    = schema.addType("CssSelector");

		rule.setImplements(URI.create("https://structr.org/v1.1/definitions/CssRule"));
		rule.setCategory("html");

		rule.addStringProperty("cssText",   PropertyView.Ui).setIndexed(true);
		rule.addIntegerProperty("ruleType", PropertyView.Ui).setIndexed(true);

		rule.relate(rule,        "CONTAINS",        Cardinality.OneToMany,  "parentRule", "childRules");
		rule.relate(declaration, "HAS_DECLARATION", Cardinality.OneToMany,  "rule",       "declarations");
		rule.relate(selector,    "HAS_SELECTOR",    Cardinality.ManyToMany, "rules",      "selectors");

		rule.addViewProperty(PropertyView.Ui, "childRules");
		rule.addViewProperty(PropertyView.Ui, "declarations");
		rule.addViewProperty(PropertyView.Ui, "selectors");
	}}
}