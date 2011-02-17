/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

import java.util.Set;

/**
 *
 * @author chrisi
 */
public class GetAgentPackagesCommand extends ModuleServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		ModuleService service = (ModuleService)getArgument("moduleService");
		Set<String> ret = null;

		if(service != null)
		{
			ret = service.getAgentPackages();
		}

		return(ret);
	}

}
