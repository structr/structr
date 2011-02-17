/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

/**
 * Command for finding entity classes by class name. This command exists because
 * the ModuleService must be initialized before entity classes can be found,
 * and initialization is intiated automatically when a command is requested.
 *
 * @author chrisi
 */
public class GetAgentClassCommand extends ModuleServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		ModuleService service = (ModuleService)getArgument("moduleService");
		String name = (String)parameters[0];
		Class ret = null;

		if(service != null)
		{
			ret = service.getAgentClass(name);
		}

		return(ret);
	}
}
