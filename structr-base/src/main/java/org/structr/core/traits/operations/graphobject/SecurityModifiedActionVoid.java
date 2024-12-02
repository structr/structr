package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.VoidAction;

public class SecurityModifiedActionVoid extends VoidAction implements SecurityModified {

	public SecurityModifiedActionVoid(final Runnable function) {
		super(function);
	}

	@Override
	public void securityModified(final GraphObject graphObject, final SecurityContext securityContext) {
		function.run();
	}
}
