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
import org.structr.storage.providers.local.LocalFSHelper;
import org.structr.web.entity.Image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 *
 */

public class AVConv implements VideoHelper {

	private static final Logger logger = LoggerFactory.getLogger(AVConv.class.getName());

	private static final ExecutorService service = Executors.newCachedThreadPool();
	private SecurityContext securityContext      = null;
	private VideoFile inputVideo                 = null;
	private String outputFileName                = null;

	private AVConv(final SecurityContext securityContext, final VideoFile inputVideo, final String outputFileName) {
		this.securityContext = securityContext;
		this.inputVideo      = inputVideo;
		this.outputFileName  = outputFileName;
	}

	// ----- public static methods -----
	public static VideoHelper newInstance(final SecurityContext securityContext, final VideoFile inputVideo) {
		return newInstance(securityContext, inputVideo, null);
	}

	public static VideoHelper newInstance(final SecurityContext securityContext, final VideoFile inputVideo, final String outputFileName) {
		return new AVConv(securityContext, inputVideo, outputFileName);
	}

	@Override
	public Future<VideoFile> doConversion(final String scriptName) {
		return service.submit(new ConverterProcess(securityContext, inputVideo, outputFileName, scriptName));
	}

	@Override
	public Future<Image> grabFrame(final String scriptName, final String imageName, final long frameIndex) {
		return service.submit(new FrameGrabberProcess(securityContext, inputVideo, imageName, frameIndex, scriptName));
	}

	@Override
	public Map<String, String> getMetadata() {

		try {

			return service.submit(new GetMetadataProcess(securityContext, inputVideo)).get();

		} catch (InterruptedException | ExecutionException ex) {
			logger.warn("", ex);
		}

		return null;
	}

	@Override
	public void setMetadata(final String key, final String value) {

		try {

			service.submit(new SetMetadataProcess(securityContext, inputVideo, key, value)).get();

		} catch (InterruptedException | ExecutionException ex) {
			logger.warn("", ex);
		}
	}

	@Override
	public void setMetadata(final Map<String, String> metadata) {

		try {

			service.submit(new SetMetadataProcess(securityContext, inputVideo, metadata)).get();

		} catch (InterruptedException | ExecutionException ex) {
			logger.warn("", ex);
		}
	}

	@Override
	public Map<String, Object> getVideoInfo() {

		try {

			final String filePath = new LocalFSHelper(inputVideo.getStorageConfiguration()).getFileOnDisk(inputVideo).getAbsolutePath();

			return service.submit(new GetVideoInfoProcess(securityContext, filePath)).get();

		} catch (InterruptedException | ExecutionException ex) {
			logger.warn("", ex);
		}

		return null;
	}

	public static boolean isAVConvInstalled() {

		final String[] args = {"/bin/sh", "-c", "which ffmpeg"};

		try {

			Process proc = Runtime.getRuntime().exec(args);

			final String res = new BufferedReader(new InputStreamReader(proc.getInputStream())).readLine();

			return ((res != null) && !("".equals(res.trim())));

		} catch (IOException ex) {

			logger.warn("", ex);
			return false;
		}
	}
}
