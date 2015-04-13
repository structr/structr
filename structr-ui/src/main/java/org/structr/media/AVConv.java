/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.structr.common.SecurityContext;
import org.structr.web.entity.VideoFile;

/**
 *
 * @author Christian Morgner
 */

public class AVConv implements VideoHelper {

	private SecurityContext securityContext = null;
	private VideoFile inputVideo            = null;
	private String outputFileName           = null;
	private int exitCode                    = -1;
	private String outputSize               = null;

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

	// ----- methods from VideoConverter -----
	@Override
	public VideoHelper scale(final VideoFormat format) {

		this.outputSize = format.name();
		return this;
	}

	@Override
	public VideoHelper scale(final int width, final int height) {

		this.outputSize = width + ":" + height;
		return this;
	}

	@Override
	public VideoHelper scale(final String customFormat) {

		this.outputSize = customFormat;
		return this;
	}

	@Override
	public Future<VideoFile> doConversion() {

		final ExecutorService service  = Executors.newSingleThreadExecutor();
		final Future<VideoFile> future = service.submit(new ConverterProcess(securityContext, inputVideo, outputFileName, outputSize));
		service.shutdown();

		return future;
	}

	@Override
	public Map<String, String> getMetadata() {

		try {

			final ExecutorService service    = Executors.newSingleThreadExecutor();
			final Map<String, String> result = service.submit(new GetMetadataProcess(securityContext, inputVideo)).get();
			service.shutdown();

			return result;

		} catch (InterruptedException | ExecutionException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public void setMetadata(final String key, final String value) {

		try {

			final ExecutorService service = Executors.newSingleThreadExecutor();
			service.submit(new SetMetadataProcess(securityContext, inputVideo, key, value)).get();
			service.shutdown();

		} catch (InterruptedException | ExecutionException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void setMetadata(final Map<String, String> metadata) {

		try {

			final ExecutorService service = Executors.newSingleThreadExecutor();
			service.submit(new SetMetadataProcess(securityContext, inputVideo, metadata)).get();
			service.shutdown();

		} catch (InterruptedException | ExecutionException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Map<String, Object> getVideoInfo() {

		try {

			final ExecutorService service  = Executors.newSingleThreadExecutor();
			final Map<String, Object> info = service.submit(new GetVideoInfoProcess(securityContext, inputVideo)).get();
			service.shutdown();

			return info;

		} catch (InterruptedException | ExecutionException ex) {
			ex.printStackTrace();
		}

		return null;
	}
}
