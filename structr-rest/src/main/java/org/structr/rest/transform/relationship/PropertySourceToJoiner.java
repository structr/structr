package org.structr.rest.transform.relationship;

import org.structr.core.entity.ManyToOne;
import org.structr.core.entity.Relation;
import org.structr.rest.transform.StructrGraphObjectJoiner;
import org.structr.rest.transform.StructrPropertySource;

/**
 *
 */
public class PropertySourceToJoiner extends ManyToOne<StructrPropertySource, StructrGraphObjectJoiner> {

	@Override
	public Class<StructrPropertySource> getSourceType() {
		return StructrPropertySource.class;
	}

	@Override
	public Class<StructrGraphObjectJoiner> getTargetType() {
		return StructrGraphObjectJoiner.class;
	}

	@Override
	public String name() {
		return "propertySourceToJoiner";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}
}
