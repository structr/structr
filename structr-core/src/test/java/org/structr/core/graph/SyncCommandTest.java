/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.graph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.structr.common.StructrTest;
import org.structr.core.entity.TestOne;

/**
 *
 * @author Christian Morgner
 */


public class SyncCommandTest extends StructrTest {

	private static final Byte[] BYTE_TEST           = new Byte[] { Byte.MIN_VALUE, -20, -1, 0, 1, 20, Byte.MAX_VALUE };
	private static final String BYTE_STRING         = "00170114-128 0113-20 0112-1 01110 01111 011220 0113127  ";

	private static final Short[] SHORT_TEST         = new Short[] { Short.MIN_VALUE, -20, -1, 0, 1, 20, Short.MAX_VALUE };
	private static final String SHORT_STRING        = "02170316-32768 0313-20 0312-1 03110 03111 031220 031532767  ";

	private static final Integer[] INTEGER_TEST     = new Integer[] { Integer.MIN_VALUE, -20, -1, 0, 1, 20, Integer.MAX_VALUE };
	private static final String INTEGER_STRING      = "041705211-2147483648 0513-20 0512-1 05110 05111 051220 052102147483647  ";

	private static final Long[] LONG_TEST           = new Long[] { Long.MIN_VALUE, -20L, -1L, 0L, 1L, 20L, Long.MAX_VALUE };
	private static final String LONG_STRING         = "061707220-9223372036854775808 0713-20 0712-1 07110 07111 071220 072199223372036854775807  ";

	private static final Float[] FLOAT_TEST         = new Float[] { Float.MIN_VALUE, -20.0f, -1.0f, 0.0f, 1.0f, 20.0f, Float.MAX_VALUE };
	private static final String FLOAT_STRING        = "081709171.4E-45 0915-20.0 0914-1.0 09130.0 09131.0 091420.0 092123.4028235E38  ";

	private static final Double[] DOUBLE_TEST       = new Double[] { Double.MIN_VALUE, -20.0, -1.0, 0.0, 1.0, 20.0, Double.MAX_VALUE };
	private static final String DOUBLE_STRING       = "101711184.9E-324 1115-20.0 1114-1.0 11130.0 11131.0 111420.0 112221.7976931348623157E308  ";

	private static final Character[] CHARACTER_TEST = new Character[] { 'a', 'b', 'z', 'A', 'B', 'Z' };
	private static final String CHARACTER_STRING    = "12161311a 1311b 1311z 1311A 1311B 1311Z  ";

	private static final String[] STRING_TEST       = new String[] { "Test", "This is Test", "\n", "\"", "\\\"", "\t", "\n\n\n\n\n\r" };
	private static final String STRING_STRING       = "14171514Test 15212This is Test 1511\n 1511\" 1512\\\" 1511\t 1516\n\n\n\n\n\r  ";

	private static final Boolean[] BOOLEAN_TEST     = new Boolean[] { true, false };
	private static final String BOOLEAN_STRING      = "16121714true 1715false  ";


