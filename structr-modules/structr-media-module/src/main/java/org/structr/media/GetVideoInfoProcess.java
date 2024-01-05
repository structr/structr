/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 *
 */

public class GetVideoInfoProcess extends AbstractProcess<Map<String, Object>> {

	private VideoFile videoFile = null;

	public GetVideoInfoProcess(final SecurityContext securityContext, final VideoFile videoFile) {

		super(securityContext);

		this.videoFile = videoFile;
	}

	@Override
	public void preprocess() {
	}

	@Override
	public StringBuilder getCommandLine() {

		StringBuilder commandLine = new StringBuilder("if [ -x \"$(which avprobe)\" ]; then avprobe -v verbose -show_format -show_streams -of json ");
		// ToDo: Fix for fs abstraction
		//commandLine.append(path);
		commandLine.append("; fi;");

		return commandLine;
	}

	@Override
	public Map<String, Object> processExited(int exitCode) {

		if (exitCode == 0) {

			return new GsonBuilder().create().fromJson(outputStream(), new TypeToken<LinkedHashMap<String, Object>>(){}.getType());
		}

		return null;
	}
}

