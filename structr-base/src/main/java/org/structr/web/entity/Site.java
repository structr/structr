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
package org.structr.web.entity;

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;

public interface Site extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Site");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Site"));
		type.setCategory("ui");

		type.addStringProperty("hostname", PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addIntegerProperty("port",    PropertyView.Public, PropertyView.Ui).setIndexed(true);

		type.addPropertyGetter("hostname", String.class);
		type.addPropertyGetter("port",     Integer.class);

		// view configuration
		type.addViewProperty(PropertyView.Ui, "pages");

		type.addViewProperty(PropertyView.Public, "pages");
		type.addViewProperty(PropertyView.Public, "name");
	}}

	String getHostname();
	Integer getPort();
}
