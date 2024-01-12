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
package org.structr.web.entity.path;

import java.net.URI;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.web.entity.dom.Page;

/**
 *
 */
public interface PagePath extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type  = schema.addType("PagePath");
		final JsonObjectType param = schema.addType("PagePathParameter");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/PagePath"));

		type.relate(param, "HAS_PARAMETER", Cardinality.OneToMany, "path", "parameters");

		type.addViewProperty(PropertyView.Public, "parameters");
		type.addViewProperty(PropertyView.Ui,     "parameters");

		type.addPropertyGetter("page",       Page.class);
		type.addPropertyGetter("parameters", Iterable.class);
	}}

	// implemented by method created with addPropertyGetter above
	public Page getPage();
	public Iterable<PagePathParameter> getParameters();
}
