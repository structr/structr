package org.structr.rest.transform.relationship;

import org.structr.core.entity.OneToOne;
import org.structr.core.entity.Relation;
import org.structr.rest.transform.StructrGraphObjectSource;
import org.structr.rest.transform.StructrPropertyExtractor;

/**
 */
public class NodeSourceToExtractor extends OneToOne<StructrGraphObjectSource, StructrPropertyExtractor> {

	@Override
	public Class<StructrGraphObjectSource> getSourceType() {
		return StructrGraphObjectSource.class;
	}

	@Override
	public Class<StructrPropertyExtractor> getTargetType() {
		return StructrPropertyExtractor.class;
	}

	@Override
	public String name() {
		return "nodeSourceToExtractor";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}

}
