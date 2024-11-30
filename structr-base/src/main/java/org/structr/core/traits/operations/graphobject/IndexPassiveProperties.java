package org.structr.core.traits.operations.graphobject;

import org.structr.core.GraphObject;
import org.structr.core.traits.operations.OverwritableOperation;

public abstract class IndexPassiveProperties extends OverwritableOperation {

	public abstract void indexPassiveProperties(final GraphObject graphObject);
}
