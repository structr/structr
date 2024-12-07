package org.structr.core.entity;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;

public interface SchemaView extends NodeTrait {

	Iterable<NodeInterface> getSchemaProperties();

	String getNonGraphProperties();

	String getSortOrder();
}
