/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

/**
 * Activates an EXISTING, deactivated module to the running system. Takes the
 * file name of the module file as a parameter.
 *
 * @author Christian Morgner
 */
public class ActivateModuleCommand extends ModuleServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		ModuleService service = (ModuleService)getArgument("moduleService");

		if(service != null && parameters.length > 0)
		{
			String moduleName = (String)parameters[0];
			service.activateModule(moduleName, true);
		}

		return(null);
	}
}
