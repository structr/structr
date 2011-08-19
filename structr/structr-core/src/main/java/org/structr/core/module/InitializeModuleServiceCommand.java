package org.structr.core.module;

/**
 * No-op command to initialize module service.
 * 
 * @author Christian Morgner
 */
public class InitializeModuleServiceCommand extends ModuleServiceCommand {

	@Override
	public Object execute(Object... parameters) {
		
		// noop
		return(null);
	}
}
