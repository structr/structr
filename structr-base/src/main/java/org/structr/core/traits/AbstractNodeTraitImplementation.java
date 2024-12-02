package org.structr.core.traits;

import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.graphobject.*;

import java.util.Map;
import java.util.Set;

public class AbstractNodeTraitImplementation extends AbstractTraitImplementation {

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnModification.class,         new OnModificationActionVoid(this::clearCaches),
			OnDeletion.class,             new OnDeletionActionVoid(this::clearCaches),
			OwnerModified.class,          new OwnerModifiedActionVoid(this::clearCaches),
			SecurityModified.class,       new SecurityModifiedActionVoid(this::clearCaches),
			LocationModified.class,       new LocationModifiedActionVoid(this::clearCaches),
			PropagatedModification.class, new PropagatedModificationActionVoid(this::clearCaches)
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return null;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	// ----- private methods -----
	private void clearCaches() {

	}


	/*
	@Override
	public void ownerModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void securityModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void locationModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
		clearCaches();
	}
	*/
}
