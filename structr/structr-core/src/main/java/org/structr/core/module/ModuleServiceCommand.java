/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

import org.structr.core.Command;

/**
 *
 * @author cmorgner
 */
public abstract class ModuleServiceCommand extends Command
{
	@Override
	public Class getServiceClass()
	{
		return(ModuleService.class);
	}
}
