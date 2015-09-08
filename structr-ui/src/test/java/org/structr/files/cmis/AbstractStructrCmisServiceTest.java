package org.structr.files.cmis;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;

/**
 *
 * @author Christian Morgner
 */

public class AbstractStructrCmisServiceTest {

	@Test
	public void testPaging() {

		final List<Integer> source1 = Arrays.asList(new Integer[] { 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 } );

		final AbstractStructrCmisService mockService = new AbstractStructrCmisService(null) {
		};

		assertEquals("Invalid paging result",  2, mockService.applyPaging(source1, BigInteger.valueOf( 2), BigInteger.valueOf(2)).size());
		assertEquals("Invalid paging result",  4, mockService.applyPaging(source1, BigInteger.valueOf( 4), BigInteger.valueOf(4)).size());
		assertEquals("Invalid paging result",  6, mockService.applyPaging(source1, BigInteger.valueOf( 6), BigInteger.valueOf(6)).size());
		assertEquals("Invalid paging result",  8, mockService.applyPaging(source1, BigInteger.valueOf( 8), BigInteger.valueOf(2)).size());
		assertEquals("Invalid paging result", 10, mockService.applyPaging(source1, BigInteger.valueOf(10), BigInteger.valueOf(4)).size());
		assertEquals("Invalid paging result", 12, mockService.applyPaging(source1, BigInteger.valueOf(12), BigInteger.valueOf(6)).size());
		assertEquals("Invalid paging result", 14, mockService.applyPaging(source1, BigInteger.valueOf(14), BigInteger.valueOf(2)).size());
		assertEquals("Invalid paging result", 16, mockService.applyPaging(source1, BigInteger.valueOf(16), BigInteger.valueOf(4)).size());
		assertEquals("Invalid paging result", 14, mockService.applyPaging(source1, BigInteger.valueOf(18), BigInteger.valueOf(6)).size());
		assertEquals("Invalid paging result", 18, mockService.applyPaging(source1, BigInteger.valueOf(20), BigInteger.valueOf(2)).size());
		assertEquals("Invalid paging result", 16, mockService.applyPaging(source1, BigInteger.valueOf(22), BigInteger.valueOf(4)).size());
		assertEquals("Invalid paging result", 14, mockService.applyPaging(source1, BigInteger.valueOf(24), BigInteger.valueOf(6)).size());
		assertEquals("Invalid paging result", 12, mockService.applyPaging(source1, BigInteger.valueOf(20), BigInteger.valueOf(8)).size());
		assertEquals("Invalid paging result", 10, mockService.applyPaging(source1, BigInteger.valueOf(22), BigInteger.valueOf(10)).size());
		assertEquals("Invalid paging result",  8, mockService.applyPaging(source1, BigInteger.valueOf(24), BigInteger.valueOf(12)).size());
		assertEquals("Invalid paging result",  6, mockService.applyPaging(source1, BigInteger.valueOf(20), BigInteger.valueOf(14)).size());
		assertEquals("Invalid paging result",  4, mockService.applyPaging(source1, BigInteger.valueOf(22), BigInteger.valueOf(16)).size());
		assertEquals("Invalid paging result",  2, mockService.applyPaging(source1, BigInteger.valueOf(24), BigInteger.valueOf(18)).size());
		assertEquals("Invalid paging result",  0, mockService.applyPaging(source1, BigInteger.valueOf(20), BigInteger.valueOf(20)).size());
		assertEquals("Invalid paging result",  0, mockService.applyPaging(source1, BigInteger.valueOf(22), BigInteger.valueOf(22)).size());
		assertEquals("Invalid paging result",  0, mockService.applyPaging(source1, BigInteger.valueOf(24), BigInteger.valueOf(24)).size());

	}

}
