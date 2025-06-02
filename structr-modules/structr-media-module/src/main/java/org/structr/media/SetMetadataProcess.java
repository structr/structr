/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.util.AbstractProcess;
import org.structr.web.common.FileHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 *
 */

public class SetMetadataProcess extends AbstractProcess<Void> {

	private static final Logger logger = LoggerFactory.getLogger(SetMetadataProcess.class.getName());

	private final Map<String, String> metadata = new LinkedHashMap<>();
	private VideoFile inputVideo               = null;
	private String outputFileName              = null;
	private String fileExtension               = null;

	public SetMetadataProcess(final SecurityContext securityContext, final VideoFile inputVideo, final String key, final String value) {
		this(securityContext, inputVideo, toMap(key, value));
	}

	public SetMetadataProcess(final SecurityContext securityContext, final VideoFile inputVideo, final Map<String, String> values) {

		super(securityContext);

		// Todo: Fix for fs abstraction
		//this.outputFileName = inputVideo.getDiskFilePath(securityContext);
		this.inputVideo     = inputVideo;

		this.metadata.putAll(values);
	}

	@Override
	public void preprocess() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			// extract file extension
			fileExtension = StringUtils.substringAfterLast(inputVideo.getName(), ".");
			tx.success();

		} catch (FrameworkException fex) {}
	}

	@Override
	public StringBuilder getCommandLine() {

		if (metadata.isEmpty()) {
			return null;
		}

		final StringBuilder commandLine = new StringBuilder("avconv -y -i ");
		//final String diskFilePath       = inputVideo.getDiskFilePath(securityContext);

		// build command line from builder options
		//commandLine.append(diskFilePath);

		for (final Entry<String, String> meta : metadata.entrySet()) {

			try {
				commandLine.append(" -metadata ");
				commandLine.append(clean(meta.getKey()));
				commandLine.append("=\"");
				commandLine.append(escape(meta.getValue()));
				commandLine.append("\"");

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

		commandLine.append(" -codec copy ");
		//commandLine.append(diskFilePath);

		if (!fileExtension.isEmpty()) {
			commandLine.append(".");
			commandLine.append(fileExtension);
		}

		return commandLine;
	}

	@Override
	public Void processExited(int exitCode) {

		if (exitCode == 0) {

			try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

				// move converted file into place
				final java.io.File diskFile = new java.io.File(outputFileName + "." + fileExtension);
				final java.io.File dstFile  = new java.io.File(outputFileName);
				if (diskFile.exists()) {

					Files.move(diskFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					FileHelper.updateMetadata(inputVideo);
				}

				tx.success();

			} catch (FrameworkException | IOException fex) {
				logger.warn("", fex);
			}

		}

		return null;
	}

	protected boolean accept(final String key, final String value) {
		return key != null && !key.startsWith(";");
	}

	private String clean(final String input) {
		return input.replaceAll("[\\W]+", "");
	}

	private String escape(final String input) throws UnsupportedEncodingException {
		return URLEncoder.encode(input, "UTF-8");
	}

	private static final Map<String, String> toMap(final String key, final String value) {

		final Map<String, String> map = new LinkedHashMap<>();
		map.put(key, value);

		return map;
	}
}

