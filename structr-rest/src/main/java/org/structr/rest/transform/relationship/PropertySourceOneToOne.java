package org.structr.rest.transform.relationship;

import org.structr.core.entity.OneToOne;
import org.structr.rest.transform.StructrPropertySource;

/**
 *
 */
public class PropertySourceOneToOne extends OneToOne<StructrPropertySource, StructrPropertySource> {

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
		return "propertySourceOneToOne";
	}
}
