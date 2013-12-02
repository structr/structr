package org.structr.core.graph;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public interface TransactionPostProcess {
	
	public boolean execute(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;
}
