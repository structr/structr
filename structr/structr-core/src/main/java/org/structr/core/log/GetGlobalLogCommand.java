/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.log;

/**
 * Returns the global log list.
 *
 * @author Axel Morgner
 */
public class GetGlobalLogCommand extends LogServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		LogService service = (LogService)getArgument("service");

		if(service != null)
		{
			return(service.getGlobalLog());
		}

		return(null);
	}

}
