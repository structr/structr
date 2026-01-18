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
package org.structr.api.config;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class IntegerSettingTest {

	@Test
	public void testFromString() {

		final SettingsGroup testGroup = new SettingsGroup("testInt", "testInt");
		final IntegerSetting setting = new IntegerSetting(testGroup, "keyInt", 123);

		final Integer newValue = 4362;
		final Integer value    = 123;
		final Integer noValue  = -1;

		// test invalid values that must not throw an exception
		setting.fromString(null);
		assertEquals("Setting should retain default value when given a null value", value, setting.getValue());

		setting.fromString("");
		assertEquals("Setting should contain empty value when given an empty value", noValue, setting.getValue());

		setting.fromString("4362");
		assertEquals("Setting should contain new value when given a new value", newValue, setting.getValue());
	}
}
