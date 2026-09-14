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

import java.util.LinkedList;
import java.util.List;
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

	/**
	 * Ticket 1587: see ConverterProcess.getCommandArguments(). The command is an argument list so that
	 * the input file path - commented out below pending the filesystem abstraction, and carrying the
	 * file name in a mounted folder - cannot be read as shell syntax when it is switched back on.
	 */
	@Override
	public List<String> getCommandArguments() {

		final List<String> arguments = new LinkedList<>();

		arguments.addAll(List.of("avconv", "-y", "-loglevel", "quiet", "-i"));
		// Todo: Fix for fs abstraction
		//arguments.add(inputVideo.getDiskFilePath(securityContext));
		arguments.addAll(List.of("-f", "ffmetadata", "-"));

		return arguments;
	}

	@Override
	public StringBuilder getCommandLine() {

		return null;
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

