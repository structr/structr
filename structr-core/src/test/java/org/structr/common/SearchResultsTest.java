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

import org.neo4j.graphdb.NotFoundException;

import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSeven;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.search.DistanceSearchAttribute;
import org.structr.core.graph.search.FilterSearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchOperator;
import org.structr.core.graph.search.TextualSearchAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

			PropertyMap props = new PropertyMap();
			PropertyKey key   = AbstractNode.name;
			String name       = "89w3hklsdfghsdkljth";

			props.put(key, name);

			AbstractNode node                      = createTestNode(TestOne.class.getSimpleName(), props);
			boolean includeDeletedAndHidden        = true;
			boolean publicOnly                     = false;
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(new TextualSearchAttribute(key, name, SearchOperator.AND));

			Result result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(node));

			// Change name attribute and search again
			name = "klppptzoehigösoiutzüw0e9hg";

			node.setProperty(key, name);
			searchAttributes.clear();
			searchAttributes.add(new TextualSearchAttribute(key, name, SearchOperator.AND));

			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);

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
			String type       = TestOne.class.getSimpleName();

			props.put(key, date);

			AbstractNode node                      = createTestNode(type, props);
			boolean includeDeletedAndHidden        = true;
			boolean publicOnly                     = false;
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(new TextualSearchAttribute(AbstractNode.type, type, SearchOperator.AND));
			searchAttributes.add(new FilterSearchAttribute(key, date, SearchOperator.AND));

			Result result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);

			assertEquals(1, result.size());
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03SearchRelationship() {

		try {

			AbstractRelationship rel = ((List<AbstractRelationship>) createTestRelationships(RelType.IS_AT, 1)).get(0);
			PropertyKey key1         = new StringProperty("jghsdkhgshdhgsdjkfgh");
			String val1              = "54354354546806849870";

			rel.setProperty(key1, val1);
			assertTrue(rel.getProperty(key1).equals(val1));

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andExactProperty(key1, val1));

			List<AbstractRelationship> result = (List<AbstractRelationship>) searchRelationshipCommand.execute(searchAttributes);

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(rel));
			searchAttributes.clear();

			val1 = "ölllldjöoa8w4rasf";

			rel.setProperty(key1, val1);
			searchAttributes.add(Search.andExactProperty(key1, val1));
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
			final String type       = TestSeven.class.getSimpleName();

			props.put(lat, 50.12284d);
			props.put(lon, 8.73923d);
			props.put(AbstractNode.name, "TestSeven-0");

			AbstractNode node                      = createTestNode(type, props);
			boolean includeDeletedAndHidden        = true;
			boolean publicOnly                     = false;
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(new TextualSearchAttribute(AbstractNode.type, type, SearchOperator.AND));
			searchAttributes.add(new DistanceSearchAttribute("Hanauer Landstraße", "200", "60314", "Frankfurt", null, "Germany", 10.0, SearchOperator.AND));
//			searchAttributes.add(new DistanceSearchAttribute("Hanauer Landstr. 200, 60314 Frankfurt, Germany", 10.0, SearchOperator.AND));

			Result result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);

			assertEquals(1, result.size());
			assertTrue(result.get(0).equals(node));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test05SpatialRollback() {

//		try {

			final String type = TestSeven.class.getSimpleName();
			AbstractNode node = null;

			try {

				// outer transaction
				node = transactionCommand.execute(new StructrTransaction<AbstractNode>() {

					@Override
					public AbstractNode execute() throws FrameworkException {

						final PropertyMap props = new PropertyMap();
						final PropertyKey lat   = TestSeven.latitude;
						final PropertyKey lon   = TestSeven.longitude;

						props.put(AbstractNode.type, type);
						props.put(lat, 50.12284d);
						props.put(lon, 8.73923d);
						props.put(AbstractNode.name, "TestSeven-0");;

						// this will work
						AbstractNode node = createNodeCommand.execute(props);

						props.remove(AbstractNode.name);
						props.put(lat, 50.12285d);
						props.put(lon, 8.73924d);

						// this will fail
						AbstractNode node2 = createNodeCommand.execute(props);
						
						// adding another 
						AbstractNode node3 = createNodeCommand.execute(props);
						
//						boolean includeDeletedAndHidden        = true;
//						boolean publicOnly                     = false;
//						List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
//
//						searchAttributes.add(new TextualSearchAttribute(AbstractNode.type, type, SearchOperator.AND));
//						searchAttributes.add(new DistanceSearchAttribute("Hanauer Landstr. 200, 60314 Frankfurt, Germany", 10.0, SearchOperator.AND));
//
//						Result result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);
//
//						assertEquals(1, result.size());
//						assertTrue(result.get(0).equals(node));

						return node2;

					}

				});

				fail("Expected a FrameworkException (name must_not_be_empty)");
			} catch (FrameworkException nfe) {

				// Expected
			}

//
//		} catch (FrameworkException ex) {
//
//			logger.log(Level.SEVERE, ex.toString());
//			System.out.println(ex.toString());
//			fail("Unexpected exception");
//
//		}

	}

	public void test06DistanceSearchOnEmptyDB() {

		try {

			boolean includeDeletedAndHidden        = true;
			boolean publicOnly                     = false;
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(new DistanceSearchAttribute("Hanauer Landstraße", "200", "60314", "Frankfurt", null, "Germany", 10.0, SearchOperator.AND));
//			searchAttributes.add(new DistanceSearchAttribute("Hanauer Landstr. 200, 60314 Frankfurt, Germany", 10.0, SearchOperator.AND));

			Result result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);

			assertEquals(0, result.size());

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			System.out.println(ex.toString());
			fail("Unexpected exception");

		}

	}

}
