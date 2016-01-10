package org.structr.api.graph;

/**
 *
 */
public interface Relationship extends PropertyContainer {

	Node getStartNode();
	Node getEndNode();
	Node getOtherNode(final Node node);

	RelationshipType getType();
}
