/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.w3c.dom.Node;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class Style extends DOMElement {

	private static final Logger logger = LoggerFactory.getLogger(Style.class.getName());

	public static final Property<String> _media  = new HtmlProperty("media");
	public static final Property<String> _type   = new HtmlProperty("type");
	public static final Property<String> _scoped = new HtmlProperty("scoped");

	public static final View htmlView = new View(Style.class, PropertyView.Html,
		_media, _type, _scoped
	);

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.isValid(errorBuffer)) {

			setProperty(Style._type, "text/css");
			return true;
		}

		return false;
	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

	@Override
	protected void handleNewChild(final Node newChild) {

		if (newChild instanceof Content) {

			final Content content = (Content)newChild;

			try {

				final String childContentType = content.getProperty(Content.contentType);
				final String thisContentType  = getProperty(_type);

				if (childContentType == null && thisContentType != null) {

					content.setProperties(securityContext, new PropertyMap(Content.contentType, thisContentType));
				}

			} catch (FrameworkException fex) {

				logger.warn("Unable to set property on new child: {}", fex.getMessage());

			}
		}
	}

}
