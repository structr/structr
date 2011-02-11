/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.log;

import java.util.Queue;

/**
 *
 * @author Christian Morgner
 */
public class LogCommand extends LogServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		Queue queue = (Queue)getArgument("queue");
		if(queue != null)
		{
			for(Object param : parameters)
			{
				queue.add(param);
			}
		}

		return(null);
	}
}
