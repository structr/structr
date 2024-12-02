package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.VoidAction;

public class PropagatedModificationActionVoid extends VoidAction implements PropagatedModification {

	public PropagatedModificationActionVoid(final Runnable function) {
		super(function);
	}

	@Override
	public void propagatedModification(final GraphObject graphObject, final SecurityContext securityContext) {
		function.run();
	}
}
