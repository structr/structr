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
package org.structr.test.script;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.*;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.*;
import org.structr.core.function.*;
import org.structr.core.graph.Tx;
import org.structr.core.traits.definitions.*;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

public class ScriptingTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(ScriptingTest.class.getName());

	@Test
	public void testNoMoreSnakeCaseFunctions() {

		try (final Tx tx = app.tx()) {

			final Set<String> functionNamesWithUnderscore = Functions.getNames().stream().filter(name -> name.contains("_")).collect(Collectors.toSet());

			if (!functionNamesWithUnderscore.isEmpty()) {
				Assert.fail("Function names can not contain underscores anymore: " + functionNamesWithUnderscore);
			}

			tx.success();

		} catch (FrameworkException e) {

			e.printStackTrace();
			Assert.fail("Unexpected exception");
		}
	}
}