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
package org.structr.core.entity;

import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;

/**
 *
 */

public interface MailTemplate extends NodeInterface {

	static class Impl { static {

		final JsonType type = SchemaService.getDynamicSchema().addType("MailTemplate");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/MailTemplate"));
		type.setCategory("core");

		type.addStringProperty("text",   PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("locale", PropertyView.Public, PropertyView.Ui).setIndexed(true);

		type.addPropertyGetter("text", String.class);
		type.addPropertyGetter("locale", String.class);
		type.addPropertySetter("locale", String.class);

		// view configuration
		type.addViewProperty(PropertyView.Public, "name");

	}}

	String getText();
	String getLocale();
	void setLocale(final String locale) throws FrameworkException;
}
