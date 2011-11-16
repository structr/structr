/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.filter;

import org.neo4j.graphdb.Node;

/**
 *
 * @author Christian Morgner
 */
public interface Filter {

	public boolean includeInResultSet(Node currentNode);
}
