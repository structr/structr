/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

import org.apache.commons.lang.StringUtils;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.log.ReadLogCommand;
import org.structr.core.log.WriteLogCommand;

//~--- JDK imports ------------------------------------------------------------

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Tests for the {@link WriteLogCommand} and {@link ReadLogCommand}
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class LogCommandsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(LogCommandsTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01TestSequentialWriteRead() {

		try {

			int number        = 100;
			String logPageKey = "test1";
			long t0           = System.nanoTime();

			for (int i = 0; i < number; i++) {

				writeLogCommand.execute(logPageKey, new String[] { "foo" + i, "bar" });
			}

			long t1                     = System.nanoTime();
			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number / ((t1 - t0) / 1000000000.0);

			logger.log(Level.INFO, "Created {0} log entries in {1} seconds ({2} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });

			Map<String, Object> result = (Map<String, Object>) readLogCommand.execute(logPageKey);

			for (Entry<String, Object> entry : result.entrySet()) {

				String key = entry.getKey();
				Object val = entry.getValue();

				assertTrue(val instanceof String[]);

				String[] values = (String[]) val;

				// System.out.println(key + ": " + StringUtils.join(values, ","));

			}

			assertTrue(result.size() == number);

			for (int i = 0; i < number; i++) {

				writeLogCommand.execute(logPageKey, new String[] { "foo" + i, "bar" });
			}

			result = (Map<String, Object>) readLogCommand.execute(logPageKey);

			assertTrue(result.size() == 2 * number);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test02TestUnknownPageKey() {

		try {

			int number        = 100;
			String logPageKey = "test1";
			long t0           = System.nanoTime();

			for (int i = 0; i < number; i++) {

				writeLogCommand.execute(logPageKey, new String[] { "foo" + i, "bar" });
			}

			long t1                     = System.nanoTime();
			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number / ((t1 - t0) / 1000000000.0);

			logger.log(Level.INFO, "Created {0} log entries in {1} seconds ({2} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });

			logPageKey = "test2";

			Map<String, Object> result = (Map<String, Object>) readLogCommand.execute(logPageKey);

			assertTrue(result.isEmpty());

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
