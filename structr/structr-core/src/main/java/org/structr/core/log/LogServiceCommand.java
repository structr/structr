/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.log;

import org.structr.core.Command;

/**
 *
 * @author Christian Morgner
 */
public abstract class LogServiceCommand extends Command
{
	@Override
	public Class getServiceClass()
	{
		return(LogService.class);
	}
}
