/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.predicate;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class TypePredicate implements Predicate<Node> {

	private static final Logger logger = Logger.getLogger(TypePredicate.class.getName());
	private String type = null;

	public TypePredicate(String type) {
		this.type = type;
	}

	@Override
	public boolean evaluate(SecurityContext securityContext, Node... nodes) {
		
		if(nodes.length > 0) {

			Node node = nodes[0];
			
			if(node.hasProperty(AbstractNode.Key.type.name())) {

				String value = (String)node.getProperty(AbstractNode.Key.type.name());

				logger.log(Level.FINEST, "Type property: {0}, expected {1}", new Object[] { value, type } );

				return type.equals(value);

			} else {

				logger.log(Level.WARNING, "Node has no type property.");
			}
		}

		return false;
	}

}
