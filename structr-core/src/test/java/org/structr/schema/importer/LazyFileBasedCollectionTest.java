package org.structr.schema.importer;

import org.junit.Test;

/**
 *
 * @author Christian Morgner
 */
public class LazyFileBasedCollectionTest {

	public LazyFileBasedCollectionTest() {
	}

	@Test
	public void testSomeMethod() {

		final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.currentTimeMillis() + ".lfc");

		for (int i=0; i<100; i++) {
			coll.add((long)i);
		}

		for (final Long val : coll) {
			System.out.println(val);
		}

		try {
			coll.close();

		} catch (Exception ex) {

		}

	}

}
