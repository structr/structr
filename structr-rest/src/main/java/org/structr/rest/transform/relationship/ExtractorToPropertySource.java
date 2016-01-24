package org.structr.rest.transform.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.rest.transform.StructrPropertyExtractor;
import org.structr.rest.transform.StructrPropertySource;

/**
 *
 */
public class ExtractorToPropertySource extends OneToMany<StructrPropertyExtractor, StructrPropertySource> {

	@Override
	public Class<StructrPropertyExtractor> getSourceType() {
		return StructrPropertyExtractor.class;
	}

	@Override
	public Class<StructrPropertySource> getTargetType() {
		return StructrPropertySource.class;
	}

	@Override
	public String name() {
		return "extractorToPropertySource";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}
}
