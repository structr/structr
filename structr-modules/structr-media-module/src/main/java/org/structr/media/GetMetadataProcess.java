/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.util.AbstractProcess;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 *
 *
 */

public class GetMetadataProcess extends AbstractProcess<Map<String, String>> {

	private static final Logger logger = LoggerFactory.getLogger(GetMetadataProcess.class.getName());

	private VideoFile inputVideo = null;

	public GetMetadataProcess(final SecurityContext securityContext, final VideoFile inputVideo) {

		super(securityContext);

		this.inputVideo = inputVideo;
	}

	@Override
	public void preprocess() {
	}

	@Override
	public StringBuilder getCommandLine() {

		StringBuilder commandLine = new StringBuilder("avconv -y -loglevel quiet -i ");

		// build command line from builder options
		// Todo: Fix for fs abstraction
		//commandLine.append(inputVideo.getDiskFilePath(securityContext));
		commandLine.append(" -f ffmetadata -");

		return commandLine;
	}

	@Override
	public Map<String, String> processExited(int exitCode) {

		if (exitCode == 0) {

			final Map<String, String> map = new LinkedHashMap<>();
			final Properties properties   = new Properties();

			try {
				properties.load(new StringReader(outputStream()));

				// convert entries to <String, String>
				for (final Entry<Object, Object> entry : properties.entrySet()) {

					final String key   = entry.getKey().toString();
					final String value = entry.getValue().toString();

					if (accept(key, value)) {
						map.put(key, value);
					}
				}

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}

			return map;
		}

		return null;
	}

	protected boolean accept(final String key, final String value) {
		return key != null && !key.startsWith(";");
	}
}

