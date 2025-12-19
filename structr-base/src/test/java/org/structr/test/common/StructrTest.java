/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsManager;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.SchemaService;
import org.structr.schema.action.EvaluationHints;
import org.structr.test.core.traits.definitions.*;
import org.structr.test.core.traits.definitions.relationships.*;
import org.testng.annotations.*;

import org.structr.test.common.SharedNeo4jContainer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

public class StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(StructrTest.class.getName());

	protected static SecurityContext securityContext = null;
	protected static String basePath                 = null;
	protected static App app                         = null;
	protected static String randomTenantId           = RandomStringUtils.secure().nextAlphabetic(10).toUpperCase();
	private boolean first                            = true;

	@BeforeMethod
	protected void starting(Method method) {
		System.out.println("##### Starting " + this.getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
	}

	@AfterMethod
	protected void finished(Method method) {
		System.out.println("##### Finished " + getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
	}

	@BeforeMethod
	public void cleanDatabaseAndSchema() {

		if (!first) {

			try (final Tx tx = app.tx()) {

				final DatabaseService db = Services.getInstance().getDatabaseService();

				// delete everything
				db.cleanDatabase();

				FlushCachesCommand.flushAll();

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				logger.error("Exception while trying to clean database: {}", t.getMessage());
			}

			SchemaService.reloadSchema(new ErrorBuffer(), null, false, false);
			FlushCachesCommand.flushAll();
		}

		first = false;
	}

	@BeforeClass(alwaysRun = true)
	public void startSystem() {

		final Date now          = new Date();
		final long timestamp    = now.getTime();

		basePath = "/tmp/structr-test-" + timestamp;

		Settings.Services.setValue("NodeService SchemaService");

		setupDatabaseConnection();

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		while (!services.isInitialized()) {
			try { Thread.sleep(100); } catch (Throwable t) {}
		}

		securityContext = SecurityContext.getSuperUserInstance();
		app = StructrApp.getInstance(securityContext);
	}

	@BeforeMethod(firstTimeOnly = true)
	public void createSchema() {

		// relationships: traits
		StructrTraits.registerTrait(new OneOneOneToOneTraitDefinition());
		StructrTraits.registerTrait(new OneTwoOneToOneTraitDefinition());
		StructrTraits.registerTrait(new OneThreeOneToOneTraitDefinition());
		StructrTraits.registerTrait(new OneFourOneToOneTraitDefinition());
		StructrTraits.registerTrait(new SixOneOneToOneTraitDefinition());
		StructrTraits.registerTrait(new SixOneOneToManyTraitDefinition());
		StructrTraits.registerTrait(new SixOneManyToManyTraitDefinition());
		StructrTraits.registerTrait(new SixThreeOneToManyTraitDefinition());
		StructrTraits.registerTrait(new SixThreeOneToOneTraitDefinition());
		StructrTraits.registerTrait(new SixThreeOneToManyCascadeBothTraitDefinition());
		StructrTraits.registerTrait(new SixThreeOneToManyCascadeIncomingTraitDefinition());
		StructrTraits.registerTrait(new SixThreeOneToManyCascadeOutgoingTraitDefinition());
		StructrTraits.registerTrait(new SixNineOneToManyCascadeConstraintTraitDefinition());
		StructrTraits.registerTrait(new TenTenOneToManyTraitDefinition());
		StructrTraits.registerTrait(new TenTenOneToOneTraitDefinition());
		StructrTraits.registerTrait(new TwoOneOneToOneTraitDefinition());

		// relationships: types
		StructrTraits.registerRelationshipType("OneOneOneToOne",                    "OneOneOneToOne");
		StructrTraits.registerRelationshipType("OneTwoOneToOne",                    "OneTwoOneToOne");
		StructrTraits.registerRelationshipType("OneThreeOneToOne",                  "OneThreeOneToOne");
		StructrTraits.registerRelationshipType("OneFourOneToOne",                   "OneFourOneToOne");
		StructrTraits.registerRelationshipType("SixOneOneToOne",                    "SixOneOneToOne");
		StructrTraits.registerRelationshipType("SixOneOneToMany",                   "SixOneOneToMany");
		StructrTraits.registerRelationshipType("SixOneManyToMany",                  "SixOneManyToMany");
		StructrTraits.registerRelationshipType("SixThreeOneToMany",                 "SixThreeOneToMany");
		StructrTraits.registerRelationshipType("SixThreeOneToOne",                  "SixThreeOneToOne");
		StructrTraits.registerRelationshipType("SixThreeOneToManyCascadeBoth",      "SixThreeOneToManyCascadeBoth");
		StructrTraits.registerRelationshipType("SixThreeOneToManyCascadeIncoming",  "SixThreeOneToManyCascadeIncoming");
		StructrTraits.registerRelationshipType("SixThreeOneToManyCascadeOutgoing",  "SixThreeOneToManyCascadeOutgoing");
		StructrTraits.registerRelationshipType("SixNineOneToManyCascadeConstraint", "SixNineOneToManyCascadeConstraint");
		StructrTraits.registerRelationshipType("TenTenOneToMany",                   "TenTenOneToMany");
		StructrTraits.registerRelationshipType("TenTenOneToOne",                    "TenTenOneToOne");
		StructrTraits.registerRelationshipType("TwoOneOneToOne",                    "TwoOneOneToOne");

		// nodes: traits
		StructrTraits.registerTrait(new TestOneTraitDefinition());
		StructrTraits.registerTrait(new TestTwoTraitDefinition());
		StructrTraits.registerTrait(new TestThreeTraitDefinition());
		StructrTraits.registerTrait(new TestFourTraitDefinition());
		StructrTraits.registerTrait(new TestFiveTraitDefinition());
		StructrTraits.registerTrait(new TestSixTraitDefinition());
		StructrTraits.registerTrait(new TestSevenTraitDefinition());
		StructrTraits.registerTrait(new TestEightTraitDefinition());
		StructrTraits.registerTrait(new TestNineTraitDefinition());
		StructrTraits.registerTrait(new TestTenTraitDefinition());
		StructrTraits.registerTrait(new TestElevenTraitDefinition());
		StructrTraits.registerTrait(new TestTwelveTraitDefinition());
		StructrTraits.registerTrait(new TestThirteenTraitDefinition());

		// nodes: types
		StructrTraits.registerNodeType("TestOne",      "TestOne");
		StructrTraits.registerNodeType("TestTwo",      "TestTwo");
		StructrTraits.registerNodeType("TestThree",    "TestThree");
		StructrTraits.registerNodeType("TestFour",     "TestFour");
		StructrTraits.registerNodeType("TestFive",     "TestFive");
		StructrTraits.registerNodeType("TestSix",      "TestSix");
		StructrTraits.registerNodeType("TestSeven",    "TestSeven");
		StructrTraits.registerNodeType("TestEight",    "TestEight");
		StructrTraits.registerNodeType("TestNine",     "TestNine");
		StructrTraits.registerNodeType("TestTen",      "TestTen");
		StructrTraits.registerNodeType("TestEleven",   "TestOne", "TestEleven");
		StructrTraits.registerNodeType("TestTwelve",   "TestOne", "TestTwelve");
		StructrTraits.registerNodeType("TestThirteen", "TestThirteen");

		// create new schema instance that includes the modified root schema
		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
	}

	@AfterClass(alwaysRun = true)
	public void stopSystem() {

		Services.getInstance().shutdown();

		try {
			File testDir = new File(basePath);
			if (testDir.isDirectory()) {

				FileUtils.deleteDirectory(testDir);

			} else {

				testDir.delete();
			}

		} catch (Throwable t) {
			logger.warn("", t);
		}
	}

	protected List<NodeInterface> createTestNodes(final String type, final int number, final long delay) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final PropertyMap properties    = new PropertyMap();
			final List<NodeInterface> nodes = new LinkedList<>();
			final Traits traits             = Traits.of(type);

			properties.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), false);
			properties.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), false);
			properties.put(traits.key(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY), false);

			for (int i = 0; i < number; i++) {

				final NodeInterface node = app.create(type, properties);

				node.setName("test" + i);

				nodes.add(node);

				try {
					Thread.sleep(delay);
				} catch (InterruptedException ex) {
				}
			}

			tx.success();

			return nodes;

		} catch (Throwable t) {

			t.printStackTrace();

			logger.warn("Unable to create test nodes of type {}: {}", type, t.getMessage());
		}

		return null;
	}

	protected List<NodeInterface> createTestNodes(final String type, final int number) throws FrameworkException {

		return createTestNodes(type, number, 0);
	}

	protected NodeInterface createTestNode(final String type, final String name) throws FrameworkException {

		final PropertyMap map = new PropertyMap();

		map.put(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

		return createTestNode(type, map);
	}

	protected NodeInterface createTestNode(final String type, final PropertyMap props) throws FrameworkException {

		props.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), type);

		try (final Tx tx = app.tx()) {

			final NodeInterface newNode = app.create(type, props);

			tx.success();

			return newNode;
		}

	}

	protected NodeInterface createTestNode(final String type, final NodeAttribute<?>... attributes) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface newNode = app.create(type, attributes);

			tx.success();

			return newNode;
		}
	}

	protected List<RelationshipInterface> createTestRelationships(final String type1, final String type2, final String relType, final int number) {

		List<RelationshipInterface> rels = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			final NodeInterface startNode    = createTestNode(type1);
			final NodeInterface endNode      = createTestNode(type2);

			for (int i = 0; i < number; i++) {

				rels.add(app.create(startNode, endNode, relType));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return rels;
	}

	protected RelationshipInterface createTestRelationship(final NodeInterface startNode, final NodeInterface endNode, final String relType) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final RelationshipInterface rel = app.create(startNode, endNode, relType);

			tx.success();

			return rel;
		}
	}

	protected NodeInterface createTestNode(final String type, final Principal owner) throws FrameworkException {
		return createTestNode(type, new PropertyMap(), owner);
	}

	protected NodeInterface createTestNode(final String type, final PropertyMap props, final Principal owner) throws FrameworkException {

		final App backendApp = StructrApp.getInstance(SecurityContext.getInstance(owner, AccessMode.Backend));

		try (final Tx tx = backendApp.tx()) {

			final NodeInterface result = backendApp.create(type, props);
			tx.success();

			return result;
		}
	}

	protected <T> List<T> toList(T... elements) {
		return Arrays.asList(elements);
	}

	protected Map<String, Object> toMap(final String key1, final Object value1) {
		return toMap(key1, value1, null, null);
	}

	protected Map<String, Object> toMap(final String key1, final Object value1, final String key2, final Object value2) {
		return toMap(key1, value1, key2, value2, null, null);
	}

	protected Map<String, Object> toMap(final String key1, final Object value1, final String key2, final Object value2, final String key3, final Object value3) {

		final Map<String, Object> map = new LinkedHashMap<>();

		if (key1 != null && value1 != null) {
			map.put(key1, value1);
		}

		if (key2 != null && value2 != null) {
			map.put(key2, value2);
		}

		if (key3 != null && value3 != null) {
			map.put(key3, value3);
		}

		return map;
	}

	protected <T> PropertyKey<T> getKey(final String typeName, final String keyName) {

		final Traits type = Traits.of(typeName);
		if (type != null) {

			return type.key(keyName);
		}

		return null;
	}

	protected void setupDatabaseConnection() {
		String testDriver = System.getProperty("testDatabaseDriver", Settings.DEFAULT_REMOTE_DATABASE_DRIVER);
		Settings.DatabaseDriver.setValue(testDriver);

		if (testDriver.contains("Bolt")) {
			Settings.ConnectionUser.setValue(SharedNeo4jContainer.getUsername());
			Settings.ConnectionPassword.setValue(SharedNeo4jContainer.getPassword());
			Settings.ConnectionUrl.setValue(SharedNeo4jContainer.getBoltUrl());
			Settings.ConnectionDatabaseName.setValue("neo4j");
		}

		Settings.TenantIdentifier.setValue(randomTenantId);
	}

	protected void tryWithTimeout(final Supplier<Boolean> workload, final Runnable onTimeout, final int timeoutInMS) {

		if (workload != null && timeoutInMS >= 0) {
			final long startTime = System.currentTimeMillis();

			do {
				if (workload.get()) {
					return;
				}
			} while ((startTime + timeoutInMS) >= System.currentTimeMillis());
		}

		if (onTimeout != null) {
			onTimeout.run();
		}
	}

	protected void tryWithTimeout(final Supplier<Boolean> workload, final Runnable onTimeout, final int timeoutInMS, final int retryDelayInMS) {

		final long startTime = System.currentTimeMillis();

		if (workload != null && onTimeout != null && timeoutInMS >= 0 && retryDelayInMS > 0) {
			do {
				if (workload.get()) {
					return;
				}

				try {

					Thread.sleep(retryDelayInMS);
				} catch (InterruptedException ex) {

					return;
				}
			} while ((startTime + timeoutInMS) >= System.currentTimeMillis());

			onTimeout.run();
		}
	}

	protected Object invokeMethod(final SecurityContext securityContext, final NodeInterface node, final String methodName, final Map<String, Object> parameters, final boolean throwIfNotExists, final EvaluationHints hints) throws FrameworkException {

		final AbstractMethod method = Methods.resolveMethod(node.getTraits(), methodName);
		if (method != null) {

			hints.reportExistingKey(methodName);

			return method.execute(securityContext, node, NamedArguments.fromMap(parameters), new EvaluationHints());
		}

		if (throwIfNotExists) {
			throw new FrameworkException(400, "Method " + methodName + " not found in type " + node.getType());
		}

		return null;
	}
}
