package org.structr.rest.transform;

import java.util.Collections;
import java.util.List;
import org.structr.common.GraphObjectComparator;
import org.structr.core.entity.AbstractNode;

/**
 *
 */
public abstract class AbstractTransformation extends AbstractNode {

	protected List<StructrPropertySource> sort(final List<StructrPropertySource> source) {

		Collections.sort(source, new GraphObjectComparator(StructrPropertySource.position, false));
		return source;
	}
}
