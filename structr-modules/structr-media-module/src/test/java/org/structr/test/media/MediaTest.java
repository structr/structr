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

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.entity.Group;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.attribute.Name;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;
import org.structr.media.AVConv;
import org.structr.media.traits.definitions.VideoFileTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;

public class MediaTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(MediaTest.class.getName());

	@Test
	public void test01() {

		if (!AVConv.isAVConvInstalled()) {
			logger.info("Not performing test because `avconv` is not installed!");
			return;
		}

		if (System.getProperty("os.name").equals("Mac OS X")) {
			logger.info("Not performing test because `avconv` behaves differently on Mac!");
			return;
		}

		final String type = StructrTraits.VIDEO_FILE;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			// create AutoClosable input stream
			try (final InputStream is = MediaTest.class.getResourceAsStream("/test.mp4")) {
				FileHelper.createFile(securityContext, is, null, type, "test.mp4");
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// use RestAssured to check file
		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			.body("result[0].type",                  equalTo(StructrTraits.VIDEO_FILE))
			.body("result[0].name",                  equalTo("test.mp4"))
			.body("result[0].path",                  equalTo("/test.mp4"))
			.body("result[0].checksum",              equalTo(3346681520328299771L))
			.body("result[0].size",                  equalTo(91960))
			.body("result[0].duration",              equalTo(34.77f))
			.body("result[0].width",                 equalTo(18))
			.body("result[0].height",                equalTo(10))
			.body("result[0].pixelFormat",           equalTo("yuv420p"))
			.body("result[0].videoCodec",            equalTo("h264"))
			.body("result[0].videoCodecName",        equalTo("H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10"))
			.when()
			.get("/VideoFile");

	}

	@Test
	public void testVideoFilePosterImage() {

		String videoId = null, imageId = null;

		/**
		 * In this test, we create a video file with a poster image and a test user
		 * that is only allowed to see the video file, but not the image, and expect
		 * the image to be visible by propagated permissions via the POSTER_IMAGE
		 * relationship.
		 *
		 * Note: this does not currently work for visibilty flags or anonymous users!
		 */
		try (final Tx tx = app.tx()) {

			createAdminUser();

			final SchemaNode videoFile = app.create(StructrTraits.SCHEMA_NODE, new Name("VideoFile")).as(SchemaNode.class);
			final User tester = app.create(StructrTraits.USER, new Name("tester")).as(User.class);
			tester.setPassword("test");

			final Group group = app.create(StructrTraits.GROUP, new Name("group")).as(Group.class);
			group.addMember(securityContext, tester);

			// setup schema grant between group and VideoFile schema node
			final SchemaGrant schemaGrant = app.create(StructrTraits.SCHEMA_GRANT).as(SchemaGrant.class);
			schemaGrant.setProperty(schemaGrant.getTraits().key(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY), group);
			schemaGrant.setProperty(schemaGrant.getTraits().key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY), videoFile);
			schemaGrant.setProperty(schemaGrant.getTraits().key(SchemaGrantTraitDefinition.ALLOW_READ_PROPERTY), true);

			grant("VideoFile", UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET, false);
			grant("Image",     UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET, false);

			final NodeInterface video = app.create(StructrTraits.VIDEO_FILE, new Name("Testvideo"));
			final NodeInterface image = app.create(StructrTraits.IMAGE, new Name("Testimage"));

			videoId = video.getUuid();
			imageId = image.getUuid();

			video.setProperty(video.getTraits().key(VideoFileTraitDefinition.POSTER_IMAGE_PROPERTY), image);

			video.setVisibility(true, true);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// use RestAssured to check file
		RestAssured
			.given()
			.header(X_USER_HEADER, "tester")
			.header(X_PASSWORD_HEADER, "test")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.statusCode(200)
			.body("result[0].type",           equalTo(StructrTraits.VIDEO_FILE))
			.body("result[0].id",             equalTo(videoId))
			.body("result[0].posterImage.id", equalTo(imageId))
			.when()
			.get("/VideoFile");

		// use RestAssured to check file
		RestAssured
			.given()
			.header(X_USER_HEADER, "tester")
			.header(X_PASSWORD_HEADER, "test")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.statusCode(200)
			.body("result[0].type", equalTo(StructrTraits.IMAGE))
			.body("result[0].id",   equalTo(imageId))
			.when()
			.get("/Image");
	}
}
