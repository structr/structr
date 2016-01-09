/**
 * Copyright (C) 2010-2016 Structr GmbH
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
/*
*  Copyright (C) 2010-2013 Axel Morgner
*
*  This file is part of Structr <http://structr.org>.
*
*  structr is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  structr is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with structr.  If not, see <http://www.gnu.org/licenses/>.
*/



package org.structr.common;


import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSeven;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic search for nodes
 *
 * All tests are executed in superuser context
 *
 *
 */
public class SearchResultsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(SearchResultsTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01SearchSingleNodeByName() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final AbstractNode node = createTestNode(TestOne.class, props);
			
			Result result = null;

			try (final Tx tx = app.tx()) {
				
				result = app.nodeQuery(TestOne.class).andName(name).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

			// Change name attribute and search again
			final String name2 = "klppptzoehi gösoiu tzüw0e9hg";

			try (final Tx tx = app.tx()) {
				
				node.setProperty(key, name2);
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				
				result = app.nodeQuery(TestOne.class).andName(name2).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test02SearchSingleNodeByDate() {

		try {

			PropertyMap props = new PropertyMap();
			PropertyKey key   = TestOne.aDate;
			Date date         = new Date();
			Class type        = TestOne.class;

			props.put(key, date);

			AbstractNode node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {
				
				Result result = app.nodeQuery(type).and(key, date).includeDeletedAndHidden().getResult();

				assertEquals(1, result.size());
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03SearchRelationship() {

		try {

			final NodeHasLocation rel = createTestRelationships(NodeHasLocation.class, 1).get(0);
			final PropertyKey key1    = new StringProperty("jghsdkhgshdhgsdjkfgh").indexed();
			final Class type          = NodeHasLocation.class;
			final String val1         = "54354354546806849870";
			
			final Result<RelationshipInterface> result;

			try (final Tx tx = app.tx()) {
				
				rel.setProperty(key1, val1);
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				
				assertTrue(rel.getProperty(key1).equals(val1));

				result = app.relationshipQuery(type).and(key1, val1).getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(rel));
			}

			final String val2 = "ölllldjöoa8w4rasf";

			try (final Tx tx = app.tx()) {
				
				rel.setProperty(key1, val2);
				tx.success();
			}

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(rel));

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test04SearchByLocation() {

		try {

			final PropertyMap props = new PropertyMap();
			final PropertyKey lat   = TestSeven.latitude;
			final PropertyKey lon   = TestSeven.longitude;
			final Class type        = TestSeven.class;

			props.put(lat, 50.12284d);
			props.put(lon, 8.73923d);
			props.put(AbstractNode.name, "TestSeven-0");

			AbstractNode node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {
				
				Result result = app.nodeQuery(type).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeDeletedAndHidden().getResult();

				assertEquals(1, result.size());
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test05SpatialRollback() {

		try {

			final Class type        = TestSeven.class;
			final PropertyMap props = new PropertyMap();
			final PropertyKey lat   = TestSeven.latitude;
			final PropertyKey lon   = TestSeven.longitude;

			props.put(AbstractNode.type, type.getSimpleName());
			props.put(lat, 50.12284d);
			props.put(lon, 8.73923d);
			props.put(AbstractNode.name, "TestSeven-0");;

			try (final Tx tx = app.tx()) {

				// this will work
				TestSeven node = app.create(TestSeven.class, props);

				props.remove(AbstractNode.name);
				props.put(lat, 50.12285d);
				props.put(lon, 8.73924d);

				// this will fail
				TestSeven node2 = app.create(TestSeven.class, props);

				// adding another 
				TestSeven node3 = app.create(TestSeven.class, props);

				tx.success();
			}

			fail("Expected a FrameworkException (name must_not_be_empty)");
			
		} catch (FrameworkException nfe) {
			nfe.printStackTrace();
			
		}

	}

	public void test06DistanceSearchOnEmptyDB() {

		try (final Tx tx = app.tx()) {

			Result result = app.nodeQuery(TestOne.class).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeDeletedAndHidden().getResult();

			assertEquals(0, result.size());

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test07SearchByStaticMethod01() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {
				
				Result result = app.nodeQuery(TestOne.class).andName(name).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}


		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
	}

	public void test08SearchByStaticMethod02() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).andName(name).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
	}

	public void test08SearchByStaticMethodWithNullSearchValue01() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "abc";

			props.put(key, name);

			createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).andName(null).includeDeletedAndHidden().getResult();

				assertTrue(result.isEmpty());
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test09SearchByEmptyStringField() {

		try {

			PropertyMap props = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {
				
				Result result = app.nodeQuery(TestOne.class).and(TestOne.aString, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test10SearchByEmptyDateField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {
				
				Result result = app.nodeQuery(TestOne.class).and(TestOne.aDate, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test11SearchByEmptyIntField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {
				
				Result result = app.nodeQuery(TestOne.class).and(TestOne.anInt, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (Throwable ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test12SearchByEmptyLongField() {

		try {
			
			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).and(TestOne.aLong, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}	

	public void test13SearchByEmptyDoubleField() {

		try {
			
			PropertyMap props = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {
				
				Result result = app.nodeQuery(TestOne.class).and(TestOne.aDouble, null).includeDeletedAndHidden().getResult();
				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}	
}
