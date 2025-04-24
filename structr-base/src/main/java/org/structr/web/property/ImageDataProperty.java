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
package org.structr.web.property;

import org.structr.common.KeyAndClass;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.StringProperty;
import org.structr.web.converter.ImageConverter;
import org.structr.web.entity.Image;

/**
 * A property that tries to create an {@link Image} from BASE64 encoded data.
 *
 * This class has two constructors:
 * The default constructor will store the image data as the image itself,
 * the other one (with a {@link KeyAndClass} argumnents stores the data with
 * setProperty as an object of the given class
 *
 *
 *
 */
public class ImageDataProperty<T> extends StringProperty {

	private KeyAndClass keyAndClass = null;

	public ImageDataProperty(String name) {
		super(name);
		this.unvalidated = true;
	}

	public ImageDataProperty(String name, KeyAndClass keyAndClass) {
		super(name);
		this.unvalidated = true;
		this.keyAndClass = keyAndClass;
	}

	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new ImageConverter(securityContext, entity, keyAndClass);
	}

	@Override
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext, boolean fromString) {
		return null;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
}
