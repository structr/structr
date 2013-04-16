/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
 * A property that tries to create an {@link Image} from BASE64 encoded data you store with setProperty.
 *
 * @author Christian Morgner
 */
public class ImageDataProperty<T> extends StringProperty {
	
	private KeyAndClass keyAndClass = null;
	
	public ImageDataProperty(String name, KeyAndClass keyAndClass) {
		super(name);
		this.isSystemProperty = true;
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
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
}
