package org.structr.core.traits.wrappers;

import org.structr.core.GraphObject;
import org.structr.core.traits.Traits;

public abstract class AbstractTraitWrapper<T extends GraphObject> {

	protected final T wrappedObject;
	protected final Traits traits;

	public AbstractTraitWrapper(final Traits traits, final T wrappedObject) {

		this.wrappedObject = wrappedObject;
		this.traits        = traits;
	}

	public String getUuid() {
		return wrappedObject.getUuid();
	}

	public String getType() {
		return wrappedObject.getType();
	}

	public T getWrappedNode() {
		return wrappedObject;
	}
}
