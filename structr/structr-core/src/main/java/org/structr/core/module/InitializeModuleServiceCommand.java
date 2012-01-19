package org.structr.core.module;

import org.structr.common.error.FrameworkException;

/**
 * No-op command to initialize module service.
 * 
 * @author Christian Morgner
 */
public class InitializeModuleServiceCommand extends ModuleServiceCommand {

	@Override
	public Object execute(Object... parameters) throws FrameworkException {
		
		// noop
		return(null);
	}
}
