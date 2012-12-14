/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity;

import org.apache.commons.lang.StringUtils;


import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CypherQueryCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.graph.NodeService.NodeIndex;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class View extends AbstractNode implements Element {

	public static final Property<String> query = new StringProperty("query");
	
	public static final org.structr.common.View uiView = new org.structr.common.View(View.class, PropertyView.Ui,
		type, name, query, paths
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(View.class, PropertyView.Public,
		type, name, query, paths
	);
	
	static {
			
		EntityContext.registerSearchablePropertySet(View.class, NodeIndex.fulltext.name(), publicView.properties());
		EntityContext.registerSearchablePropertySet(View.class, NodeIndex.keyword.name(),  publicView.properties());

	}

	//~--- get methods ----------------------------------------------------

	public List<GraphObject> getGraphObjects(final HttpServletRequest request) {

		try {

			return (List<GraphObject>) Services.command(securityContext, CypherQueryCommand.class).execute(getQuery(request));
			
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

				String key = matcher.group(1);

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

}
