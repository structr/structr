/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

import java.util.Map;

/**
 *
 * @author chrisi
 */
public class GetEntitiesCommand extends ModuleServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		ModuleService service = (ModuleService)getArgument("moduleService");
		Map<String, Class> ret = null;

		if(service != null)
		{
			ret = service.getCachedEntities();
		}

		return(ret);
	}

}
