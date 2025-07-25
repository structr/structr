/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.Importer;
import org.testng.annotations.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class PerformanceTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);

	/**
	 * Tests basic throughput of node creation operations
	 *
	 * Note that this is just a very rough test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to create nodes with typical performance.
	 */
	@Test
	public void testPerformanceOfNodeCreation() {

		final List<NodeInterface> nodes = new LinkedList<>();
		final long number               = 1000;

		// start measuring
		final long t0 = System.currentTimeMillis();
		final App app = StructrApp.getInstance(setupSecurityContext());

		try {

			try (final Tx tx = app.tx()) {

				final long t1 = System.currentTimeMillis();

				for (int i=0; i<number; i++) {

					nodes.add(app.create("TestOne",
						new NodeAttribute(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestOne" + i),
						new NodeAttribute(Traits.of("TestOne").key("aDate"), new Date()),
						new NodeAttribute(Traits.of("TestOne").key("aDouble"), 1.234),
						new NodeAttribute(Traits.of("TestOne").key("aLong"), 12345L),
						new NodeAttribute(Traits.of("TestOne").key("anInt"), 123)
					));
				}

				final long t2 = System.currentTimeMillis();
				System.out.println((t2 - t1) + " ms");

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

		final long t1 = System.currentTimeMillis();

		assertTrue(nodes.size() == number);

		DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		Double time                 = (t1 - t0) / 1000.0;
		Double rate                 = number / ((t1 - t0) / 1000.0);

		logger.info("Created {} nodes in {} seconds ({} per s)", number, decimalFormat.format(time), decimalFormat.format(rate) );
		assertTrue("Creation rate of nodes too low, expected > 30, was " + rate, rate > 30);
	}

	/**
	 * Tests basic throughput of relationship creation operations
	 *
	 * Note that this is just a very rought test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to create relationship with typical performance.
	 */
	@Test
	public void testPerformanceOfRelationshipCreation() {

		try {

			int expected                     = 1000;
			final App app                    = StructrApp.getInstance(setupSecurityContext());
			final List<NodeInterface> threes = new ArrayList<>(createNodes(app, "TestThree", expected));
			final List<NodeInterface> fours  = new ArrayList<>(createNodes(app, "TestFour", expected));
			List<RelationshipInterface> rels = new LinkedList<>();
			long t0                        = System.nanoTime();

 			try (final Tx tx = app.tx()) {

				for (int i=0 ;i<expected; i++) {

					final NodeInterface n1 = fours.get(i);
					final NodeInterface n2 = threes.get(i);

					rels.add(app.create(n1, n2, "FourThreeOneToOne"));
				}

				tx.success();
			}

			long t1                       = System.nanoTime();

			assertEquals("Invalid relationship creation result", expected, rels.size());

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = expected / ((t1 - t0) / 1000000000.0);

			logger.info("Created {} relationships in {} seconds ({} per s)", new Object[] { expected, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 50);

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	/**
	 * Tests basic throughput of reading node properties.
	 *
	 * Note that this is just a very rought test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to read nodes with typical performance.
	 */
	@Test
	public void testReadPerformance() {

		try {

			final App app = StructrApp.getInstance(setupSecurityContext());
			int number    = 1000;
			int loop      = 300;

			logger.info("Creating {} nodes..", number);

			createNodes(app, "TestOne", number);

			long t0 = System.nanoTime();

			logger.info("Flushing caches..");

			FlushCachesCommand.flushAll();

			logger.info("Reading {} x {} nodes..", loop, number);

			for (int i=0; i<loop; i++) {

				try (final Tx tx = app.tx()) {

					for (final NodeInterface t : app.nodeQuery("TestOne").getResultStream()) {

						final String name = t.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
					}

					tx.success();
				}
			}

			long t1 = System.nanoTime();


			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double time                 = (t1 - t0) / 1000000000.0;
			double rate                 = number * loop / ((t1 - t0) / 1000000000.0);

			logger.info("Read {}x {} nodes in {} seconds ({} per s)", loop, number, decimalFormat.format(time), decimalFormat.format(rate));
			assertTrue("Invalid read performance result", rate > 10000);

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	/**
	 * Tests basic throughput of reading node properties.
	 *
	 * Note that this is just a very rought test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to read nodes with typical performance.
	 */
	@Test
	public void testReadPerformanceWithPrefetching() {

		try {

			final App app = StructrApp.getInstance(setupSecurityContext());
			int number    = 1000;
			int loop      = 10;

			try (final Tx tx = app.tx()) {

				for (int i=0; i<number; i++) {

					app.create("TestFive",
						new NodeAttribute<>(Traits.of("TestFive").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestFive" + i),
						new NodeAttribute<>(Traits.of("TestFive").key("testTwo"), app.create("TestTwo", "TestTwo" + i))
					);
				}

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}

			long t0 = System.nanoTime();

			// flush caches
			FlushCachesCommand.flushAll();

			for (int i=0; i<loop; i++) {

				try (final Tx tx = app.tx()) {

					tx.prefetch("(n:TestTwo)-[r]->(m:TestFive)", Set.of(
						"all/INCOMING/TEST",
						"all/OUTGOING/TEST"
					));

					for (final NodeInterface t : app.nodeQuery("TestTwo").getAsList()) {

						t.getName();

						final PropertyKey<Iterable<NodeInterface>> key = Traits.of("TestTwo").key("testFives");
						final List<NodeInterface> list                 = Iterables.toList(t.getProperty(key));

						for (final NodeInterface f : list) {

							f.getName();
						}

						assertEquals("Invalid collection size", 1, list.size());
					}

					tx.success();
				}
			}

			long t1                   = System.nanoTime();


			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double time                 = (t1 - t0) / 1000000000.0;
			double rate                 = number * loop / ((t1 - t0) / 1000000000.0);

			logger.info("Read {}x {} nodes with relationship in {} seconds ({} per s)", new Object[] { loop, number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue("Invalid read performance result", rate > 2000);

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void testPerformanceOfNodeDeletion() {

		final App app                   = StructrApp.getInstance(setupSecurityContext());
		final List<NodeInterface> nodes = new LinkedList<>();
		final int number                = 1000;

		try {

			try (final Tx tx = app.tx()) {

				nodes.addAll(createNodes(app, "TestOne", number));
				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

		// start measuring
		final long t0 = System.currentTimeMillis();

		try {

			try (final Tx tx = app.tx()) {

				final DeleteNodeCommand command = app.command(DeleteNodeCommand.class);

				for (final NodeInterface node : nodes) {

					command.execute(node);
				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		final long t1 = System.currentTimeMillis();

		DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		Double time                 = (t1 - t0) / 1000.0;
		Double rate                 = number / ((t1 - t0) / 1000.0);

		logger.info("Deleted {} nodes in {} seconds ({} per s)", number, decimalFormat.format(time), decimalFormat.format(rate) );
		assertTrue("Deletion rate of nodes too low, expected > 40, was " + rate, rate > 40);
	}

	@Test
	public void testPerformanceOfUserSupernodes() {

		final App app                   = StructrApp.getInstance(setupSecurityContext());
		final Random randm              = new Random();
		final int number                = 1000;
		final int loops                 = 10;
		int count                       = 0;

		try {

			for (int i=0; i<10; i++) {

				logger.info("Creating {} nodes for user admin, count is {}", number, count);
				try (final Tx tx = app.tx()) {

					for (final NodeInterface n : createNodes(app, "TestTwo", number)){

						n.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Test" + StringUtils.leftPad(Integer.toString(count++), 5, "0"));
						n.setProperty(Traits.of("TestTwo").key("testFives"), createNodes(app, "TestFive", 3));
					}

					tx.success();
				}
			}

			logger.info("Done, count is {}", count);

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

		long averageNodeFetchTime = 0L;
		long averageUserFetchTime = 0L;

		try {

			for (int i=0; i<loops; i++) {

				logger.info("Fetching some nodes..");
				try (final Tx tx = app.tx()) {

					final long t0 = System.currentTimeMillis();

					final int r               = randm.nextInt(10000);
					final List<NodeInterface> nodes = app.nodeQuery("TestTwo").name("Test" + StringUtils.leftPad(Integer.toString(r), 5, "0")).getAsList();

					assertEquals(1, nodes.size());

					final NodeInterface testOne = nodes.get(0);
					final List<NodeInterface> testFives = Iterables.toList((Iterable)testOne.getProperty(Traits.of("TestTwo").key("testFives")));

					assertEquals(3, testFives.size());

					averageNodeFetchTime += System.currentTimeMillis() - t0;

					tx.success();
				}
			}

			averageNodeFetchTime /= loops;

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		logger.info("Average node fetch time: {} ms", averageNodeFetchTime);

		// fetching a single TestOne node from 30000 should not take more than 100ms on average
		assertTrue(averageNodeFetchTime < 100);

		try {

			for (int i=0; i<loops; i++) {

				logger.info("Fetching user node..");
				try (final Tx tx = app.tx()) {

					final long t0 = System.currentTimeMillis();

					final Principal user = app.nodeQuery(StructrTraits.PRINCIPAL).getFirst().as(Principal.class);

					user.getGroups();

					assertNotNull(user);

					averageUserFetchTime += System.currentTimeMillis() - t0;

					tx.success();
				}
			}

			averageUserFetchTime /= loops;

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

		logger.info("Average user fetch time: {} ms", averageUserFetchTime);

		// fetching a single user node should not take more than 100ms on average
		assertTrue(averageUserFetchTime < 100);
	}

	@Test
	public void testRenderingPerformance() {

		if (System.getProperty("os.name").equals("Mac OS X")) {
			logger.info("Not performing test because it always fails on my Mac :)");
			return;
		}

		final String address = "http://structr.github.io/structr/getbootstrap.com/docs/3.3/examples/jumbotron/";

		// render page into HTML string
		try (final Tx tx = app.tx()) {

			final Importer importer = new Importer(securityContext, null, address, "testpage", true, true, false, false);

			importer.parse();

			// create page from source
			importer.readPage();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}


		final long t0   = System.currentTimeMillis();
		final int num   = 20;

		for (int i=0; i<num; i++) {

			try (final Tx tx = app.tx()) {

				final Page page      = app.nodeQuery(StructrTraits.PAGE).getFirst().as(Page.class);
				final String content = page.getContent(RenderContext.EditMode.NONE);

				System.out.println("Content length: " + content.length());

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
			}
		}

		final long duration = System.currentTimeMillis() - t0;
		final long average  = duration / num;

		System.out.println("Rendering took " + duration + " ms, average of " + average + " ms per run");

		assertTrue("Rendering performance is too low", average < 600);
	}

	@Test
	public void testRelationshipCreationAndRead() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema       = StructrSchema.createFromDatabase(app);
			final JsonObjectType baseType = schema.addType("BaseType");
			final JsonObjectType type1    = schema.addType("Type1");
			final JsonObjectType type2    = schema.addType("Type2");
			final JsonObjectType linked   = schema.addType("LinkedType");

			type1.addTrait("BaseType");
			type2.addTrait("BaseType");

			baseType.relate(linked, "LINKED", Cardinality.ManyToOne, "baseTypes", "linked");

			baseType.addMethod("onCreate","{ $.this.linked = $.requestStore.linked; }");

			// creation method
			type1.addMethod("test", "{ $.requestStore.linked = $.create('LinkedType'); for (var i=0; i<100; i++) { $.create('Type1'); $.create('Type2'); } }").setIsStatic(true);

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Type1.test(); }}", "test", "test");

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

	}

	// ----- private methods -----
	private SecurityContext setupSecurityContext() {

		final App app      = StructrApp.getInstance();
		NodeInterface user = null;

		try (final Tx tx = app.tx()) {

			user = createAdminUser();

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		return SecurityContext.getInstance(user.as(User.class), AccessMode.Backend);
	}

	private List<NodeInterface> createNodes(final App app, final String type, final int number) throws FrameworkException {

		final List<NodeInterface> nodes = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (int i = 0; i < number; i++) {

				nodes.add(app.create(type));
			}

			tx.success();
		}

		return nodes;

	}
}