	public void testExportImport() {

		final String string1 = "This is a simple test string.";
		final String string2 = "This is a simple test string with\na newline.";
		final String string3 = "This is a simple\ntest\nstring\nwith\nmultiple\nnewlines.";
		final String string4 = "sjdhf lkjshd\tjalksjdfas \"sjdfajsdfb'sdfjkasbdfhja\t\t\n\nskdfajkshfasd\n\n\n\n\r\t\t\r\n\r\n\r\nsfasdfas";

		try {

			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);

			// 1. create data and export
			try (final Tx tx = app.tx()) {

				final TestOne test1 = createTestNode(TestOne.class);
				final TestOne test2 = createTestNode(TestOne.class);
				final TestOne test3 = createTestNode(TestOne.class);
				final TestOne test4 = createTestNode(TestOne.class);

				test1.setProperty(TestOne.aString, string1);
				test1.setProperty(TestOne.anInt, 1);

				test2.setProperty(TestOne.aString, string2);
				test2.setProperty(TestOne.anInt, 2);

				test3.setProperty(TestOne.aString, string3);
				test3.setProperty(TestOne.anInt, 3);

				test4.setProperty(TestOne.aString, string4);
				test4.setProperty(TestOne.anInt, 4);

				SyncCommand.exportToStream(
					outputStream,
					app.nodeQuery(TestOne.class).getAsList(),
					app.relationshipQuery(RelationshipInterface.class).getAsList(),
					null,
					false
				);

				tx.success();
			}


			// 2. clear database
			try (final Tx tx = app.tx()) {

				for (final TestOne test : app.nodeQuery(TestOne.class).getAsList()) {

					app.delete(test);
				}

				tx.success();
			}

			// 3. verify that database is empty
			try (final Tx tx = app.tx()) {

				assertEquals("Database should contain not TestOne entities.", 0, app.nodeQuery(TestOne.class).getResult().size());
				tx.success();
			}


			// 4. import data again
			try (final Tx tx = app.tx()) {

				SyncCommand.importFromStream(
					app.getGraphDatabaseService(),
					securityContext,
					new ByteArrayInputStream(outputStream.toByteArray()),
					true
				);

				tx.success();

			}

			// 5. check result
			try (final Tx tx = app.tx()) {

				final TestOne test1 = app.nodeQuery(TestOne.class).and(TestOne.anInt, 1).getFirst();
				final TestOne test2 = app.nodeQuery(TestOne.class).and(TestOne.anInt, 2).getFirst();
				final TestOne test3 = app.nodeQuery(TestOne.class).and(TestOne.anInt, 3).getFirst();
				final TestOne test4 = app.nodeQuery(TestOne.class).and(TestOne.anInt, 4).getFirst();

				assertEquals("Strings from exported and re-imported data should be equal", string1, test1.getProperty(TestOne.aString));
				assertEquals("Strings from exported and re-imported data should be equal", string2, test2.getProperty(TestOne.aString));
				assertEquals("Strings from exported and re-imported data should be equal", string3, test3.getProperty(TestOne.aString));
				assertEquals("Strings from exported and re-imported data should be equal", string4, test4.getProperty(TestOne.aString));

				tx.success();
			}


		} catch (Throwable fex) {

			fex.printStackTrace();

			fail("Unexpected exception.");
		}
	}

	public void testSerializer() throws IOException {

		// 00, 01: byte[], byte
		assertEquals(BYTE_STRING, serialize(BYTE_TEST));

		// 02, 03: short[], short
		assertEquals(SHORT_STRING, serialize(SHORT_TEST));

		// 04, 05: int[], int
		assertEquals(INTEGER_STRING, serialize(INTEGER_TEST));

		// 06, 07: long[], long
		assertEquals(LONG_STRING, serialize(LONG_TEST));

		// 08, 09: float[], float
		assertEquals(FLOAT_STRING, serialize(FLOAT_TEST));

		// 10, 11: double[], double
		assertEquals(DOUBLE_STRING, serialize(DOUBLE_TEST));

		// 12, 13: Character[], Character
		assertEquals(CHARACTER_STRING, serialize(CHARACTER_TEST));

		// 14, 15: String[], String
		assertEquals(STRING_STRING, serialize(STRING_TEST));

		// 16, 17: Boolean[], Boolean
		assertEquals(BOOLEAN_STRING, serialize(BOOLEAN_TEST));
	}

	public void testDeserializer() {

		// 00, 01: byte[], byte
		assertTrue(equal(BYTE_TEST, (Object[])deserialize(BYTE_STRING)));

		// 02, 03: short[], short
		assertTrue(equal(SHORT_TEST, (Object[])deserialize(SHORT_STRING)));

		// 04, 05: int[], int
		assertTrue(equal(INTEGER_TEST, (Object[])deserialize(INTEGER_STRING)));

		// 06, 07: long[], long
		assertTrue(equal(LONG_TEST, (Object[])deserialize(LONG_STRING)));

		// 08, 09: float[], float
		assertTrue(equal(FLOAT_TEST, (Object[])deserialize(FLOAT_STRING)));

		// 10, 11: double[], double
		assertTrue(equal(DOUBLE_TEST, (Object[])deserialize(DOUBLE_STRING)));

		// 12, 13: Character[], Character
		assertTrue(equal(CHARACTER_TEST, (Object[])deserialize(CHARACTER_STRING)));

		// 14, 15: String[], String
		assertTrue(equal(STRING_TEST, (Object[])deserialize(STRING_STRING)));

		// 16, 17: Boolean[], Boolean
		assertTrue(equal(BOOLEAN_TEST, (Object[])deserialize(BOOLEAN_STRING)));

	}

	private boolean equal(Object[] source, Object[] target) {
		return Arrays.equals(source, target);
	}


	private String serialize(final Object obj) throws IOException {

		final ByteArrayOutputStream os     = new ByteArrayOutputStream();

		SyncCommand.serialize(os, obj);

		return os.toString("utf-8");
	}

	private Object deserialize(final String source) {

		try {
			return SyncCommand.deserialize(new ByteArrayInputStream(source.getBytes("utf-8")));

		} catch (IOException ioex) {
			throw new IllegalStateException("WTF?");
		}
	}
}
