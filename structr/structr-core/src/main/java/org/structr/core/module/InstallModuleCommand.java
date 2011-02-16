/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

/**
 * steps:
 *  - open module JAR
 *  - create index from contents, store in $BASEDIR/modules/index/$NAME.index
 *  - add module to ModuleService
 *  - add entities to Services.entityClassCache
 *  - service commands need to be accessible by class loader etc..
 *  - on success: add to global module index ($BASEDIR/modules/modules.conf) => module is active
 *
 * @author Christian Morgner
 */
public class InstallModuleCommand extends ModuleServiceCommand
{

	@Override
	public Object execute(Object... parameters)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
