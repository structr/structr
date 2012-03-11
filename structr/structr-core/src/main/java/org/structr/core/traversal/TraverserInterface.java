/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.traversal;

import java.util.Comparator;
import java.util.List;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public interface TraverserInterface {
	
	public TraversalDescription getTraversalDescription(Object sourceProperty);
	public List transformResult(List<AbstractNode> traversalResult);
	public Comparator<AbstractNode> getComparator();
	public Notion getNotion();
	public void cleanup();
}
