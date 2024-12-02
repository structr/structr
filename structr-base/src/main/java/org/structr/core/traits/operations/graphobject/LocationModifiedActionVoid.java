package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.VoidAction;

public class LocationModifiedActionVoid extends VoidAction implements LocationModified {

	public LocationModifiedActionVoid(final Runnable function) {
		super(function);
	}

	@Override
	public void locationModified(final GraphObject graphObject, final SecurityContext securityContext) {
		function.run();
	}
}
