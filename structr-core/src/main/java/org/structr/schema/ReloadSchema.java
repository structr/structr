package org.structr.schema;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.TransactionPostProcess;

/**
 *
 * @author Christian Morgner
 */
public class ReloadSchema implements TransactionPostProcess {

	@Override
	public boolean execute(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return SchemaHelper.reloadSchema(errorBuffer);
	}
}
