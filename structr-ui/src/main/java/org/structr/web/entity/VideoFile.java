/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import static org.structr.core.GraphObject.type;
import org.structr.core.GraphObjectMap;
import org.structr.core.JsonInput;
import org.structr.core.app.StructrApp;
import static org.structr.core.graph.NodeInterface.name;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.dynamic.File;
import org.structr.media.AVConv;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.web.common.FileHelper;
import static org.structr.web.entity.FileBase.relativeFilePath;
import static org.structr.web.entity.FileBase.size;
import org.structr.web.entity.relation.VideoFileHasConvertedVideoFile;
import org.structr.web.entity.relation.VideoFileHasPosterImage;

//~--- classes ----------------------------------------------------------------

/**
 * A video whose binary data will be stored on disk.
 *
 *
 *
 */
public class VideoFile extends File {

	// register this type as an overridden builtin type
	static {

		SchemaService.registerBuiltinTypeOverride("VideoFile", VideoFile.class.getName());
	}

	public static final Property<List<VideoFile>> convertedVideos = new EndNodes<>("convertedVideos", VideoFileHasConvertedVideoFile.class);
	public static final Property<VideoFile> originalVideo         = new StartNode<>("originalVideo", VideoFileHasConvertedVideoFile.class);
	public static final Property<Image> posterImage               = new EndNode<>("posterImage", VideoFileHasPosterImage.class);
	public static final Property<Boolean> isVideo                 = new BooleanProperty("isVideo").defaultValue(true).readOnly();
	public static final Property<String>  videoCodecName          = new StringProperty("videoCodecName").cmis();
	public static final Property<String>  videoCodec              = new StringProperty("videoCodec").cmis();
	public static final Property<String>  pixelFormat             = new StringProperty("pixelFormat").cmis();
	public static final Property<String>  audioCodecName          = new StringProperty("audioCodecName").cmis();
	public static final Property<String>  audioCodec              = new StringProperty("audioCodec").cmis();
	public static final Property<Integer> audioChannels           = new IntProperty("audioChannels").cmis();

	public static final Property<Double>  sampleRate              = new DoubleProperty("sampleRate").cmis().indexed();
	public static final Property<Double>  duration                = new DoubleProperty("duration").cmis().indexed();

	public static final Property<Integer> width                   = new IntProperty("width").cmis().indexed();
	public static final Property<Integer> height                  = new IntProperty("height").cmis().indexed();

	public static final org.structr.common.View uiView = new org.structr.common.View(VideoFile.class, PropertyView.Ui,
		type, name, contentType, size, relativeFilePath, owner, parent, path, isVideo, videoCodecName, videoCodec, pixelFormat,
		audioCodecName, audioCodec, audioChannels, sampleRate, duration, width, height, originalVideo, convertedVideos,
		posterImage
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(VideoFile.class, PropertyView.Public,
		type, name, owner, parent, path, isVideo, videoCodecName, videoCodec, pixelFormat,
		audioCodecName, audioCodec, audioChannels, sampleRate, duration, width, height,
		convertedVideos, posterImage
	);


	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		updateVideoInfo();
		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		updateVideoInfo();
		return super.onModification(securityContext, errorBuffer);
	}

	public String getDiskFilePath(final SecurityContext securityContext) {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final String path = getRelativeFilePath();

			tx.success();

			if (path != null) {
				return new java.io.File(FileHelper.getFilePath(path)).getAbsolutePath();
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}

	@Export
	public void convert(final String scriptName, final String newFileName) throws FrameworkException {
		AVConv.newInstance(securityContext, this, newFileName).doConversion(scriptName);
	}

	@Export
	public void grab(final String scriptName, final String imageName, final long timeIndex) throws FrameworkException {
		AVConv.newInstance(securityContext, this, imageName).grabFrame(scriptName, imageName, timeIndex);
	}

	@Export
	public RestMethodResult getMetadata() throws FrameworkException {

		final Map<String, String> metadata = AVConv.newInstance(securityContext, this).getMetadata();
		final RestMethodResult result      = new RestMethodResult(200);
		final GraphObjectMap map           = new GraphObjectMap();

		for (final Entry<String, String> entry : metadata.entrySet()) {
			map.setProperty(new StringProperty(entry.getKey()), entry.getValue());
		}

		result.addContent(map);

		return result;
	}

	@Export
	public void setMetadata(final String key, final String value) throws FrameworkException {
		AVConv.newInstance(securityContext, this).setMetadata(key, value);
	}

	@Export
	public void setMetadata(final JsonInput metadata) throws FrameworkException {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : metadata.entrySet()) {
			map.put(entry.getKey(), entry.getValue().toString());
		}

		AVConv.newInstance(securityContext, this).setMetadata(map);
	}

	@Export
	public void updateVideoInfo() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Map<String, Object> info = AVConv.newInstance(securityContext, this).getVideoInfo();
			if (info != null && info.containsKey("streams")) {

				final List<Map<String, Object>> streams = (List<Map<String, Object>>)info.get("streams");
				for (final Map<String, Object> stream : streams) {

					final String codecType = (String)stream.get("codec_type");
					if (codecType != null) {

						if ("video".equals(codecType)) {

							setIfNotNull(videoCodecName, stream.get("codec_long_name"));
							setIfNotNull(videoCodec,     stream.get("codec_name"));
							setIfNotNull(pixelFormat,    stream.get("pix_fmt"));
							setIfNotNull(width,          toInt(stream.get("width")));
							setIfNotNull(height,         toInt(stream.get("height")));
							setIfNotNull(duration,       toDouble(stream.get("duration")));


						} else if ("audio".equals(codecType)) {

							setIfNotNull(audioCodecName, stream.get("codec_long_name"));
							setIfNotNull(audioCodec,     stream.get("codec_name"));
							setIfNotNull(sampleRate,     toInt(stream.get("sampleRate")));
						}
					}
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	private void setIfNotNull(final Property key, final Object value) throws FrameworkException {

		if (value != null) {
			setProperty(key, value);
		}
	}

	private Integer toInt(final Object value) {

		if (value instanceof Number) {
			return ((Number)value).intValue();
		}

		if (value instanceof String) {

			try {
				return Integer.valueOf((String)value);

			} catch (Throwable t) {
				return null;
			}
		}

		return null;
	}

	private Double toDouble(final Object value) {

		if (value instanceof Number) {
			return ((Number)value).doubleValue();
		}

		if (value instanceof String) {

			try {
				return Double.valueOf((String)value);

			} catch (Throwable t) {
				return null;
			}
		}

		return null;
	}
}
