/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.schema.importer.LazyFileBasedLongCollection;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class LazyFileBasedLongCollectionTest {

	private static final Logger logger = LoggerFactory.getLogger(LazyFileBasedLongCollectionTest.class);

	@Test
	public void testCollection() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());

			for (int i=0; i<100; i++) {
				coll.add((long)i);
			}

			long test = 0;

			for (final Long val : coll) {
				assertEquals("Invalid value in collection", test++, val.longValue());
			}


		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testValues() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());

			coll.add(Long.MIN_VALUE);
			coll.add(Long.MAX_VALUE);

			final Object[] arr = coll.toArray();

			assertEquals("Invalid toArray() result.", 2, arr.length);
			assertEquals("Invalid value in collection", Long.MIN_VALUE, arr[0]);
			assertEquals("Invalid value in collection", Long.MAX_VALUE, arr[1]);

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSize() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());

			final int count = new Random().nextInt(100);

			for (int i=0; i<count; i++) {
				coll.add((long)i);
			}

			assertEquals("Invalid size() result", count, coll.size());

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testIsEmpty() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid isEmpty() result", true, coll.isEmpty());

			coll.add(1L);

			assertEquals("Invalid isEmpty() result", false, coll.isEmpty());

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testContains() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());


		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testToArray1() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());

			for (int i=0; i<10; i++) {
				coll.add((long)i);
			}

			final Object[] arr = coll.toArray();

			assertNotNull("Invalid toArray() result", arr);
			assertEquals("Invalid toArray() result", 10, arr.length);

			long test = 0;

			for (final Long val : coll) {
				assertEquals("Invalid value in collection", test++, val.longValue());
			}

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testToArray2() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());

			for (int i=0; i<10; i++) {
				coll.add((long)i);
			}

			final Long[] arr = coll.toArray(new Long[0]);

			assertNotNull("Invalid toArray() result", arr);
			assertEquals("Invalid toArray() result", 10, arr.length);

			long test = 0;

			for (final Long val : coll) {
				assertEquals("Invalid value in collection", test++, val.longValue());
			}

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testAdd() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());

			coll.add(0L);

			assertEquals("Invalid add() result", 1, coll.size());
			assertEquals("Invalid add() result", false, coll.isEmpty());

			long test = 0;

			for (final Long val : coll) {
				assertEquals("Invalid value in collection", test++, val.longValue());
			}

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testAddAll() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			final List<Long> list = new ArrayList<>();
			for (int i=0; i<10; i++) {
				list.add((long)i);
			}

			coll.addAll(list);

			assertEquals("Invalid addAll() result", 10, coll.size());
			assertEquals("Invalid addAll() result", false, coll.isEmpty());

			long test = 0;

			for (final Long val : coll) {
				assertEquals("Invalid value in collection", test++, val.longValue());
			}

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testClear() {

		try (final LazyFileBasedLongCollection coll = new LazyFileBasedLongCollection("/tmp/" + System.nanoTime() + ".lfc")) {

			assertEquals("Invalid test prerequisite", true, coll.isEmpty());
			coll.add(1L);
			assertEquals("Invalid test prerequisite", false, coll.isEmpty());

			coll.clear();

			assertEquals("Invalid clear() result", true, coll.isEmpty());

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}
}
