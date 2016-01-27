package org.structr.rest.transform;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;

/**
 *
 */
public class VirtualTypeProperty extends OneToMany<VirtualType, VirtualProperty> {

	@Override
	public Class<VirtualType> getSourceType() {
		return VirtualType.class;
	}

	@Override
	public Class<VirtualProperty> getTargetType() {
		return VirtualProperty.class;
	}

	@Override
	public String name() {
		return "virtualProperty";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
