package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.VoidAction;

public class OwnerModifiedActionVoid extends VoidAction implements OwnerModified {

	public OwnerModifiedActionVoid(final Runnable function) {
		super(function);
	}

	@Override
	public void ownerModified(final GraphObject graphObject, final SecurityContext securityContext) {
		function.run();
	}
}
