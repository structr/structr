package org.structr.core.entity;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractEndpoint {

	private static final Logger logger = Logger.getLogger(AbstractEndpoint.class.getName());
	
	public Relationship getSingle(final SecurityContext securityContext, final Node dbNode, final RelationshipType relationshipType, final Direction direction, final Class otherNodeType) {
		
		final Iterable<Relationship> rels     = getMultiple(securityContext, dbNode, relationshipType, direction, otherNodeType);
		final Iterator<Relationship> iterator = rels.iterator();
		
		// FIXME: this returns only the first relationship that matches, i.e. there is NO check for multiple relationships
		if (iterator.hasNext()) {
			return iterator.next();
		}
		
		return null;
	}

	public Iterable<Relationship> getMultiple(final SecurityContext securityContext, final Node dbNode, final RelationshipType relationshipType, final Direction direction, final Class otherNodeType) {
		return Iterables.filter(new OtherNodeTypeFilter(securityContext, dbNode, otherNodeType), dbNode.getRelationships(direction, relationshipType));
	}
	
	// ----- protected methods -----
	protected PropertyMap getNotionProperties(final SecurityContext securityContext, final Class type) {
		
		PropertyMap notionProperties = (PropertyMap)securityContext.getAttribute("notionProperties");
		if (notionProperties != null) {
			
			final Set<PropertyKey> keySet = Services.getInstance().getConfigurationProvider().getPropertySet(type, PropertyView.Public);
			for (final Iterator<PropertyKey> it = notionProperties.keySet().iterator(); it.hasNext();) {
				
				final PropertyKey key = it.next();
				if (!keySet.contains(key)) {
					
					it.remove();
				}
			}
		}		
		
		return notionProperties;
	}

	// ----- nested classes -----
	private static final class OtherNodeTypeFilter implements Predicate<Relationship> {

		private NodeFactory nodeFactory = null;
		private Node thisNode           = null;
		private Class desiredType       = null;
		
		public OtherNodeTypeFilter(final SecurityContext securityContext, final Node thisNode, final Class desiredType) {

			this.nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
			this.desiredType = desiredType;
			this.thisNode    = thisNode;
		}
		
		@Override
		public boolean accept(final Relationship item) {
			
			try {
				final NodeInterface otherNode = nodeFactory.instantiate(item.getOtherNode(thisNode));
				
				final Class otherNodeType     = otherNode.getClass();
				
				return desiredType.isAssignableFrom(otherNodeType) || otherNodeType.isAssignableFrom(desiredType);
				
			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
			
			return false;
		}
	}
}
