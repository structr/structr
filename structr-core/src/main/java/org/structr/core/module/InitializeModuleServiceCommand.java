package org.structr.core.module;

import org.structr.common.error.FrameworkException;

/**
 * Initializes the module service.
 *
 * @author Christian Morgner
 */
public class InitializeModuleServiceCommand extends ModuleServiceCommand {

	public void execute() throws FrameworkException {
		// noop, instantiation of this command
		// is enough to initialize the service
	}
}
