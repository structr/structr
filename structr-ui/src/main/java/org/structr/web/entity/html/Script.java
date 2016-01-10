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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.core.property.Property;
import org.apache.commons.lang3.ArrayUtils;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.Content;
import org.w3c.dom.Node;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class Script extends LinkSource {

	private static final Logger logger = Logger.getLogger(Script.class.getName());

	public static final Property<String> _src     = new HtmlProperty("src");
	public static final Property<String> _async   = new HtmlProperty("async");
	public static final Property<String> _defer   = new HtmlProperty("defer");
	public static final Property<String> _type    = new HtmlProperty("type");
	public static final Property<String> _charset = new HtmlProperty("charset");

	public static final View uiView = new View(Script.class, PropertyView.Ui,
		linkableId, linkable
	);

	public static final View htmlView = new View(Script.class, PropertyView.Html,
		_src, _async, _defer, _type, _charset
	);

	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

	@Override
	protected void handleNewChild(final Node newChild) {

		if (newChild instanceof Content) {

			try {
				final String scriptType = getProperty(_type);
				
				if (StringUtils.isNotBlank(scriptType)) {
				
					((Content)newChild).setProperty(Content.contentType, scriptType);
				
				}

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to set property on new child: {0}", fex.getMessage());

			}
		}
	}

}
