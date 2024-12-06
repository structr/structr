package org.structr.core.entity;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;

import java.util.List;

public interface AbstractSchemaNode extends NodeTrait {

	Iterable<NodeInterface> getSchemaProperties();
	Iterable<NodeInterface> getSchemaViews();
	Iterable<NodeInterface> getSchemaMethods();

	Iterable<NodeInterface> getSchemaMethodsIncludingInheritance();
	NodeInterface getSchemaMethod(String name);
	List<NodeInterface> getSchemaMethodsByName(String name);
	NodeInterface getSchemaProperty(String name);
	NodeInterface getSchemaView(String name);
}
