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
package org.structr.media.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.InstanceMethod;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.media.AVConv;
import org.structr.media.VideoFile;
import org.structr.media.traits.wrappers.VideoFileTraitWrapper;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.operations.OnUploadCompletion;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A video whose binary data will be stored on disk.
 */
public class VideoFileTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CONVERTED_VIDEOS_PROPERTY = "convertedVideos";
	public static final String POSTER_IMAGE_PROPERTY     = "posterImage";
	public static final String ORIGINAL_VIDEO_PROPERTY   = "originalVideo";
	public static final String IS_VIDEO_PROPERTY         = "isVideo";
	public static final String VIDEO_CODEC_NAME_PROPERTY = "videoCodecName";
	public static final String VIDEO_CODEC_PROPERTY      = "videoCodec";
	public static final String PIXEL_FORMAT_PROPERTY     = "pixelFormat";
	public static final String AUDIO_CODEC_NAME_PROPERTY = "audioCodecName";
	public static final String AUDIO_CODEC_PROPERTY      = "audioCodec";
	public static final String AUDIO_CHANNELS_PROPERTY   = "audioChannels";
	public static final String SAMPLE_RATE_PROPERTY      = "sampleRate";
	public static final String DURATION_PROPERTY         = "duration";
	public static final String WIDTH_PROPERTY            = "width";
	public static final String HEIGHT_PROPERTY           = "height";

	public VideoFileTraitDefinition() {
		super(StructrTraits.VIDEO_FILE);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			OnUploadCompletion.class,
			new OnUploadCompletion() {

				@Override
				public void onUploadCompletion(final File file, final SecurityContext securityContext) {
					updateVideoInfo(file.as(VideoFile.class), securityContext);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			VideoFile.class, (traits, node) -> new VideoFileTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new InstanceMethod(StructrTraits.VIDEO_FILE, "updateVideoInfo") {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException {

					VideoFileTraitDefinition.updateVideoInfo(entity.as(VideoFile.class), securityContext);

					return null;
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> convertedVideosProperty = new EndNodes(CONVERTED_VIDEOS_PROPERTY, StructrTraits.VIDEO_FILE_HAS_CONVERTED_VIDEO_VIDEO_FILE);
		final Property<NodeInterface> posterImageProperty               = new EndNode(POSTER_IMAGE_PROPERTY, StructrTraits.VIDEO_FILE_HAS_POSTER_IMAGE_IMAGE);
		final Property<NodeInterface> originalVideoProperty             = new StartNode(ORIGINAL_VIDEO_PROPERTY, StructrTraits.VIDEO_FILE_HAS_CONVERTED_VIDEO_VIDEO_FILE);
		final PropertyKey<Boolean> isVideoProperty                      = new ConstantBooleanProperty(IS_VIDEO_PROPERTY, true);
		final PropertyKey<String> videoCodecNameProperty                = new StringProperty(VIDEO_CODEC_NAME_PROPERTY);
		final PropertyKey<String> videoCodecProperty                    = new StringProperty(VIDEO_CODEC_PROPERTY);
		final PropertyKey<String> pixelFormatProperty                   = new StringProperty(PIXEL_FORMAT_PROPERTY);
		final PropertyKey<String> audioCodecNameProperty                = new StringProperty(AUDIO_CODEC_NAME_PROPERTY);
		final PropertyKey<String> audioCodecProperty                    = new StringProperty(AUDIO_CODEC_PROPERTY);
		final PropertyKey<Integer> audioChannelsProperty                = new IntProperty(AUDIO_CHANNELS_PROPERTY);
		final PropertyKey<Double> sampleRateProperty                    = new DoubleProperty(SAMPLE_RATE_PROPERTY);
		final PropertyKey<Double> durationProperty                      = new DoubleProperty(DURATION_PROPERTY);
		final PropertyKey<Integer> widthProperty                        = new IntProperty(WIDTH_PROPERTY);
		final PropertyKey<Integer> heightProperty                       = new IntProperty(HEIGHT_PROPERTY);

		return newSet(
			convertedVideosProperty,
			posterImageProperty,
			originalVideoProperty,
			isVideoProperty,
			videoCodecNameProperty,
			videoCodecProperty,
			pixelFormatProperty,
			audioCodecNameProperty,
			audioCodecProperty,
			audioChannelsProperty,
			sampleRateProperty,
			durationProperty,
			widthProperty,
			heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					CONVERTED_VIDEOS_PROPERTY, POSTER_IMAGE_PROPERTY, IS_VIDEO_PROPERTY,
					VIDEO_CODEC_NAME_PROPERTY, VIDEO_CODEC_PROPERTY, PIXEL_FORMAT_PROPERTY, AUDIO_CODEC_NAME_PROPERTY, AUDIO_CODEC_PROPERTY,
					AUDIO_CHANNELS_PROPERTY, SAMPLE_RATE_PROPERTY, DURATION_PROPERTY, WIDTH_PROPERTY, HEIGHT_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					CONVERTED_VIDEOS_PROPERTY, POSTER_IMAGE_PROPERTY, IS_VIDEO_PROPERTY,
					VIDEO_CODEC_NAME_PROPERTY, VIDEO_CODEC_PROPERTY, PIXEL_FORMAT_PROPERTY, AUDIO_CODEC_NAME_PROPERTY, AUDIO_CODEC_PROPERTY,
					AUDIO_CHANNELS_PROPERTY, SAMPLE_RATE_PROPERTY, DURATION_PROPERTY, WIDTH_PROPERTY, HEIGHT_PROPERTY,
					ORIGINAL_VIDEO_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	public static void updateVideoInfo(final VideoFile thisVideo, final SecurityContext ctx) {

		try (final Tx tx = StructrApp.getInstance(ctx).tx()) {

			final Map<String, Object> info = AVConv.newInstance(ctx, thisVideo).getVideoInfo();
			if (info != null && info.containsKey("streams")) {

				final List<Map<String, Object>> streams = (List<Map<String, Object>>)info.get("streams");
				for (final Map<String, Object> stream : streams) {

					final String codecType = (String)stream.get("codec_type");
					if (codecType != null) {

						final Traits traits = Traits.of(StructrTraits.VIDEO_FILE);

						if ("video".equals(codecType)) {

							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(VIDEO_CODEC_NAME_PROPERTY), stream.get("codec_long_name"));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(VIDEO_CODEC_PROPERTY),      stream.get("codec_name"));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(PIXEL_FORMAT_PROPERTY),     stream.get("pix_fmt"));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(WIDTH_PROPERTY),            VideoFileTraitDefinition.toInt(stream.get("width")));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(HEIGHT_PROPERTY),           VideoFileTraitDefinition.toInt(stream.get("height")));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(DURATION_PROPERTY),         VideoFileTraitDefinition.toDouble(stream.get("duration")));

						} else if ("audio".equals(codecType)) {

							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(AUDIO_CODEC_NAME_PROPERTY), stream.get("codec_long_name"));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(AUDIO_CODEC_PROPERTY),      stream.get("codec_name"));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(AUDIO_CHANNELS_PROPERTY),   VideoFileTraitDefinition.toInt(stream.get("channels")));
							VideoFileTraitDefinition.setIfNotNull(thisVideo, traits.key(SAMPLE_RATE_PROPERTY),      VideoFileTraitDefinition.toInt(stream.get("sample_rate")));
						}
					}
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			final Logger logger = LoggerFactory.getLogger(VideoFile.class);
			logger.warn("", fex);
		}
	}

	static void setIfNotNull(final VideoFile thisVideo, final PropertyKey key, final Object value) throws FrameworkException {

		if (value != null) {
			thisVideo.setProperty(key, value);
		}
	}

	static Integer toInt(final Object value) {

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

	static Double toDouble(final Object value) {

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
