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

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;

/**
 *
 */

public class MailTemplate extends AbstractNode {

	public static final Property<String> textProperty = new StringProperty("text").partOfBuiltInSchema();
	public static final Property<String> localeProperty = new StringProperty("locale").indexed().partOfBuiltInSchema();

	public static final View defaultView = new View(MailTemplate.class, PropertyView.Public,
		name, textProperty, localeProperty
	);

	public static final View uiView      = new View(MailTemplate.class, PropertyView.Ui,
		name, textProperty, localeProperty
	);

	static {

		final JsonSchema schema = SchemaService.getDynamicSchema();
		final JsonType type     = schema.addType("MailTemplate");

		type.setExtends(MailTemplate.class);
	}

	public String getText() {
		return getProperty(textProperty);
	}

	public String getLocale() {
		return getProperty(localeProperty);
	}

	public void setLocale(final String locale) throws FrameworkException {
		setProperty(localeProperty, locale);
	}
}
