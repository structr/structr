package org.structr.core.module;

import org.structr.common.error.FrameworkException;

/**
 * No-op command to initialize module service.
 *
 * @author Christian Morgner
 */
public class InitializeModuleServiceCommand extends ModuleServiceCommand {

	public void execute() throws FrameworkException {
		// noop
	}
}
