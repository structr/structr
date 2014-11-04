package org.structr.media;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.structr.common.SecurityContext;
import org.structr.web.entity.VideoFile;

/**
 *
 * @author Christian Morgner
 */

public class GetMetadataProcess extends AbstractProcess<Map<String, String>> {

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
		commandLine.append(inputVideo.getDiskFilePath(securityContext));
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
				ioex.printStackTrace();
			}

			return map;
		}

		return null;
	}

	protected boolean accept(final String key, final String value) {
		return key != null && !key.startsWith(";");
	}
}

