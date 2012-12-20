/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity;

import org.structr.core.property.Property;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.StringProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class PlainText extends AbstractNode {

	public static final Property<String> content     = new StringProperty("content");
	public static final Property<String> contentType = new StringProperty("contentType");
	public static final Property<String> size        = new StringProperty("size");

	static {

//		EntityContext.registerPropertySet(PlainText.class, PropertyView.All, Key.values());
	}

	//~--- constant enums -------------------------------------------------

	
	//~--- get methods ----------------------------------------------------

	public String getContent() {

		return getProperty(PlainText.content);

	}

	public String getContentType() {

		return getProperty(PlainText.contentType);

	}

	public String getSize() {

		return getProperty(PlainText.size);

	}

	//~--- set methods ----------------------------------------------------

	public void setContent(final String content) throws FrameworkException {

		setProperty(PlainText.content, content);

	}

	public void setContentType(final String contentType) throws FrameworkException {

		setProperty(PlainText.contentType, contentType);

	}

	public void setSize(final String size) throws FrameworkException {

		setProperty(PlainText.size, size);

	}

}
