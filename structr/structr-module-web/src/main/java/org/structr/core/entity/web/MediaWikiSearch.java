/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.structr.common.CurrentRequest;
import org.structr.common.PropertyKey;
import org.structr.core.TemporaryValue;
import org.structr.core.entity.AbstractNode;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

//~--- JDK imports ------------------------------------------------------------

import java.net.URL;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class MediaWikiSearch extends AbstractNode {

	private static final String CACHED_WIKI_CONTENT = "cached_wiki_content";

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ baseUrl }

	//~--- get methods ----------------------------------------------------

	@Override
	public Iterable<AbstractNode> getDataNodes() {

		// content is cached in servlet context
		ServletContext context       = CurrentRequest.getRequest().getSession().getServletContext();
		List<AbstractNode> dataNodes = null;

		// TODO: synchronization
		if (context != null) {

			TemporaryValue<List<AbstractNode>> value =
				(TemporaryValue<List<AbstractNode>>) context.getAttribute(
				    CACHED_WIKI_CONTENT.concat(this.getIdString()));

//                      if (value == null) {
//
//                              value = new TemporaryValue<List<AbstractNode>>(null, TimeUnit.MINUTES.toMillis(10));
//                              context.setAttribute(CACHED_WIKI_CONTENT.concat(this.getIdString()), value);
//                      }
//
//                      if ((value.getStoredValue() == null) || value.isExpired()) {
			dataNodes = getContentFromSource();

//                      value.refreshStoredValue(ret);
//
//                      } else {
//                      ret = value.getStoredValue();
//                      }
		}

		return dataNodes;
	}

	@Override
	public String getIconSrc() {
		return ("/images/lightbulb.png");
	}

	// ----- private methods ----
	private LinkedList<AbstractNode> getContentFromSource() {

		LinkedList<AbstractNode> nodeList = new LinkedList<AbstractNode>();
		String source                     = getStringProperty(Key.baseUrl);
		String nodePath                   = CurrentRequest.getCurrentNodePath();
		String search                     = null;

		if (nodePath != null) {

			if (nodePath.endsWith("/")) {
				nodePath = nodePath.substring(0, nodePath.length() - 1);
			}

			search = nodePath.substring(nodePath.lastIndexOf("/")+1);
		}

		if ((source != null) && (search != null)) {

			source = source.concat("?action=opensearch&format=xml&search=").concat(search);

//                      CurrentRequest.getRequest().getParameter("search"));    // TODO: generalize "search" keyword into global variable!!
			try {

				URL url                 = new URL(source);
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc            = builder.parse(url.openStream());
				NodeList items          = doc.getElementsByTagName("Item");
				int len                 = items.getLength();

				for (int i = 0; i < len; i++) {
					nodeList.add(new MediaWikiItem(i, items.item(i)));
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return nodeList;
	}
}
