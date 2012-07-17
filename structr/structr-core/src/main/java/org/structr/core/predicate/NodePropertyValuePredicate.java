/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.predicate;

import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;

/**
 *
 * @author Christian Morgner
 */
public class NodePropertyValuePredicate implements Predicate<Node> {

	private String propertyKey = null;
	private Object value = null;
	
	public NodePropertyValuePredicate(String propertyKey, Object value) {
		this.propertyKey = propertyKey;
		this.value = value;
	}
	
	@Override
	public boolean evaluate(SecurityContext securityContext, Node... nodes) {
		
		if(nodes.length > 0) {
			
			Node node = nodes[0];
			
			return node.hasProperty(propertyKey) && node.getProperty(propertyKey).equals(value);
		}
		
		return false;
	}
}
