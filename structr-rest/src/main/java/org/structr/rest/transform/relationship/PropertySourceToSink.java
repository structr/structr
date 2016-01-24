package org.structr.rest.transform.relationship;

import org.structr.core.entity.ManyToOne;
import org.structr.rest.transform.StructrCopyPropertySource;
import org.structr.rest.transform.StructrNodeSink;

/**
 *
 * @author Christian Morgner
 */
public class PropertySourceToSink extends ManyToOne<StructrCopyPropertySource, StructrNodeSink> {

	@Override
	public Class<StructrCopyPropertySource> getSourceType() {
		return StructrCopyPropertySource.class;
	}

	@Override
	public Class<StructrNodeSink> getTargetType() {
		return StructrNodeSink.class;
	}

	@Override
	public String name() {
		return "propertySourceNodeSink";
	}
}
