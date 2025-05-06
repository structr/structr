package org.structr.test.helper;/*
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class ConcurrentPortNumberHelper {

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
		while (attempts++ < 3) {

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
}
