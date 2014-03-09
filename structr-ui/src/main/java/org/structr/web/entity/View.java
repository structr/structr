/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.apache.commons.lang3.StringUtils;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.CypherQueryCommand;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.relationship.DOMChildren;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class View extends DOMElement {

	private static final Logger logger = Logger.getLogger(View.class.getName());
	private DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	
	public static final Property<String> query             = new StringProperty("query").indexed();
	public static final org.structr.common.View uiView     = new org.structr.common.View(View.class, PropertyView.Ui, query);
	public static final org.structr.common.View publicView = new org.structr.common.View(View.class, PropertyView.Public, query);

	//~--- get methods ----------------------------------------------------

	public List<GraphObject> getGraphObjects(final HttpServletRequest request) {

		try {

			return (List<GraphObject>) StructrApp.getInstance(securityContext).command(CypherQueryCommand.class).execute(getQuery(request));

		} catch (Throwable t) {

			t.printStackTrace();

		}

		return Collections.emptyList();

	}

	protected String getQuery(HttpServletRequest request) {

		String rawQuery = getProperty(query);
		String query    = rawQuery;

		if (request != null && query != null) {

			Pattern pattern = Pattern.compile("\\$\\{request.(.*)\\}");
			Matcher matcher = pattern.matcher(rawQuery);

			while (matcher.find()) {

				String key   = matcher.group(1);
				String value = request.getParameter(key);

				if (StringUtils.isNotEmpty(value)) {

					query = StringUtils.replace(rawQuery, "${request." + key + "}", value);
				}

			}

		}

		return query;

	}

//      public static void main(String[] args) throws Exception {
//              
//              String rawQuery = "START n=node:keywordAllNodes(type='Page') MATCH n-[:CONTAINS*]->child WHERE (child.type = 'Content' AND child.content =~ /.*${request.search}.*/) RETURN DISTINCT n";
//              
//              Pattern pattern = Pattern.compile("\\$\\{request.(.*)\\}");
//                      Matcher matcher = pattern.matcher(rawQuery);
//
//                      while (matcher.find()) {
//
//                              String key = matcher.group(1);
//                              
//                              System.out.println("key: " + key);
//
//
//                      }       
//              
//      }
	@Override
	public short getNodeType() {

		return ELEMENT_NODE;

	}

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		double startView = System.nanoTime();
		
		HttpServletRequest request = renderContext.getRequest();

		// fetch query results
		List<GraphObject> results = getGraphObjects(request);
		double endView            = System.nanoTime();

		logger.log(Level.FINE, "Get graph objects for {0} in {1} seconds", new java.lang.Object[] { getUuid(), decimalFormat.format((endView - startView) / 1000000000.0) });

		for (GraphObject result : results) {

			if (result instanceof DOMNode) {

				// recursively render children
				for (DOMChildren rel : ((DOMNode)result).getChildRelationships()) {

					DOMElement subNode = (DOMElement) rel.getTargetNode();

					if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

						subNode.render(securityContext, renderContext, depth + 1);

					}
				}
			}
		}

	}

}
