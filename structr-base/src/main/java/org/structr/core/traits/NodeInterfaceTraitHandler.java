package org.structr.core.traits;

import org.structr.api.Predicate;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

public class NodeInterfaceTraitHandler extends AbstractTraitImplementation implements NodeInterfaceTrait {

	private final NodeInterfaceTraitImplementation defaultImplementation;

	public NodeInterfaceTraitHandler(final Traits traits) {

		super(traits);

		this.defaultImplementation = new NodeInterfaceTraitImplementation(traits);
	}

	@Override
	public void onNodeCreation(final NodeInterface node, final SecurityContext securityContext) throws FrameworkException {
		defaultImplementation.onNodeCreation(node, securityContext);
	}

	@Override
	public void onNodeInstantiation(final NodeInterface node, final boolean isCreation) {
		defaultImplementation.onNodeInstantiation(node, isCreation);
	}

	@Override
	public void onNodeDeletion(final NodeInterface node, final SecurityContext securityContext) throws FrameworkException {
		defaultImplementation.onNodeDeletion(node, securityContext);
	}

	@Override
	public boolean hasRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode) {
		return defaultImplementation.hasRelationshipTo(node, type, targetNode);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> R getRelationshipTo(final NodeInterface node, final RelationshipType type, final NodeInterface targetNode) {
		return defaultImplementation.getRelationshipTo(node, type, targetNode);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationships(final NodeInterface node) {
		return defaultImplementation.getRelationships(node);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser(final NodeInterface node) {
		return defaultImplementation.getRelationshipsAsSuperUser(node);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getIncomingRelationships(final NodeInterface node) {
		return defaultImplementation.getIncomingRelationships(node);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getOutgoingRelationships(final NodeInterface node) {
		return defaultImplementation.getOutgoingRelationships(node);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final NodeInterface node, final Class<? extends Relation<A, B, S, T>> type) {
		return defaultImplementation.hasRelationship(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.hasIncomingRelationships(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.hasOutgoingRelationships(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationships(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.getRelationships(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationship(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.getIncomingRelationship(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.getIncomingRelationshipAsSuperUser(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationships(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.getIncomingRelationships(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationshipsAsSuperUser(final NodeInterface node, final Class<R> type, final Predicate<NodeInterface> predicate) {
		return defaultImplementation.getIncomingRelationshipsAsSuperUser(node, type, predicate);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationship(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.getOutgoingRelationship(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationshipAsSuperUser(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.getOutgoingRelationshipAsSuperUser(node, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<RelationshipInterface<A, B>> getOutgoingRelationships(final NodeInterface node, final Class<R> type) {
		return defaultImplementation.getOutgoingRelationships(node, type);
	}
}
