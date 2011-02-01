/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.node;

/**
 *
 * @author cmorgner
 */
public class GraphDatabaseCommand extends NodeServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		return(arguments.get("graphDb"));
	}
}
