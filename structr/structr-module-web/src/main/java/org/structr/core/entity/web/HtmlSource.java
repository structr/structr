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



package org.structr.core.entity.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PlainText;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class HtmlSource extends PlainText {

	static {

		EntityContext.registerPropertySet(HtmlSource.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/html.png";
	}

	/**
	 * If a node has a Page node as ancestor, return it. If not, return null.
	 *
	 * @return
	 */
	public Page getAncestorPage() {

		List<AbstractNode> ancestors = getAncestorNodes();

		for (AbstractNode n : ancestors) {

			if (n instanceof Page) {
				return (Page) n;
			}
		}

		return null;
	}

	@Override
	public Object getPropertyForIndexing(final String key) {

		Object value = getProperty(key);

		if (PlainText.Key.content.name().equals(key)) {

			String stringValue = (String) value;
			Document htmlDoc   = Jsoup.parse(stringValue);

			return htmlDoc.text();
		}

		return getProperty(key);
	}
}
