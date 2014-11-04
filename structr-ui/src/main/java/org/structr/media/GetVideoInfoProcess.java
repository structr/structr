package org.structr.media;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.web.entity.VideoFile;

/**
 *
 * @author Christian Morgner
 */

public class GetVideoInfoProcess extends AbstractProcess<Map<String, Object>> {

	private VideoFile inputVideo = null;

	public GetVideoInfoProcess(final SecurityContext securityContext, final VideoFile inputVideo) {

		super(securityContext);

		this.inputVideo = inputVideo;
	}

	@Override
	public void preprocess() {
	}

	@Override
	public StringBuilder getCommandLine() {

		StringBuilder commandLine = new StringBuilder("avprobe -v verbose -show_format -show_streams -of json ");

		// build command line from builder options
		commandLine.append(inputVideo.getDiskFilePath(securityContext));

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

