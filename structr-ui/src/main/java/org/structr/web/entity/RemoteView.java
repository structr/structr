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
package org.structr.web.entity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.core.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.RelationshipFactory;

/**
 *
 * @author Christian Morgner
 */

public class RemoteView extends View {

	public static final Property<String>            repositoryUrl  = new StringProperty("repositoryUrl");
	public static final Property<String>            remoteUser     = new StringProperty("remoteUser");
	public static final Property<String>            remotePassword = new StringProperty("remotePassword");

	public static final org.structr.common.View uiView = new org.structr.common.View(RemoteView.class, PropertyView.Ui,
		type, repositoryUrl, remoteUser, remotePassword
	);
		
	public static final org.structr.common.View publicView = new org.structr.common.View(RemoteView.class, PropertyView.Public,
		type, repositoryUrl, remoteUser, remotePassword
	);
		
		
	static {
		
		EntityContext.registerSearchablePropertySet(RemoteView.class, NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(RemoteView.class, NodeIndex.keyword.name(),  uiView.properties());
	}
	
	@Override
	public List<GraphObject> getGraphObjects(final HttpServletRequest request) {

		try {

			List<GraphObject> resultList = new LinkedList<GraphObject>();
			String query                 = getQuery(request);
			String repositoryUrl         = getProperty(RemoteView.repositoryUrl);
			String username              = getProperty(RemoteView.remoteUser);
			String password              = getProperty(RemoteView.remotePassword);
			
			// fetch factories
			RelationshipFactory relFactory  = new RelationshipFactory(securityContext);
			NodeFactory nodeFactory         = new NodeFactory(securityContext);

			// initialize remote cypher engine
			GraphDatabaseService gds        = new RestGraphDatabase(repositoryUrl, username, password);
			ExecutionEngine engine          = new ExecutionEngine(gds);

			// execute cypher query
			ExecutionResult result          = engine.execute(query);
			
			// process result
			for (String column : result.columns()) {

				for (Object o : IteratorUtil.asIterable(result.columnAs(column))) {

					if (o instanceof Node) {

						AbstractNode node = nodeFactory.createNode((Node) o);

						if (node != null) {

							resultList.add(node);
						}
						
					} else if (o instanceof Relationship) {

						AbstractRelationship rel = relFactory.instantiateRelationship(securityContext, (Relationship) o);

						if (rel != null) {

							resultList.add(rel);
						}

					}

				}

			}

			// shutdown rest database connection
			gds.shutdown();
			
			return resultList;
			
		} catch (Throwable t) {

			t.printStackTrace();

		}

		return Collections.emptyList();
	}
}
