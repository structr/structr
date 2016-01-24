package org.structr.rest.transform.relationship;

import org.structr.core.entity.ManyToOne;
import org.structr.rest.transform.StructrPropertySource;

/**
 *
 */
public class PropertySourceManyToOne extends ManyToOne<StructrPropertySource, StructrPropertySource> {

	@Override
	public Class<StructrPropertySource> getSourceType() {
		return StructrPropertySource.class;
	}

	@Override
	public Class<StructrPropertySource> getTargetType() {
		return StructrPropertySource.class;
	}

	@Override
	public String name() {
		return "propertySourceManyToOne";
	}
}
