/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

/**
 * - stop services
 * - remove commands
 * - remove entities
 * - remove module index from $BASEDIR/modules/index/$NAME.index
 * - remove module from global module index ($BASEDIR/modules/modules.conf)
 *
 * @author Christian Morgner
 */
public class UninstallModuleCommand extends ModuleServiceCommand
{

	@Override
	public Object execute(Object... parameters)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
