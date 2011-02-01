/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

/**
 * Removes a module from the running system, undeploying resources etc.
 *
 * @author Christian Morgner
 */
public class DeactivateModuleCommand extends ModuleServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		ModuleService service = (ModuleService)getArgument("moduleService");

		if(service != null && parameters.length > 0)
		{
			String moduleName = (String)parameters[0];
			service.deactivateModule(moduleName);
		}

		return(null);
	}
}
