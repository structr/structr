/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import java.util.Comparator;
import org.structr.common.GraphObjectComparator;
import org.structr.core.entity.AbstractNode;

/**
 * An adapter for AbstractNode to GraphObject.
 * 
 * @author Christian Morgner
 */
public class AbstractNodePropertyComparator implements Comparator<AbstractNode> {

	private GraphObjectComparator comparator = null;
	
	public AbstractNodePropertyComparator(String sortKey, String sortOrder) {
		comparator = new GraphObjectComparator(sortKey, sortOrder);
	}
	
	@Override
	public int compare(AbstractNode o1, AbstractNode o2) {
		return comparator.compare(o1, o2);
	}
}
