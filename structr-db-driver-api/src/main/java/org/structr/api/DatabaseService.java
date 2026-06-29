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
package org.structr.api;

import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.index.Index;
import org.structr.api.index.NewIndexConfig;
import org.structr.api.util.CountResult;
import org.structr.api.util.NodeWithOwnerResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

/**
 *
 */
public interface DatabaseService<IDType> {

	/**
	 * Loads and instantiates the {@link DatabaseService} implementation with the
	 * given fully-qualified class name.
	 *
	 * Discovery goes through {@link ServiceLoader}, which finds providers both in
	 * named modules (declared via {@code provides ... with}) and on the class path
	 * (declared via {@code META-INF/services}). This is what allows structr-base -
	 * a named module on the module path - to load the embedded Neo4j driver, which
	 * is quarantined on the class path because the embedded engine is not module
	 * path compatible. A named module cannot reach a class-path type via reflective
	 * {@code Class.forName(...).newInstance()}, but it can consume it as a service.
	 *
	 * A direct reflective instantiation is kept as a transition fallback for drivers
	 * that do not (yet) declare a service provider entry.
	 *
	 * @param driverClassName the fully-qualified class name of the driver
	 * @return a new, uninitialized DatabaseService instance
	 * @throws ReflectiveOperationException if no matching driver could be instantiated
	 */
	static DatabaseService loadByClassName(final String driverClassName) throws ReflectiveOperationException {

		final ServiceLoader<DatabaseService> loader                          = ServiceLoader.load(DatabaseService.class);
		final Optional<ServiceLoader.Provider<DatabaseService>> matchingDriver = loader.stream()
			.filter(provider -> provider.type().getName().equals(driverClassName))
			.findFirst();

		if (matchingDriver.isPresent()) {

			return matchingDriver.get().get();
		}

		// transition fallback: driver without a service provider declaration
		return (DatabaseService) Class.forName(driverClassName).getDeclaredConstructor().newInstance();
	}

	// ----- lifecycle -----
	/**
	 * Initializes the service, returns true if the
	 * service was initialized successfully.
	 *
	 * @param serviceName the name of the service
	 * @param version the service version
	 * @param instanceName the instance name
	 *
	 * @return whether the service was initialized successfully
	 */
	boolean initialize(final String serviceName, final String version, final String instanceName);
	void shutdown();
	void cleanDatabase();

	RelationshipType getRelationshipType(final String name);

	Transaction<IDType> beginTx();
	Transaction<IDType> beginTx(boolean forceNew);
	Transaction<IDType> beginTx(final int timeoutInSeconds);

	Node<IDType> createNode(final String type, final Set<String> labels, final Map<String, Object> properties);
	NodeWithOwnerResult createNodeWithOwner(final Identity<IDType> ownerId, final String type, final Set<String> labels, final Map<String, Object> nodeProperties, final Map<String, Object> ownsProperties, final Map<String, Object> securityProperties);

	Node<IDType> getNodeById(final Identity<IDType> id);
	Relationship<IDType> getRelationshipById(final Identity<IDType> id);

	Iterable<Node<IDType>> getAllNodes();

	/**
	 * Returns an Iterable that iterates over all nodes in the database,
	 * optionally filtered by the given label.
	 *
	 * @param label the label or null
	 *
	 * @return an Iterable of Nodes
	 */
	Iterable<Node<IDType>> getNodesByLabel(final String label);
	Iterable<Node<IDType>> getNodesByTypeProperty(final String type);

	Iterable<Relationship<IDType>> getAllRelationships();
	Iterable<Relationship<IDType>> getRelationshipsByType(final String type);

	String getTenantIdentifier();
	String getInternalTimestamp(final long millisOffset, final long nanoOffset);
	String getErrorMessage();

	public Map<String, Map<String, Integer>> getCachesInfo();

	// ----- index -----
	Index<Node<IDType>> nodeIndex();
	Index<Relationship<IDType>> relationshipIndex();
	void updateIndexConfiguration(final List<NewIndexConfig> indexConfigList);
	boolean isIndexUpdateFinished();

	// utils
	CountResult getNodeAndRelationshipCount();
	List<Map<String, Object>> globalSearch(final Set<String> types, final String searchString);

	// native
	<T> T execute(final NativeQuery<T> nativeQuery);
	<T> T execute(final NativeQuery<T> nativeQuery, final Transaction<IDType> tx);
	<T> NativeQuery<T> query(final Object query, final Class<T> resultType);
	boolean supportsFeature(final DatabaseFeature feature, final Object...  parameters);

	void flushCaches();
}
