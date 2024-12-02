package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.traits.operations.VoidAction;

public class OnModificationActionVoid extends VoidAction implements OnModification {

	public OnModificationActionVoid(final Runnable function) {
		super(function);
	}

	@Override
	public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		function.run();
	}
}
