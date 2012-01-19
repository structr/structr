/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.filter;

import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public interface Filter {

	public boolean includeInResultSet(GraphObject object);
}
