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
		repositoryUrl, remoteUser, remotePassword
	);
		
	public static final org.structr.common.View publicView = new org.structr.common.View(RemoteView.class, PropertyView.Public,
		repositoryUrl, remoteUser, remotePassword
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
