/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
*  Copyright (C) 2010-2013 Axel Morgner
*
*  This file is part of structr <http://structr.org>.
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
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.structr.core.entity.relationship.LocationRelationship;
import org.structr.core.graph.RelationshipInterface;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic search for nodes
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
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

			Result result = app.nodeQuery(TestOne.class).name(name).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

			// Change name attribute and search again
			final String name2 = "klppptzoehi gösoiu tzüw0e9hg";

			app.beginTx();
			node.setProperty(key, name2);
			app.commitTx();
			
			result = app.nodeQuery(TestOne.class).name(name2).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

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

			Result result = app.nodeQuery(type).and(key, date).includeDeletedAndHidden().getResult();

			assertEquals(1, result.size());
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03SearchRelationship() {

		try {

			final LocationRelationship rel = createTestRelationships(LocationRelationship.class, 1).get(0);
			final PropertyKey key1         = new StringProperty("jghsdkhgshdhgsdjkfgh").indexed();
			final Class type               = LocationRelationship.class;
			final String val1              = "54354354546806849870";

			app.beginTx();
			rel.setProperty(key1, val1);
			app.commitTx();
			
			assertTrue(rel.getProperty(key1).equals(val1));

			List<SearchAttribute> searchAttributes = new LinkedList<>();

			searchAttributes.add(Search.andExactProperty(securityContext, key1, val1));

			Result<RelationshipInterface> result = app.relationshipQuery(type).and(key1, val1).getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(rel));
			searchAttributes.clear();

			final String val2 = "ölllldjöoa8w4rasf";

			app.beginTx();
			rel.setProperty(key1, val2);
			app.commitTx();
			
			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(rel));

		} catch (FrameworkException ex) {

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

			Result result = app.nodeQuery(type).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeDeletedAndHidden().getResult();

			assertEquals(1, result.size());
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

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

			app.beginTx();
			
			// this will work
			TestSeven node = app.create(TestSeven.class, props);

			props.remove(AbstractNode.name);
			props.put(lat, 50.12285d);
			props.put(lon, 8.73924d);

			// this will fail
			TestSeven node2 = app.create(TestSeven.class, props);

			// adding another 
			TestSeven node3 = app.create(TestSeven.class, props);

			app.commitTx();
			
			fail("Expected a FrameworkException (name must_not_be_empty)");
			
		} catch (FrameworkException nfe) {
		}

	}

	public void test06DistanceSearchOnEmptyDB() {

		try {

			Result result = app.nodeQuery(TestOne.class).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeDeletedAndHidden().getResult();

			assertEquals(0, result.size());

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			System.out.println(ex.toString());
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

			Result result = app.nodeQuery(TestOne.class).name(name).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));


		} catch (FrameworkException ex) {

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

			Result result = app.nodeQuery(TestOne.class).name(name).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

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

			Result result = app.nodeQuery(TestOne.class).name(null).includeDeletedAndHidden().getResult();

			assertTrue(result.isEmpty());


		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test09SearchByEmptyStringField() {

		try {

			PropertyMap props = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			Result result = app.nodeQuery(TestOne.class).and(TestOne.aString, null).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test10SearchByEmptyDateField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			Result result = app.nodeQuery(TestOne.class).and(TestOne.aDate, null).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test11SearchByEmptyIntField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			Result result = app.nodeQuery(TestOne.class).and(TestOne.anInt, null).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test12SearchByEmptyLongField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			Result result = app.nodeQuery(TestOne.class).and(TestOne.aLong, null).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}	

	public void test13SearchByEmptyDoubleField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			Result result = app.nodeQuery(TestOne.class).and(TestOne.aDouble, null).includeDeletedAndHidden().getResult();

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}	
}
