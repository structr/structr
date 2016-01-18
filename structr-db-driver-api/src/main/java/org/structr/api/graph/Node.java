package org.structr.api.graph;

/**
 *
 */
public interface Node extends PropertyContainer {

	Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType);

	void addLabel(final Label label);
	void removeLabel(final Label label);

	Iterable<Label> getLabels();

	Iterable<Relationship> getRelationships();
	Iterable<Relationship> getRelationships(final Direction direction);
	Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType);
}
