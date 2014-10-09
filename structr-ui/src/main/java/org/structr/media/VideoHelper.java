package org.structr.media;

import java.util.Map;
import java.util.concurrent.Future;
import org.structr.web.entity.VideoFile;

/**
 *
 * @author Christian Morgner
 */
public interface VideoHelper {

	public VideoHelper scale(final VideoFormat format);
	public VideoHelper scale(final int width, final int height);
	public VideoHelper scale(final String customFormat);

	public Future<VideoFile> doConversion();

	public Map<String, String> getMetadata();
	public void setMetadata(final String key, final String value);
	public void setMetadata(final Map<String, String> metadata);

	public Map<String, Object> getVideoInfo();
}
