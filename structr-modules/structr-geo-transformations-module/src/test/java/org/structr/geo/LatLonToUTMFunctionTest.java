/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.geo;

import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;

/**
 *
 */
public class LatLonToUTMFunctionTest {

	@Test
	public void testLatLonToUTM() {

		final LatLonToUTMFunction func = new LatLonToUTMFunction();

		try {

			final Object result1 = func.apply(null, null, new Object[] { 53.85499997165232, 8.081674915658844 });
			Assert.assertEquals("Invalid UTM conversion result", "32U 439596 5967780" , result1);

			final Object result2 = func.apply(null, null, new Object[] { 51.319997116243364, 7.49998773689121 });
			Assert.assertEquals("Invalid UTM conversion result", "32U 395473 5686479", result2);

			final Object result3 = func.apply(null, null, new Object[] { -38.96442577579118, 7.793498600057568 });
			Assert.assertEquals("Invalid UTM conversion result", "32H 395473 5686479", result3);

			final Object result4 = func.apply(null, null, new Object[] { 51.319997116243364, -166.5000122631088});
			Assert.assertEquals("Invalid UTM conversion result", "3U 395473 5686479", result4);

			final Object result5 = func.apply(null, null, new Object[] { -36.59789213337618, -164.5312529421211 });
			Assert.assertEquals("Invalid UTM conversion result", "3H 541926 5949631", result5);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

	}
}