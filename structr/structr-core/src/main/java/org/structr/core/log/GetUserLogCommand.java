/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.log;

import org.structr.core.entity.User;

/**
 * Returns the log list for the given user.
 *
 * @param user the user for which the log list should be returned
 *
 * @author Christian Morgner
 */
public class GetUserLogCommand extends LogServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		LogService service = (LogService)getArgument("service");
		User user = null;

		if(parameters.length > 0 && parameters[0] instanceof User)
		{
			user = (User)parameters[0];
		}

		if(service != null && user != null)
		{
			return(service.getUserLog(user));
		}

		return(null);
	}

}
