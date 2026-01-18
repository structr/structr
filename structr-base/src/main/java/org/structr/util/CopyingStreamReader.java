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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 *
 */
public class CopyingStreamReader extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(CopyingStreamReader.class.getName());

	private InputStream inputStream   = null;
	private OutputStream outputStream = null;
	private AtomicBoolean running     = null;

	public CopyingStreamReader(final InputStream is, final OutputStream out, final AtomicBoolean running) {

		super("StreamReader");

		this.inputStream  = is;
		this.outputStream = out;
		this.running      = running;

		this.setDaemon(true);
	}

	@Override
	public void run() {

		while (running.get()) {

			try {

				IOUtils.copy(inputStream, outputStream);

				Thread.sleep(10);

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}
	}
}
