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
package org.structr.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Writes the input of a process to its standard input and closes it, so a script that reads stdin gets its end.
 * In a thread of its own, because a process that writes much output before reading would otherwise block both sides.
 */
public class StdinWriter extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(StdinWriter.class.getName());

	private final InputStream input;
	private final OutputStream stdin;

	public StdinWriter(final InputStream input, final OutputStream stdin) {

		super("StdinWriter");

		this.input = input;
		this.stdin = stdin;

		this.setDaemon(true);
	}

	@Override
	public void run() {

		try (stdin) {

			if (input != null) {

				try (input) {

					IOUtils.copy(input, stdin);
				}
			}

		} catch (Throwable t) {

			// a script that does not read its input exits before taking it, the input is not an error then
			logger.debug("Input not fully taken by the process: {}", t.getMessage());
		}
	}
}
