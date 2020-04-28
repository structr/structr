/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.test.files.cmis.wrapper;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.structr.files.cmis.wrapper.CMISPagingListWrapper;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;


/**
 *
 *
 */


public class CMISPagingListWrapperTest {

	@Test
	public void testPaging() {

		final List<Integer> source1 = Arrays.asList(new Integer[] { 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 } );

		assertEquals("Invalid paging result",  2, new CMISPagingListWrapper<>(source1, BigInteger.valueOf( 2), BigInteger.valueOf(2)).getPagedList().size());
		assertEquals("Invalid paging result",  4, new CMISPagingListWrapper<>(source1, BigInteger.valueOf( 4), BigInteger.valueOf(4)).getPagedList().size());
		assertEquals("Invalid paging result",  6, new CMISPagingListWrapper<>(source1, BigInteger.valueOf( 6), BigInteger.valueOf(6)).getPagedList().size());
		assertEquals("Invalid paging result",  8, new CMISPagingListWrapper<>(source1, BigInteger.valueOf( 8), BigInteger.valueOf(2)).getPagedList().size());
		assertEquals("Invalid paging result", 10, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(10), BigInteger.valueOf(4)).getPagedList().size());
		assertEquals("Invalid paging result", 12, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(12), BigInteger.valueOf(6)).getPagedList().size());
		assertEquals("Invalid paging result", 14, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(14), BigInteger.valueOf(2)).getPagedList().size());
		assertEquals("Invalid paging result", 16, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(16), BigInteger.valueOf(4)).getPagedList().size());
		assertEquals("Invalid paging result", 14, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(18), BigInteger.valueOf(6)).getPagedList().size());
		assertEquals("Invalid paging result", 18, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(20), BigInteger.valueOf(2)).getPagedList().size());
		assertEquals("Invalid paging result", 16, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(22), BigInteger.valueOf(4)).getPagedList().size());
		assertEquals("Invalid paging result", 14, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(24), BigInteger.valueOf(6)).getPagedList().size());
		assertEquals("Invalid paging result", 12, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(20), BigInteger.valueOf(8)).getPagedList().size());
		assertEquals("Invalid paging result", 10, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(22), BigInteger.valueOf(10)).getPagedList().size());
		assertEquals("Invalid paging result",  8, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(24), BigInteger.valueOf(12)).getPagedList().size());
		assertEquals("Invalid paging result",  6, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(20), BigInteger.valueOf(14)).getPagedList().size());
		assertEquals("Invalid paging result",  4, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(22), BigInteger.valueOf(16)).getPagedList().size());
		assertEquals("Invalid paging result",  2, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(24), BigInteger.valueOf(18)).getPagedList().size());
		assertEquals("Invalid paging result",  0, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(20), BigInteger.valueOf(20)).getPagedList().size());
		assertEquals("Invalid paging result",  0, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(22), BigInteger.valueOf(22)).getPagedList().size());
		assertEquals("Invalid paging result",  0, new CMISPagingListWrapper<>(source1, BigInteger.valueOf(24), BigInteger.valueOf(24)).getPagedList().size());


	}
}
