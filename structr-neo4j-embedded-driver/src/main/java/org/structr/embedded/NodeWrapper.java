/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.embedded;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Label;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;

import java.util.*;

/**
 *
 */
class NodeWrapper extends EntityWrapper<org.neo4j.graphdb.Node> implements Node<String> {

	private String cachedTenantId = null;

	public NodeWrapper(final EmbeddedDatabaseService db, final org.neo4j.graphdb.Node entity) {
		super(db, entity);
	}

	@Override
	public String toString() {
		return "N" + getId();
	}

	@Override
	protected String getQueryPrefix() {
		return concat("MATCH (n", getTenantIdentifier(database), ")");
	}

	@Override
	public Relationship<String> createRelationshipTo(final Node<String> endNode, final RelationshipType relationshipType) {
		return createRelationshipTo(endNode, relationshipType, new LinkedHashMap<>());
	}

	@Override
	public Relationship<String> createRelationshipTo(final Node<String> endNode, final RelationshipType relationshipType, final Map<String, Object> properties) {

		final org.neo4j.graphdb.Relationship newRelationship = entity.createRelationshipTo(convert(endNode), convert(relationshipType));

		for (final Map.Entry<String, Object> entry : properties.entrySet()) {
			newRelationship.setProperty(entry.getKey(), entry.getValue());
		}

		return new RelationshipWrapper(database, newRelationship);
	}

	@Override
	public void addLabels(final Set<String> input) {

		for (final String name : input) {

			entity.addLabel(Label.label(name));
		}
	}

	@Override
	public void removeLabel(final String label) {
		entity.removeLabel(Label.label(label));
	}

	@Override
	public Iterable<String> getLabels() {
		return Iterables.map(l -> l.name(), entity.getLabels());
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final Node targetNode) {
		return getRelationshipTo(type, targetNode) != null;
	}

	@Override
	public Relationship<String> getRelationshipTo(final RelationshipType type, final Node<String> targetNode) {

		final org.neo4j.graphdb.Node otherNode = convert(targetNode);

		for (final org.neo4j.graphdb.Relationship rel : entity.getRelationships(convert(type))) {

			if (rel.getEndNode().equals(otherNode)) {

				return database.getCurrentTransaction().getRelationshipWrapper(rel);

			}
		}

		return null;
	}

	@Override
	public Iterable<Relationship<String>> getRelationships() {
		return Iterables.map(r -> new RelationshipWrapper(database, r), sort(entity.getRelationships()));
	}

	@Override
	public Iterable<Relationship<String>> getRelationships(final Direction direction) {
		return Iterables.map(r -> new RelationshipWrapper(database, r), sort(entity.getRelationships(convert(direction))));
	}

	@Override
	public Iterable<Relationship<String>> getRelationships(final Direction direction, final RelationshipType relationshipType) {

		if (isDeleted()) {

			// deleted nodes has not relationships
			return Collections.emptyList();
		}

		return Iterables.map(r -> new RelationshipWrapper(database, r), sort(entity.getRelationships(convert(direction), convert(relationshipType))));
	}

	@Override
	public Map<String, Long> getDegree() {

		final Map<String, Long> degree = new LinkedHashMap<>();

		for (final org.neo4j.graphdb.RelationshipType type : entity.getRelationshipTypes()) {

			final List<org.neo4j.graphdb.Relationship> rels   = Iterables.toList(entity.getRelationships(type));
			final org.neo4j.graphdb.Relationship relationship = rels.get(0);
			final String typeName                             = (String) relationship.getProperty("type");

			degree.put(typeName, Long.valueOf(rels.size()));
		}

		return degree;
	}

	@Override
	public void delete(final boolean deleteRelationships) {

		database.getCurrentTransaction().delete(this);

		super.delete(deleteRelationships);
	}

	@Override
	public void invalidate() {
	}

	@Override
	public boolean isNode() {
		return true;
	}

	public org.neo4j.graphdb.Node getNode() {
		return entity;
	}

	// ----- private methods -----
	private String concat(final String... parts) {

		final StringBuilder buf = new StringBuilder();

		for (final String part : parts) {

			// handle nulls gracefully (ignore)
			if (part != null) {

				buf.append(part);
			}
		}

		return buf.toString();
	}

	private String getTenantIdentifier(final EmbeddedDatabaseService db) {

		if (cachedTenantId == null) {

			final String identifier = db.getTenantIdentifier();

			if (StringUtils.isNotBlank(identifier)) {

				cachedTenantId = ":" + identifier;

			} else {

				cachedTenantId = "";
			}
		}

		return cachedTenantId;
	}

	private org.neo4j.graphdb.RelationshipType convert(final RelationshipType relationshipType) {
		return  org.neo4j.graphdb.RelationshipType.withName(relationshipType.name());
	}

	private org.neo4j.graphdb.Direction convert(final Direction direction) {
		return org.neo4j.graphdb.Direction.valueOf(direction.name());
	}

	private org.neo4j.graphdb.Node convert(final Node node) {
		return ((NodeWrapper)node).getNode();
	}

	private Iterable<org.neo4j.graphdb.Relationship> sort(final Iterable<org.neo4j.graphdb.Relationship> input) {

		final List<org.neo4j.graphdb.Relationship> result = Iterables.toList(input);

		Collections.sort(result, (a, b) -> {

			if (a.hasProperty("internalTimestamp") && b.hasProperty("internalTimestamp")) {

				final Comparable t1 = (Comparable) a.getProperty("internalTimestamp");
				final Comparable t2 = (Comparable) b.getProperty("internalTimestamp");

				// Yes, we deliberately provoke both ClassCastException and NullPointerException here.
				return t1.compareTo(t2);
			}

			// fallback: element ID
			return a.getElementId().compareTo(b.getElementId());
		});

		return result;
	}
}
