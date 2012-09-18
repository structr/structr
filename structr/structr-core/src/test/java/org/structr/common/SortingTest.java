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

import java.util.Collections;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.SortField;

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

			boolean includeDeletedAndHidden = true;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 43;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			String name;
			int i = 10;

			Collections.shuffle(nodes);
			
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
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
			assertTrue(result.size() == pageSize);
			assertTrue(result.get(0).getStringProperty(AbstractNode.Key.name).equals("10"));
			assertTrue(result.get(1).getStringProperty(AbstractNode.Key.name).equals("11"));
			assertTrue(result.get(2).getStringProperty(AbstractNode.Key.name).equals("12"));
			assertTrue(result.get(3).getStringProperty(AbstractNode.Key.name).equals("13"));
			assertTrue(result.get(4).getStringProperty(AbstractNode.Key.name).equals("14"));
			assertTrue(result.get(5).getStringProperty(AbstractNode.Key.name).equals("15"));
			assertTrue(result.get(6).getStringProperty(AbstractNode.Key.name).equals("16"));
			assertTrue(result.get(7).getStringProperty(AbstractNode.Key.name).equals("17"));
			assertTrue(result.get(8).getStringProperty(AbstractNode.Key.name).equals("18"));
			assertTrue(result.get(9).getStringProperty(AbstractNode.Key.name).equals("19"));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test02SortByNameDesc() {

		try {

			boolean includeDeletedAndHidden = true;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 43;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			String name;
			int i = 10;

			Collections.shuffle(nodes);

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
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
			assertTrue(result.size() == pageSize);
			assertTrue(result.get(0).getStringProperty(AbstractNode.Key.name).equals("52"));
			assertTrue(result.get(1).getStringProperty(AbstractNode.Key.name).equals("51"));
			assertTrue(result.get(2).getStringProperty(AbstractNode.Key.name).equals("50"));
			assertTrue(result.get(3).getStringProperty(AbstractNode.Key.name).equals("49"));
			assertTrue(result.get(4).getStringProperty(AbstractNode.Key.name).equals("48"));
			assertTrue(result.get(5).getStringProperty(AbstractNode.Key.name).equals("47"));
			assertTrue(result.get(6).getStringProperty(AbstractNode.Key.name).equals("46"));
			assertTrue(result.get(7).getStringProperty(AbstractNode.Key.name).equals("45"));
			assertTrue(result.get(8).getStringProperty(AbstractNode.Key.name).equals("44"));
			assertTrue(result.get(9).getStringProperty(AbstractNode.Key.name).equals("43"));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03SortByDate() {

		try {

			boolean includeDeletedAndHidden = true;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 43;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			String name;
			int i = 10;

			Collections.shuffle(nodes);

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
			boolean sortDesc = false;
			int pageSize     = 10;
			int page         = 1;

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

//                      for (GraphObject obj : result.getResults()) {
//                              
//                              System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ", " + obj.getDateProperty(AbstractNode.Key.lastModifiedDate));
//                              
//                      }
			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
			assertTrue(result.size() == pageSize);
			assertTrue(result.get(0).getStringProperty(AbstractNode.Key.name).equals("10"));
			assertTrue(result.get(1).getStringProperty(AbstractNode.Key.name).equals("11"));
			assertTrue(result.get(2).getStringProperty(AbstractNode.Key.name).equals("12"));
			assertTrue(result.get(3).getStringProperty(AbstractNode.Key.name).equals("13"));
			assertTrue(result.get(4).getStringProperty(AbstractNode.Key.name).equals("14"));
			assertTrue(result.get(5).getStringProperty(AbstractNode.Key.name).equals("15"));
			assertTrue(result.get(6).getStringProperty(AbstractNode.Key.name).equals("16"));
			assertTrue(result.get(7).getStringProperty(AbstractNode.Key.name).equals("17"));
			assertTrue(result.get(8).getStringProperty(AbstractNode.Key.name).equals("18"));
			assertTrue(result.get(9).getStringProperty(AbstractNode.Key.name).equals("19"));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test04SortByDateDesc() {

		try {

			boolean includeDeletedAndHidden = true;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 43;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			String name;
			int i = 10;
			
			Collections.shuffle(nodes);

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

			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

			for (GraphObject obj : result.getResults()) {

				System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ", " + obj.getDateProperty(AbstractNode.Key.lastModifiedDate));
			}

			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
			assertTrue(result.getRawResultCount() == number);
			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
			assertTrue(result.size() == pageSize);
			assertTrue(result.get(0).getStringProperty(AbstractNode.Key.name).equals("52"));
			assertTrue(result.get(1).getStringProperty(AbstractNode.Key.name).equals("51"));
			assertTrue(result.get(2).getStringProperty(AbstractNode.Key.name).equals("50"));
			assertTrue(result.get(3).getStringProperty(AbstractNode.Key.name).equals("49"));
			assertTrue(result.get(4).getStringProperty(AbstractNode.Key.name).equals("48"));
			assertTrue(result.get(5).getStringProperty(AbstractNode.Key.name).equals("47"));
			assertTrue(result.get(6).getStringProperty(AbstractNode.Key.name).equals("46"));
			assertTrue(result.get(7).getStringProperty(AbstractNode.Key.name).equals("45"));
			assertTrue(result.get(8).getStringProperty(AbstractNode.Key.name).equals("44"));
			assertTrue(result.get(9).getStringProperty(AbstractNode.Key.name).equals("43"));

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

//	public void test05SortByInt() {
//
//		try {
//
//			boolean includeDeletedAndHidden = true;
//			boolean publicOnly              = false;
//			String type                     = TestOne.class.getSimpleName();
//			int number                      = 130;
//			List<AbstractNode> nodes        = this.createTestNodes(type, number);
//			String key                      = TestOne.Key.anInt.name();
//			int i                           = 10;
//
//			Collections.shuffle(nodes);
//			
//			for (AbstractNode node : nodes) {
//
//				node.setName(Integer.toString(i));
//				
////                              try {
////                                      Thread.sleep(1000L);
////                              } catch (InterruptedException ex) {
////                              }
//				node.setProperty(key, i);
//
//				i++;
//			}
//
//			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
//
//			searchAttributes.add(Search.andType(type));
//
//			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);
//
//			assertTrue(result.size() == number);
//
//			String sortKey   = key;
//			boolean sortDesc = false;
//			int pageSize     = 10;
//			int page         = 1;
//
//			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, SortField.INT);
//
//			for (GraphObject obj : result.getResults()) {
//
//				System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ": " + obj.getIntProperty(key));
//			}
//
//			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
//			assertTrue(result.getRawResultCount() == number);
//			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
//			assertTrue(result.size() == pageSize);
//			assertTrue(result.get(0).getIntProperty(key) == 10);
//			assertTrue(result.get(1).getIntProperty(key) == 11);
//			assertTrue(result.get(2).getIntProperty(key) == 12);
//			assertTrue(result.get(3).getIntProperty(key) == 13);
//			assertTrue(result.get(4).getIntProperty(key) == 14);
//			assertTrue(result.get(5).getIntProperty(key) == 15);
//			assertTrue(result.get(6).getIntProperty(key) == 16);
//			assertTrue(result.get(7).getIntProperty(key) == 17);
//			assertTrue(result.get(8).getIntProperty(key) == 18);
//			assertTrue(result.get(9).getIntProperty(key) == 19);
//
//		} catch (FrameworkException ex) {
//
//			logger.log(Level.SEVERE, ex.toString());
//			fail("Unexpected exception");
//
//		}
//
//	}
//	public void test06SortByLong() {
//
//		try {
//
//			boolean includeDeletedAndHidden = true;
//			boolean publicOnly              = false;
//			String type                     = TestOne.class.getSimpleName();
//			int number                      = 13;
//			List<AbstractNode> nodes        = this.createTestNodes(type, number);
//			String key                      = TestOne.Key.aLong.name();
//			long i                          = 10;
//
//			for (AbstractNode node : nodes) {
//
//				node.setName(Long.toString(i));
//				
////                              try {
////                                      Thread.sleep(1000L);
////                              } catch (InterruptedException ex) {
////                              }
//				node.setProperty(key, i);
//
//				i++;
//			}
//
//			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
//
//			searchAttributes.add(Search.andType(type));
//			//searchAttributes.add(Search.orExactProperty(TestOne.Key.aLong, "10"));
//				
//
//			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);
//
//			assertTrue(result.size() == number);
//
//			String sortKey   = key;
//			boolean sortDesc = false;
//			int pageSize     = 10;
//			int page         = 1;
//
//			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, SortField.LONG);
//
//			for (GraphObject obj : result.getResults()) {
//
//				System.out.println(obj.getStringProperty(AbstractNode.Key.name) + ": " + obj.getLongProperty(key));
//			}
//
//			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
//			assertTrue(result.getRawResultCount() == number);
//			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
//			assertTrue(result.size() == pageSize);
//			assertTrue(result.get(0).getLongProperty(key) == 10);
//			assertTrue(result.get(1).getLongProperty(key) == 11);
//			assertTrue(result.get(2).getLongProperty(key) == 12);
//			assertTrue(result.get(3).getLongProperty(key) == 13);
//			assertTrue(result.get(4).getLongProperty(key) == 14);
//			assertTrue(result.get(5).getLongProperty(key) == 15);
//			assertTrue(result.get(6).getLongProperty(key) == 16);
//			assertTrue(result.get(7).getLongProperty(key) == 17);
//			assertTrue(result.get(8).getLongProperty(key) == 18);
//			assertTrue(result.get(9).getLongProperty(key) == 19);
//
//		} catch (FrameworkException ex) {
//
//			logger.log(Level.SEVERE, ex.toString());
//			fail("Unexpected exception");
//
//		}
//
//	}

}
