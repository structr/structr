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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.JsonInput;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.web.entity.File;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A video whose binary data will be stored on disk.
 */
public interface VideoFile extends File {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("VideoFile");
		final JsonObjectType img  = schema.addType("Image");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/VideoFile"));
		type.setExtends(URI.create("#/definitions/File"));

		type.addBooleanProperty("isVideo", PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());

		type.addStringProperty("videoCodecName", PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("videoCodec",     PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("pixelFormat",    PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("audioCodecName", PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("audioCodec",     PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("audioChannels", PropertyView.Public, PropertyView.Ui);
		type.addNumberProperty("sampleRate",     PropertyView.Public, PropertyView.Ui);
		type.addNumberProperty("duration",       PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("width",         PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("height",        PropertyView.Public, PropertyView.Ui);

		type.overrideMethod("onCreation",      true,  "updateVideoInfo(arg0);");
		type.overrideMethod("onModification",  true,  "updateVideoInfo(arg0);");

		type.addMethod("updateVideoInfo")
			.addParameter("ctx", SecurityContext.class.getName())
			.setSource(VideoFile.class.getName() + ".updateVideoInfo(this, ctx);")
			.setDoExport(true);

		type.addMethod("convert")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("scriptName", String.class.getName())
			.addParameter("newFileName", String.class.getName())
			.setSource(AVConv.class.getName() + ".newInstance(ctx, this, newFileName).doConversion(scriptName);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("grab")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("scriptName", String.class.getName())
			.addParameter("imageFileName", String.class.getName())
			.addParameter("timeIndex", "long")
			.setSource(AVConv.class.getName() + ".newInstance(ctx, this, imageFileName).grabFrame(scriptName, imageFileName, timeIndex);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("getMetadata")
			.addParameter("ctx", SecurityContext.class.getName())
			.setReturnType(RestMethodResult.class.getName())
			.setSource("return " + VideoFile.class.getName() + ".getMetadata(this, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("setMetadata")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("key", String.class.getName())
			.addParameter("value", String.class.getName())
			.setSource(AVConv.class.getName() + ".newInstance(ctx, this).setMetadata(key, value);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("setMetadata")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("metadata", JsonInput.class.getName())
			.setSource(VideoFile.class.getName() + ".setMetadata(this, metadata, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.relate(type, "HAS_CONVERTED_VIDEO", Cardinality.OneToMany, "originalVideo",      "convertedVideos").setCascadingDelete(Cascade.sourceToTarget);
		type.relate(img,  "HAS_POSTER_IMAGE",    Cardinality.OneToOne,  "posterImageOfVideo", "posterImage").setCascadingDelete(Cascade.sourceToTarget);

		// view configuration
		type.addViewProperty(PropertyView.Public, "parent");
		type.addViewProperty(PropertyView.Public, "checksum");
		type.addViewProperty(PropertyView.Public, "convertedVideos");
		type.addViewProperty(PropertyView.Public, "posterImage");

		type.addViewProperty(PropertyView.Ui, "parent");
		type.addViewProperty(PropertyView.Ui, "parent");
		type.addViewProperty(PropertyView.Ui, "originalVideo");
		type.addViewProperty(PropertyView.Ui, "convertedVideos");
		type.addViewProperty(PropertyView.Ui, "posterImage");
	}}

	static RestMethodResult getMetadata(final VideoFile thisVideo, final SecurityContext ctx) throws FrameworkException {

		final SecurityContext securityContext = thisVideo.getSecurityContext();
		final Map<String, String> metadata    = AVConv.newInstance(securityContext, thisVideo).getMetadata();
		final RestMethodResult result         = new RestMethodResult(200);
		final GraphObjectMap map              = new GraphObjectMap();

		if (metadata != null) {

			for (final Entry<String, String> entry : metadata.entrySet()) {
				map.setProperty(new StringProperty(entry.getKey()), entry.getValue());
			}
		}

		result.addContent(map);

		return result;
	}

	static void setMetadata(final VideoFile thisVideo, final JsonInput metadata, final SecurityContext ctx) throws FrameworkException {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : metadata.entrySet()) {
			map.put(entry.getKey(), entry.getValue().toString());
		}

		AVConv.newInstance(ctx, thisVideo).setMetadata(map);
	}

	static void updateVideoInfo(final VideoFile thisVideo, final SecurityContext ctx) {

		try (final Tx tx = StructrApp.getInstance(ctx).tx()) {

			final Map<String, Object> info = AVConv.newInstance(ctx, thisVideo).getVideoInfo();
			if (info != null && info.containsKey("streams")) {

				final List<Map<String, Object>> streams = (List<Map<String, Object>>)info.get("streams");
				for (final Map<String, Object> stream : streams) {

					final String codecType = (String)stream.get("codec_type");
					if (codecType != null) {

						if ("video".equals(codecType)) {

							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "videoCodecName"), stream.get("codec_long_name"));
							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "videoCodec"),     stream.get("codec_name"));
							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "pixelFormat"),    stream.get("pix_fmt"));
							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "width"),          VideoFile.toInt(stream.get("width")));
							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "height"),         VideoFile.toInt(stream.get("height")));
							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "duration"),       VideoFile.toDouble(stream.get("duration")));

						} else if ("audio".equals(codecType)) {

							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "audioCodecName"), stream.get("codec_long_name"));
							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "audioCodec"),     stream.get("codec_name"));
							VideoFile.setIfNotNull(thisVideo, StructrApp.key(VideoFile.class, "sampleRate"),     VideoFile.toInt(stream.get("sampleRate")));
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
