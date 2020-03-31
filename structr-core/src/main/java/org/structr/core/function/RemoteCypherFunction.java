/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.core.function;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.GenericProperty;
import org.structr.schema.action.ActionContext;

public class RemoteCypherFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_CYPHER    = "Usage: ${remote_cypher(url, username, password, query)}. Example ${remote_cypher('bolt://database.url', 'user', 'password', 'MATCH (n) RETURN n')}";
	public static final String ERROR_MESSAGE_CYPHER_JS = "Usage: ${{Structr.remoteCypher(url, username, password query)}}. Example ${{Structr.remoteCypher('bolt://database.url', 'user', 'password', 'MATCH (n) RETURN n')}}";

	@Override
	public String getName() {
		return "remote_cypher";
	}

	@Override
	public String getSignature() {
		return "url, username, password, query [, parameterMap ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndTypes(sources, 4, String.class, String.class, String.class, String.class, Map.class);

			final Map<String, Object> params = new LinkedHashMap<>();
			final String url                 = sources[0].toString();
			final String username            = sources[1].toString();
			final String password            = sources[2].toString();
			final String query               = sources[3].toString();

			// parameters?
			if (sources.length > 4 && sources[4] != null && sources[4] instanceof Map) {
				params.putAll((Map)sources[4]);
			}

			try (final Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password), Config.build().withEncryption().toConfig())) {

				try (final Session session = driver.session(AccessMode.WRITE)) {

					final StatementResult result         = session.run(query, params);
					final List<Map<String, Object>> list = result.list(r -> {
						return r.asMap();
					});

					return extractRows(ctx.getSecurityContext(), list);
				}
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CYPHER_JS : ERROR_MESSAGE_CYPHER);
	}

	@Override
	public String shortDescription() {
		return "Returns the result of the given Cypher query";
	}


	// ----- private methods -----
	private Iterable extractRows(final SecurityContext securityContext, final Iterable<Map<String, Object>> result) {
		return Iterables.map(map -> { return extractColumns(securityContext, map); }, result);
	}

	private Object extractColumns(final SecurityContext securityContext, final Map<String, Object> map) {

		if (map.size() == 1) {

			final Entry<String, Object> entry = map.entrySet().iterator().next();
			final String key                  = entry.getKey();
			final Object value                = entry.getValue();

			try {

				return handleObject(securityContext, key, value, 0);

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

		} else {

			return Iterables.map(entry -> {

				final String key = entry.getKey();
				final Object val = entry.getValue();

				try {

					return handleObject(securityContext, key, val, 0);

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}

				return null;

			}, map.entrySet());
		}

		return null;
	}

	final Object handleObject(final SecurityContext securityContext, final String key, final Object value, int level) throws FrameworkException {

		if (value instanceof org.neo4j.driver.v1.types.Node) {

			final org.neo4j.driver.v1.types.Node node = (org.neo4j.driver.v1.types.Node)value;
			final RemoteNode remoteNode               = new RemoteNode(node);

			return handleObject(securityContext, key, remoteNode, level);
		}

		if (value instanceof Node) {

			return instantiateNode(securityContext ,(Node) value);

		} else if (value instanceof Relationship) {

			final Relationship relationship = (Relationship)value;
			final GraphObject sourceNode    = instantiateNode(securityContext, relationship.getStartNode());
			final GraphObject targetNode    = instantiateNode(securityContext, relationship.getEndNode());

			if (sourceNode != null && targetNode != null) {

				return instantiateRelationship(securityContext, (Relationship) value);
			}

			return null;

		} else if (value instanceof Path) {

			final List list = new LinkedList<>();
			final Path path = (Path)value;

			for (final PropertyContainer container : path) {

				final Object child = handleObject(securityContext, null, container, level + 1);
				if (child != null) {

					list.add(child);

				} else {

					// remove path from list if one of the children is null (=> permission)
					return null;
				}
			}

			return list;

		} else if (value instanceof Map) {

			final Map<String, Object> valueMap = (Map<String, Object>)value;
			final GraphObjectMap graphObject   = new GraphObjectMap();

			for (final Entry<String, Object> valueEntry : valueMap.entrySet()) {

				final String valueKey   = valueEntry.getKey();
				final Object valueValue = valueEntry.getValue();
				final Object result     = handleObject(securityContext, valueKey, valueValue, level + 1);

				if (result != null) {

					graphObject.setProperty(new GenericProperty(valueKey), result);
				}
			}

			return graphObject;

		} else if (value instanceof Iterable) {

			final Iterable<Object> valueCollection = (Iterable<Object>)value;
			final List<Object> collection          = new LinkedList<>();

			for (final Object valueEntry : valueCollection) {

				final Object result = handleObject(securityContext, null, valueEntry, level + 1);
				if (result != null) {

					collection.add(result);

				} else {

					// remove tuple from list if one of the children is null (=> permission)
					return null;
				}
			}

			return collection;

		} else if (level == 0) {

			final GraphObjectMap graphObject = new GraphObjectMap();
			graphObject.setProperty(new GenericProperty(key), value);

			return graphObject;
		}

		return value;
	}

	private <T extends RelationshipInterface> T instantiateRelationship(final SecurityContext securityContext, final Relationship source) {

		final T relationship =  (T)new GenericRelationship(securityContext, source, 0);

		relationship.init(securityContext, source, GenericRelationship.class, 0);
		relationship.onRelationshipInstantiation();

		return relationship;
	}

	private <T extends NodeInterface> T instantiateNode(final SecurityContext securityContext, final Node source) {

		final T node =  (T)new GenericNode();

		node.init(securityContext, source, GenericNode.class, 0);
		node.onNodeInstantiation(false);

		return node;
	}

	private static class RemoteNode implements Node {

		private org.neo4j.driver.v1.types.Node node = null;
		private RemoteIdentity identity             = null;

		public RemoteNode(final org.neo4j.driver.v1.types.Node node) {

			this.identity = new RemoteIdentity();
			this.node     = node;
		}

		@Override
		public Relationship createRelationshipTo(Node endNode, RelationshipType relationshipType) {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public Relationship createRelationshipTo(Node endNode, RelationshipType relationshipType, Map<String, Object> properties) {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public void addLabel(String label) {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public void removeLabel(String label) {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public Iterable<String> getLabels() {
			return node.labels();
		}

		@Override
		public boolean hasRelationshipTo(RelationshipType relationshipType, Node targetNode) {
			throw new UnsupportedOperationException("Not supported");
		}

		@Override
		public Relationship getRelationshipTo(RelationshipType relationshipType, Node targetNode) {
			throw new UnsupportedOperationException("Not supported");
		}

		@Override
		public Iterable<Relationship> getRelationships() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Iterable<Relationship> getRelationships(Direction direction) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Iterable<Relationship> getRelationships(Direction direction, RelationshipType relationshipType) {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Identity getId() {
			return identity;
		}

		@Override
		public boolean hasProperty(String name) {
			return getProperty(name) != null;
		}

		@Override
		public Object getProperty(String name) {
			return getProperty(name, null);
		}

		@Override
		public Object getProperty(String name, Object defaultValue) {

			final org.neo4j.driver.v1.Value value = node.get(name);
			if (value.isNull()) {

				return defaultValue;
			}

			return value.asObject();
		}

		@Override
		public void setProperty(String name, Object value) {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public void setProperties(Map<String, Object> values) {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public void removeProperty(String name) {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public Iterable<String> getPropertyKeys() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public void delete(boolean deleteRelationships) throws NotInTransactionException {
			throw new UnsupportedOperationException("Read only");
		}

		@Override
		public boolean isStale() {
			return true;
		}

		@Override
		public boolean isDeleted() {
			return false;
		}

		private class RemoteIdentity implements Identity {

			public Long getId() {
				return node.id();
			}

			@Override
			public int compareTo(Object o) {

				if (o instanceof RemoteIdentity) {


					final RemoteIdentity other = (RemoteIdentity)o;
					return getId().compareTo(other.getId());
				}

				throw new ClassCastException();
			}
		}
	}
}
