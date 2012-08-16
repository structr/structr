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
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.RelationshipFactory;

/**
 *
 * @author Christian Morgner
 */

public class RemoteView extends View {

	public enum Key implements PropertyKey {
		repositoryUrl, remoteUser, remotePassword
	};
	
	static {
		
		EntityContext.registerPropertySet(RemoteView.class, PropertyView.Public, Key.values());
		EntityContext.registerPropertySet(RemoteView.class, PropertyView.All,    Key.values());
		EntityContext.registerPropertySet(RemoteView.class, PropertyView.Ui,     Key.values());
	}
	
	@Override
	public List<GraphObject> getGraphObjects(final HttpServletRequest request) {

		try {

			List<GraphObject> resultList = new LinkedList<GraphObject>();
			String query                 = getQuery(request);
			String repositoryUrl         = getStringProperty(Key.repositoryUrl);
			String username              = getStringProperty(Key.remoteUser);
			String password              = getStringProperty(Key.remotePassword);
			
			// fetch factories
			RelationshipFactory relFactory  = new RelationshipFactory(securityContext);
			NodeFactory nodeFactory         = new NodeFactory();
			boolean includeHiddenAndDeleted = false;
			boolean publicOnly              = false;

			// initialize remote cypher engine
			GraphDatabaseService gds        = new RestGraphDatabase(repositoryUrl, username, password);
			ExecutionEngine engine          = new ExecutionEngine(gds);

			// execute cypher query
			ExecutionResult result          = engine.execute(query);
			
			// process result
			for (String column : result.columns()) {

				for (Object o : IteratorUtil.asIterable(result.columnAs(column))) {

					if (o instanceof Node) {

						AbstractNode node = nodeFactory.createNode(securityContext, (Node) o, includeHiddenAndDeleted, publicOnly);

						if (node != null) {

							resultList.add(node);
						}
						
					} else if (o instanceof Relationship) {

						AbstractRelationship rel = relFactory.createRelationship(securityContext, (Relationship) o);

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
