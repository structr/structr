/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.geo;

import org.junit.Assert;
import org.structr.common.error.FrameworkException;

/**
 *
 */
public class LatLonToUTMFunctionTest extends StructrTest {

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