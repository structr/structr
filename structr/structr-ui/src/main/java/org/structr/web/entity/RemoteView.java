package org.structr.web.entity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.RelationshipFactory;
import org.structr.web.entity.html.HtmlElement.UiKey;

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
		EntityContext.registerEntityRelation(RemoteView.class, Page.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(RemoteView.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerSearchablePropertySet(RemoteView.class, NodeIndex.fulltext.name(), UiKey.values());
		EntityContext.registerSearchablePropertySet(RemoteView.class, NodeIndex.keyword.name(), UiKey.values());
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
