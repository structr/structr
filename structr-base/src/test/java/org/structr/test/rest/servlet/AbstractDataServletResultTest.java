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
package org.structr.test.rest.servlet;

import org.structr.api.util.Iterables;
import org.structr.rest.servlet.AbstractDataServlet;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Mapping from a {@code RestMethodResult}'s non-graph-object result to the iterable that gets
 * serialized. Pure logic, no servlet container or database needed.
 *
 * This lives under org.structr.test rather than in the package it exercises: the structr jars are
 * signed (maven-jarsigner-plugin), so an unsigned test class sharing a package with signed
 * production classes is rejected by the JVM once both reach the same classloader. That happens in a
 * full-suite run but not when the class is run on its own, which is why it can look green in
 * isolation.
 */
public class AbstractDataServletResultTest {

	/**
	 * A handler that returns a bare RestMethodResult -- {@code DELETE /<Type>} being the
	 * common case -- has no content, no message and no non-graph-object result. That has to
	 * serialize as {@code "result": []}; wrapping the null produced {@code [ null ]}, a single
	 * null element that clients then have to filter out.
	 */
	@Test
	public void testNullResultBecomesAnEmptyIterable() {

		final Iterable<Object> result = AbstractDataServlet.resultIterable(null);

		assertTrue("a null result must serialize as an empty array, not [null]", Iterables.toList(result).isEmpty());
	}

	/** A single object is still a single-element result. */
	@Test
	public void testSingleObjectIsWrapped() {

		assertEquals(List.of("value"), Iterables.toList(AbstractDataServlet.resultIterable("value")));
	}

	/** An iterable result passes through as-is, including the empty and single-element cases. */
	@Test
	public void testIterableResultPassesThrough() {

		final List<Object> three = Arrays.asList("a", "b", "c");
		assertEquals(three, Iterables.toList(AbstractDataServlet.resultIterable(three)));

		assertEquals(List.of("only"), Iterables.toList(AbstractDataServlet.resultIterable(List.of("only"))));
		assertTrue(Iterables.toList(AbstractDataServlet.resultIterable(new ArrayList<>())).isEmpty());
	}

	/**
	 * A collection that genuinely contains a null keeps it: only the "no result at all" case
	 * collapses to empty, so a method returning a list with a null element is unaffected.
	 */
	@Test
	public void testNullInsideAnIterableIsPreserved() {

		final List<Object> withNull = Arrays.asList("a", null);

		assertEquals(withNull, Iterables.toList(AbstractDataServlet.resultIterable(withNull)));
	}
}
