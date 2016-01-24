package org.structr.rest.transform.relationship;

import org.structr.core.entity.OneToOne;
import org.structr.core.entity.Relation;
import org.structr.rest.transform.StructrGraphObjectSource;
import org.structr.rest.transform.StructrNodeSink;

/**
 *
 */
public class SourceToSink extends OneToOne<StructrGraphObjectSource, StructrNodeSink> {

	@Override
	public Class<StructrGraphObjectSource> getSourceType() {
		return StructrGraphObjectSource.class;
	}

	@Override
	public Class<StructrNodeSink> getTargetType() {
		return StructrNodeSink.class;
	}

	@Override
	public String name() {
		return "sourceToSink";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}
}
