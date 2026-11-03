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
package org.structr.test.media;

import org.structr.common.SecurityContext;
import org.structr.media.AVConv;
import org.structr.media.GetVideoInfoProcess;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/** ffprobe through the process base class: the complete JSON output arrives on every run. */
public class VideoInfoProcessTest {

	@Test
	public void testVideoInfoIsCompleteOnEveryRun() throws Exception {

		if (!AVConv.isAVConvInstalled()) {

			throw new SkipException("ffprobe is not installed");
		}

		final Path video = Files.createTempFile("structr-video-info", ".mp4");

		try (final InputStream in = VideoInfoProcessTest.class.getResourceAsStream("/test.mp4")) {

			Files.copy(in, video, StandardCopyOption.REPLACE_EXISTING);
		}

		try {

			// the output used to be read before the reader thread had taken it, which made the JSON empty or cut off
			for (int run = 0; run < 20; run++) {

				final Map<String, Object> info = new GetVideoInfoProcess(SecurityContext.getSuperUserInstance(), video.toString()).call();

				assertNotNull("run " + run, info);

				final Map<String, Object> stream = ((List<Map<String, Object>>) info.get("streams")).get(0);

				assertEquals("run " + run, "h264", stream.get("codec_name"));
				assertEquals("run " + run, 18.0, stream.get("width"));
				assertEquals("run " + run, 10.0, stream.get("height"));
			}

		} finally {

			Files.deleteIfExists(video);
		}
	}
}
