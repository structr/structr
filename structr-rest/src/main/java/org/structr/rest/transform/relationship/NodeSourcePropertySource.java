package org.structr.rest.transform.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.rest.transform.StructrCopyPropertySource;
import org.structr.rest.transform.StructrQueryNodeSource;

/**
 *
 * @author Christian Morgner
 */
public class NodeSourcePropertySource extends OneToMany<StructrQueryNodeSource, StructrCopyPropertySource> {

	@Override
	public Class<StructrQueryNodeSource> getSourceType() {
		return StructrQueryNodeSource.class;
	}

	@Override
	public Class<StructrCopyPropertySource> getTargetType() {
		return StructrCopyPropertySource.class;
	}

	@Override
	public String name() {
		return "nodeSourcePropertySource";
	}
}
