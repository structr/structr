package org.structr.core.traits.operations.nodeinterface;

import org.structr.api.Predicate;
import org.structr.api.graph.RelationshipType;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.operations.OverwritableOperation;

public interface GetRelationships extends OverwritableOperation<GetRelationships> {

	boolean hasRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode);
	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> R getRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode);

	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationships(final NodeInterface node);
	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser(final NodeInterface node);

	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getIncomingRelationships(final NodeInterface node);
	<A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getOutgoingRelationships(final NodeInterface node);

	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final NodeInterface node, final Class<? extends Relation<A, B, S, T>> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final NodeInterface node, final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final NodeInterface node, final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationships(final NodeInterface node, final Class<R> type);

	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationship(final NodeInterface node, final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationships(final NodeInterface node, final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationshipsAsSuperUser(final NodeInterface node, final Class<R> type, final Predicate<NodeInterface> predicate);

	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationship(final NodeInterface node, final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type);
	<A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<RelationshipInterface<A, B>> getOutgoingRelationships(final NodeInterface node, final Class<R> type);
}
