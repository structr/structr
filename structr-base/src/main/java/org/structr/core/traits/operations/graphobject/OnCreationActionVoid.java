package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.VoidAction;

public class OnCreationActionVoid extends VoidAction implements OnCreation {

	public OnCreationActionVoid(final Runnable function) {
		super(function);
	}

	@Override
	public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		function.run();
	}
}
