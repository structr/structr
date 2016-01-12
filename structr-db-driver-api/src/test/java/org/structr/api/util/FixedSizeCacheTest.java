package org.structr.api.util;

import org.junit.Assert;

/**
 *
 */
public class FixedSizeCacheTest {

	public FixedSizeCacheTest() {
	}

	@org.junit.Test
	public void testFixedSizeCache() {

		final FixedSizeCache<Long, Long> test = new FixedSizeCache<>(10);

		for (int i=0; i<100; i++) {

			final Long value = Long.valueOf(i);
			test.put(value, value);
		}

		Assert.assertEquals("Invalid FixedSizeCache size", 10, test.size());

		// check that only the eldest entries are kept
		for (int i=0; i<90; i++) {

			final Long value = Long.valueOf(i);
			Assert.assertNull("Invalid FixedSizeCache contents", test.get(value));
		}

		// check that only the eldest entries are kept
		for (int i=90; i<100; i++) {

			final Long value = Long.valueOf(i);
			Assert.assertEquals("Invalid FixedSizeCache contents", test.get(value), value);
		}

	}
}
