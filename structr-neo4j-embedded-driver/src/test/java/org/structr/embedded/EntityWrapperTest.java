/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.embedded;

import org.neo4j.values.storable.BooleanArray;
import org.neo4j.values.storable.DoubleArray;
import org.neo4j.values.storable.LongArray;
import org.neo4j.values.storable.TextArray;
import org.neo4j.values.storable.Values;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * The conversion of property values before they are written to and after they are read from Neo4j.
 */
public class EntityWrapperTest {

	@Test
	public void testWrapperArraysBecomePrimitiveArrays() {

		final Object doubles = EntityWrapper.convertForStorage(new Double[] { 0.25, -1.5, 3.0 });

		assertTrue(doubles instanceof double[], "Double[] is stored as double[], got " + doubles.getClass());
		assertEquals((double[]) doubles, new double[] { 0.25, -1.5, 3.0 });

		assertTrue(EntityWrapper.convertForStorage(new Long[] { 1L, 2L }) instanceof long[]);
		assertTrue(EntityWrapper.convertForStorage(new Integer[] { 1, 2 }) instanceof int[]);
		assertTrue(EntityWrapper.convertForStorage(new Float[] { 1.0f }) instanceof float[]);
		assertTrue(EntityWrapper.convertForStorage(new Boolean[] { true, false }) instanceof boolean[]);
		assertTrue(EntityWrapper.convertForStorage(new Character[] { 'a' }) instanceof char[]);
		assertTrue(EntityWrapper.convertForStorage(new Short[] { 1 }) instanceof short[]);
		assertTrue(EntityWrapper.convertForStorage(new Byte[] { 1 }) instanceof byte[]);
	}

	@Test
	public void testOtherArraysAreUnchanged() {

		final String[] strings = { "a", "b" };
		final double[] doubles = { 1.0 };

		assertSame(EntityWrapper.convertForStorage(strings), strings);
		assertSame(EntityWrapper.convertForStorage(doubles), doubles);
	}

	@Test
	public void testArraysWithNullElementsAreUnchanged() {

		final Double[] withNull = { 1.0, null };

		assertSame(EntityWrapper.convertForStorage(withNull), withNull, "Neo4j reports null elements itself");
	}

	@Test
	public void testListsBecomeTypedArrays() {

		final Object doubles = EntityWrapper.convertForStorage(List.of(0.5, 1.5));

		assertTrue(doubles instanceof double[], "A list of Doubles is stored as double[], got " + doubles.getClass());
		assertEquals((double[]) doubles, new double[] { 0.5, 1.5 });

		final Object longs = EntityWrapper.convertForStorage(List.of(1L, 2L));

		assertTrue(longs instanceof long[]);

		final Object strings = EntityWrapper.convertForStorage(List.of("a", "b"));

		assertTrue(strings instanceof String[]);
		assertEquals((String[]) strings, new String[] { "a", "b" });

		final Object nulls = EntityWrapper.convertForStorage(Arrays.asList(null, null));

		assertTrue(nulls instanceof String[], "A list of nulls becomes a String[], as before");
	}

	@Test
	public void testEmptyListsAndScalarsAreUnchanged() {

		final List<Object> empty = List.of();

		assertSame(EntityWrapper.convertForStorage(empty), empty);
		assertEquals(EntityWrapper.convertForStorage(1.5), 1.5);
		assertNull(EntityWrapper.convertForStorage(null));
	}

	@Test
	public void testNeo4jAcceptsTheConvertedValues() {

		assertTrue(Values.of(EntityWrapper.convertForStorage(new Double[] { 0.25 })) instanceof DoubleArray);
		assertTrue(Values.of(EntityWrapper.convertForStorage(List.of(0.25, 0.5))) instanceof DoubleArray);
		assertTrue(Values.of(EntityWrapper.convertForStorage(List.of(1L))) instanceof LongArray);
		assertTrue(Values.of(EntityWrapper.convertForStorage(List.of(true))) instanceof BooleanArray);
		assertTrue(Values.of(EntityWrapper.convertForStorage(List.of("a"))) instanceof TextArray);
	}

	@Test
	public void testRoundtripRestoresWrapperArrays() {

		final Object stored   = EntityWrapper.convertForStorage(new Double[] { 0.25, -1.5 });
		final Object restored = EntityWrapper.revertFromStorage(stored);

		assertTrue(restored instanceof Double[]);
		assertEquals((Double[]) restored, new Double[] { 0.25, -1.5 });
	}
}
