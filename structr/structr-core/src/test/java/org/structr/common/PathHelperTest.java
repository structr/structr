/*
 *  Copyright (C) 2011 Axel Morgner
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

import junit.framework.TestCase;

/**
 *
 * @author axel
 */
public class PathHelperTest extends TestCase {

	public PathHelperTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test of find method, of class PathHelper.
//	 */
//	public void testFind() {
//		System.out.println("find");
//		String path = "";
//		PathHelper instance = new PathHelper();
//		AbstractNode expResult = null;
//		AbstractNode result = instance.find(path);
//		assertEquals(expResult, result);
//		// TODO review the generated test code and remove the default call to fail.
//		fail("The test case is a prototype.");
//	}

	/**
	 * Test of getRelativeNodePath method, of class PathHelper.
	*/
	public void testGetRelativeNodePath() {
		
		/*44
		System.out.println("getNewRelativePath");

		String basePath = "/a/b";
		String newPath = "/a/b/c";
		String expResult = "c";
		String result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/b/c";
		newPath = "/a/b/c";
		expResult = ".";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/b/c";
		newPath = "/a/b";
		expResult = "..";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/b/c/";
		newPath = "/a/b";
		expResult = "..";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/b/c";
		newPath = "/a/b/x";
		expResult = "../x";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/b/c";
		newPath = "/a";
		expResult = "../..";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a";
		newPath = "/a/b/c";
		expResult = "b/c";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/";
		newPath = "/a/b/c";
		expResult = "a/b/c";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/";
		newPath = "/";
		expResult = ".";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/";
		newPath = "";
		expResult = "";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/x/b";
		newPath = "/a/b/x";
		expResult = "../../b/x";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/../b";
		newPath = "/a/b";
		expResult = "../a/b";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);

		basePath = "/a/b/../../x";
		newPath = "/a/b";
		expResult = "../a/b";
		result = PathHelper.getRelativeNodePath(basePath, newPath);
		//System.out.println(basePath + " -> " + newPath + " => " + result + " expected " + expResult);
		assertEquals(expResult, result);
		 */
	}
}
