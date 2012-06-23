/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.filter;

import java.util.Comparator;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public abstract class Filter {

	private Comparator<GraphObject> comparator = null;

	public abstract boolean includeInResultSet(SecurityContext securityContext, GraphObject object);

	public void setComparator(Comparator<GraphObject> comparator) {
		this.comparator = comparator;
	}

	public Comparator<GraphObject> getComparator() {
		return comparator;
	}
}
