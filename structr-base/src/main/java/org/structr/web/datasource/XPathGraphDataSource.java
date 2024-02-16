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
package org.structr.web.datasource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.datasources.GraphDataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 */
public class XPathGraphDataSource implements GraphDataSource<Iterable<GraphObject>> {

	private static final Logger logger = LoggerFactory.getLogger(XPathGraphDataSource.class.getName());

	@Override
	public Iterable<GraphObject> getData(final ActionContext renderContext, final NodeInterface referenceNode) throws FrameworkException {

		final String xpathQuery = ((DOMNode) referenceNode).getXpathQuery();
		if (StringUtils.isBlank(xpathQuery)) {

			return null;
		}

		final Document document    = ((DOMNode) referenceNode).getOwnerDocument();
		final XPathFactory factory = XPathFactory.newInstance();
		final XPath xpath          = factory.newXPath();

		try {

			// FIXME: this code works only with absolute xpath queries because
			//        the xpath parser implementation is stupid (comparing object
			//        equality using ==).
			Object result = xpath.evaluate(xpathQuery, document, XPathConstants.NODESET);
			List<GraphObject> results = new ArrayList<>();

			if (result instanceof NodeList) {

				NodeList nodes = (NodeList) result;
				int len = nodes.getLength();

				for (int i = 0; i < len; i++) {

					Node node = nodes.item(i);

					if (node instanceof GraphObject) {

						results.add((GraphObject) node);
					}
				}

			} else if (result instanceof GraphObject) {

				results.add((GraphObject) result);
			}

			return results;

		} catch (Throwable t) {

			logger.warn("Unable to execute xpath query: {}", t.getMessage());
		}

		return Collections.EMPTY_LIST;

	}
}
