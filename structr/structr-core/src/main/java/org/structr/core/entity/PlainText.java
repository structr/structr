/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

import org.structr.common.PropertyKey;

/**
 * 
 * @author amorgner
 * 
 */
public class PlainText extends AbstractNode {

	public enum Key implements PropertyKey {
		content, contentType, size;
	}
	
    private final static String ICON_SRC = "/images/page_white_text.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public String getContent() {
        return getStringProperty(Key.content.name());
    }

    public void setContent(final String content) {
        setProperty(Key.content.name(), content);
    }

    @Override
    public String getContentType() {
        return getStringProperty(Key.contentType.name());
    }

    public void setContentType(final String contentType) {
        setProperty(Key.contentType.name(), contentType);
    }

    public String getSize() {
        return getStringProperty(Key.size.name());
    }

    public void setSize(final String size) {
        setProperty(Key.size.name(), size);
    }
}
