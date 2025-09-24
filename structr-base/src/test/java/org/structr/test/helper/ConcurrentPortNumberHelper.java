package org.structr.test.helper;/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConcurrentPortNumberHelper {

	public static int getNextPortNumber(final Class clazz) {

		final Logger logger = LoggerFactory.getLogger(clazz);

		// allow override via system property (-DhttpPort=...)
		if (System.getProperty("httpPort") != null) {

			final int port = Integer.parseInt(System.getProperty("httpPort"));

			logger.info("HTTP port assignment overridden by system property! Value is {}", port);

			return port;
		}

		return getTimeBasedPortNumber(Calendar.getInstance());
	}

	private static int getTimeBasedPortNumber(final Calendar calendar) {

		// Let's try something different: map the current minute, second and millisecond to a unique
		// port number if possible. Range is 8875 to 65500 => 56625 ports. If we assume that no test
		// runs longer than 5 minutes, we can map the port range to the time.

		final int max          = 65500;
		final int min          = 8875;
		final double range     = max - min;

		final int minute        = calendar.get(Calendar.MINUTE);
		final int second        = calendar.get(Calendar.SECOND);
		final int millisecond   = calendar.get(Calendar.MILLISECOND);

		final double time       = (minute % 5) * 60000 + second * 1000 + millisecond;
		final int port          = (int)Math.rint((time / 300000.0) * range);

		System.out.println("!!!!! allocating port " + port + " for " + time);

		return port + min;
	}

	public static final void main(final String[] args) {

		final DateFormat format       = new SimpleDateFormat("HH:mm:ss.SSS");
		final Calendar calendar       = Calendar.getInstance();
		final Map<Integer, Long> test = new LinkedHashMap<>();

		calendar.set(2025,0, 1,0, 0, 0);

		for (int i=0; i<10000; i++) {

			final int port = getTimeBasedPortNumber(calendar);
			final long time = calendar.getTimeInMillis();

			System.out.println(format.format(time) + ": " + port);

			calendar.add(Calendar.MILLISECOND, (int)(Math.random() * 1000) + 1);

			final Long prev = test.put(port, time);
			if (prev != null) {

				System.out.println("COLLISION of " + format.format(prev) + " and " + format.format(time) + " leading to port number " + port);
			}
		}
	}


	/*
	public static int getNextPortNumber(final Class clazz) {

		final Logger logger = LoggerFactory.getLogger(clazz);

		// allow override via system property (-DhttpPort=...)
		if (System.getProperty("httpPort") != null) {

			final int port = Integer.parseInt(System.getProperty("httpPort"));

			logger.info("HTTP port assignment overridden by system property! Value is {}", port);

			return port;
		};

		// use locked file to store last used port
		final String fileName = "/tmp/structr.test.port.lock";
		final int max         = 65500;
		final int min         = 8875;
		int port              = min;
		int attempts          = 0;

		// try again if an error occurs
		while (attempts++ < 5) {

			try (final RandomAccessFile raf = new RandomAccessFile(fileName, "rwd")) {

				try (final FileLock lock = raf.getChannel().lock()) {

					if (raf.length() > 0) {

						port = raf.readInt();
					}

					port++;

					if (port > max) {
						port = min;
					}

					raf.setLength(0);
					raf.writeInt(port);

					lock.release();

					return port;
				}

			} catch (Throwable t) {

				t.printStackTrace();

				if (attempts < 5) {

					logger.warn("Unable to determine HTTP port for test, retrying in 1s");

					try { Thread.sleep(1000); } catch (Throwable ignore) { }

				} else {

					logger.warn("Unable to determine HTTP port for test, assigning random port.");

					// random number between (0 and 56000) plus 8877
					port = (int)Math.floor(Math.random() * 56000.0) + 8877;
				}
			}
		}

		return port;
	}
	*/
}
