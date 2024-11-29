package org.structr.core.traits.operations.graphobject;

import org.structr.core.GraphObject;
import org.structr.core.traits.operations.OverwritableOperation;

public interface IndexPassiveProperties extends OverwritableOperation {

	void indexPassiveProperties(final GraphObject graphObject);
}
