/*
*  Copyright (C) 2010-2012 Axel Morgner
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

import org.apache.lucene.search.SortField;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Test paging
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class SortingTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(SortingTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01SortByName() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 4; // no more than 89 to avoid sort order TestOne-10, TestOne-100 ...
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			int offset                      = 10;
			int i                           = offset;
			String name;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				//System.out.println("Node ID: " + node.getNodeId());
				
				name = "TestOne-" + i;

				i++;

				node.setName(name);

			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andExactTypeAndSubtypes(type));

			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			String sortKey   = AbstractNode.Key.name.name();
			boolean sortDesc = false;
			int pageSize     = 10;
			int page         = 1;

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

//                      for (GraphObject obj : result.getResults()) {
//                              
//                              System.out.println(obj.getStringProperty(AbstractNode.Key.name));
//                              
//                      }
			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), Math.min(number, pageSize) });
			assertTrue(result.size() == Math.min(number, pageSize));

			for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

				String expectedName = "TestOne-" + (offset + j);
				String gotName     = result.get(j).getStringProperty(AbstractNode.Key.name);

				System.out.println(expectedName + ", got: " + gotName);
				assertTrue(gotName.equals(expectedName));

			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test02SortByNameDesc() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 43;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			int offset                      = 10;
			int i                           = offset;
			String name;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				name = Integer.toString(i);

				i++;

				node.setName(name);

			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andType(type));

			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			String sortKey   = AbstractNode.Key.name.name();
			boolean sortDesc = true;
			int pageSize     = 10;
			int page         = 1;

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

//                      for (GraphObject obj : result.getResults()) {
//                              
//                              System.out.println(obj.getStringProperty(AbstractNode.Key.name));
//                              
//                      }
			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), Math.min(number, pageSize) });
			assertTrue(result.size() == Math.min(number, pageSize));

			for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

				int expectedNumber = number + offset - 1 - j;
				String gotName     = result.get(j).getStringProperty(AbstractNode.Key.name);

				System.out.println(expectedNumber + ", got: " + gotName);
				assertTrue(gotName.equals(Integer.toString(expectedNumber)));

			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03SortByDate() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 97;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			int offset                      = 10;
			int i                           = offset;
			String name;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				name = Integer.toString(i);

				i++;

//                              try {
//                                      Thread.sleep(1000L);
//                              } catch (InterruptedException ex) {
//                              }
				node.setName("TestOne-" + name);

				long timestamp = (new Date()).getTime();

				node.setProperty(TestOne.Key.aDate, timestamp);

				// System.out.println(node.getStringProperty(AbstractNode.Key.name) + ", " + node.getProperty(TestOne.Key.aDate) + " (set: " + timestamp + ")");

			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andType(type));

			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			String sortKey   = TestOne.Key.aDate.name();
			boolean sortDesc = false;
			int pageSize     = 10;
			int page         = 1;

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, null, SortField.LONG);

//                      for (GraphObject obj : result.getResults()) {
//
//                              System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ", " + obj.getProperty(TestOne.Key.aDate.name()));
//                      }
			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
			assertTrue(result.size() == Math.min(number, pageSize));

			for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

				String expectedName = "TestOne-" + (offset + j);
				String gotName     = result.get(j).getStringProperty(AbstractNode.Key.name);

				System.out.println(expectedName + ", got: " + gotName);
				assertTrue(gotName.equals(expectedName));

			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test04SortByDateDesc() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 131;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			String name;
			int offset = 10;
			int i      = offset;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				name = Integer.toString(i);

				i++;

//                              try {
//                                      Thread.sleep(1000L);
//                              } catch (InterruptedException ex) {
//                              }
				node.setName(name);

			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andType(type));

			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			String sortKey   = AbstractNode.Key.lastModifiedDate.name();
			boolean sortDesc = true;
			int pageSize     = 10;
			int page         = 1;

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, null, SortField.LONG);

//                      for (GraphObject obj : result.getResults()) {
//
//                              System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ", " + obj.getDateProperty(AbstractNode.Key.lastModifiedDate));
//                      }
			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
			assertTrue(result.size() == Math.min(number, pageSize));

			for (int j = 0; j < pageSize; j++) {

				int expectedNumber = number + offset - 1 - j;
				String gotName     = result.get(j).getStringProperty(AbstractNode.Key.name);

				System.out.println(expectedNumber + ", got: " + gotName);
				assertTrue(gotName.equals(Integer.toString(expectedNumber)));

			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test05SortByInt() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 61;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			String key                      = TestOne.Key.anInt.name();
			int offset                      = 10;
			int i                           = offset;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				node.setName(Integer.toString(i));

//                              try {
//                                      Thread.sleep(1000L);
//                              } catch (InterruptedException ex) {
//                              }
				node.setProperty(key, i);

				i++;

			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andType(type));

			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			String sortKey   = key;
			boolean sortDesc = false;
			int pageSize     = 5;
			int page         = 1;

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, null, SortField.INT);

//                      for (GraphObject obj : result.getResults()) {
//
//                              System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ": " + obj.getIntProperty(key));
//                      }
			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
			assertTrue(result.size() == Math.min(number, pageSize));

			for (int j = 0; j < pageSize; j++) {

				int expectedNumber = offset + j;
				int gotNumber      = result.get(j).getIntProperty(key);

				System.out.println("expected: " + expectedNumber + ", got: " + gotNumber);
				assertTrue(gotNumber == expectedNumber);

			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test06SortByLong() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 43;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			String key                      = TestOne.Key.aLong.name();
			long offset                     = 10;
			long i                          = offset;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				node.setName(Long.toString(i));

//                              try {
//                                      Thread.sleep(1000L);
//                              } catch (InterruptedException ex) {
//                              }
				node.setProperty(key, i);

				i++;

				System.out.println(node.getStringProperty(AbstractNode.Key.name) + ": " + node.getLongProperty(key));

			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andType(type));

			// searchAttributes.add(Search.orExactProperty(TestOne.Key.aLong, "10"));
			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			String sortKey   = key;
			boolean sortDesc = false;
			int pageSize     = 20;
			int page         = 1;

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, null, SortField.LONG);

			for (GraphObject obj : result.getResults()) {

				System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ": " + obj.getLongProperty(key));
			}

			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });

			if (result.size() >= pageSize) {

				assertTrue(result.size() == pageSize);
			}

			for (int j = 0; j < pageSize; j++) {

				long expectedNumber = offset + j;
				long gotNumber      = result.get(j).getLongProperty(key);

				System.out.println("expected: " + expectedNumber + ", got: " + gotNumber);
				assertTrue(gotNumber == expectedNumber);

			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
