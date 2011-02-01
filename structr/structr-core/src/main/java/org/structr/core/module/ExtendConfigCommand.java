/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

import org.w3c.dom.Document;

/**
 *
 * @author chrisi
 */
public class ExtendConfigCommand extends ModuleServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		ModuleService service = (ModuleService)getArgument("moduleService");

		if(service != null && parameters != null && parameters.length == 1)
		{
			if(parameters[0] instanceof Document)
			{
				Document xmlConfig = (Document)parameters[0];
				service.extendConfigDocument(xmlConfig);
			}
		}

		return(null);
	}
}
