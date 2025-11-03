/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import jakarta.servlet.http.HttpServletResponse;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class UploadServletTest extends StructrUiTest {

	final String UPLOAD_SERVLET_PATH = "structr/upload";

	@Test
	public void testSuccessfulFileUpload() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String response = RestAssured
			.given()
				.header(X_USER_HEADER,       ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER,   ADMIN_PASSWORD)
				.multiPart("file",      "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(200)

			.when()
				.post(UPLOAD_SERVLET_PATH)

			.andReturn()
				.body()
				.asString();

		// find file
		try (final Tx tx = app.tx()) {

			final File file = app.nodeQuery(StructrTraits.FILE).getFirst().as(File.class);

			assertEquals("UUID returned from file upload does not match actual UUID", response,    file.getUuid());
			assertEquals("Name of uploaded file does not match actual name",          "test.txt",  file.getName());
			assertEquals("Location of uploaded file is not correct",                  Settings.DefaultUploadFolder.getValue() + "/test.txt", file.getFolderPath());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuccessfulFileUploadWithRedirectAndUUID() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String targetFolderPath = Settings.DefaultUploadFolder.getValue() + "/uploads";

		final String locationHeader = RestAssured
			.given()
				.header(X_USER_HEADER,       ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER,   ADMIN_PASSWORD)
				.multiPart("redirectOnSuccess",    "/nonexisting-url/")
				.multiPart("appendUuidOnRedirect", "true")
				.multiPart("uploadFolderPath",     targetFolderPath)
				.multiPart("file",                 "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(302)

			.when()
				.post(UPLOAD_SERVLET_PATH)

			.andReturn()
				.header("Location");

		// find file
		try (final Tx tx = app.tx()) {

			final File file               = app.nodeQuery(StructrTraits.FILE).getFirst().as(File.class);
			final Folder uploadFolder     = app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PATH_PROPERTY), targetFolderPath).getFirst().as(Folder.class);
			final String expectedLocation = "/nonexisting-url/" + file.getUuid();

			assertEquals("Location header of file upload response is not correct", expectedLocation,    locationHeader);
			assertEquals("Name of uploaded file does not match actual name",       "test.txt",          file.getName());
			assertEquals("Location of uploaded file is not correct",               targetFolderPath + "/test.txt", file.getFolderPath());
			assertNotNull("Requested upload folder was not created",               uploadFolder);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuccessfulFileUploadWithTypeChange() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createAdminUser();

			JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonType ext  = schema.addType("ExtendedFile");

			ext.addTrait(StructrTraits.FILE);

			StructrSchema.extendDatabaseSchema(app, schema);


			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.multiPart("type",    "ExtendedFile")
				.multiPart("file",    "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(200)

			.when()
				.post(UPLOAD_SERVLET_PATH);

		// find file
		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.nodeQuery("ExtendedFile").getFirst();

			assertEquals("Name of uploaded file does not match actual name", "test.txt",     file.getName());
			assertEquals("Type of uploaded file does not match actual type", "ExtendedFile", file.getType());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuccessfulFileUploadWithRedirect() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String targetFolderPath = Settings.DefaultUploadFolder.getValue() + "/uploads";

		final String locationHeader = RestAssured
			.given()
				.header(X_USER_HEADER,       ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER,   ADMIN_PASSWORD)
				.multiPart("redirectOnSuccess",    "/nonexisting-url/")
				.multiPart("uploadFolderPath",     targetFolderPath)
				.multiPart("file",                 "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(302)

			.when()
				.post(UPLOAD_SERVLET_PATH)

			.andReturn()
				.header("Location");

		// find file
		try (final Tx tx = app.tx()) {

			final File file               = app.nodeQuery(StructrTraits.FILE).getFirst().as(File.class);
			final String expectedLocation = "/nonexisting-url/";
			final Folder uploadFolder     = app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PATH_PROPERTY), targetFolderPath).getFirst().as(Folder.class);

			assertEquals("Location header of file upload response is not correct", expectedLocation,    locationHeader);
			assertEquals("Name of uploaded file does not match actual name",       "test.txt",          file.getName());
			assertEquals("Location of uploaded file is not correct",               targetFolderPath + "/test.txt", file.getFolderPath());
			assertNotNull("Requested upload folder was not created",               uploadFolder);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testErrorInFileUpload() {

		// setup
		try (final Tx tx = app.tx()) {

			createAdminUser();

			JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.getType(StructrTraits.FILE);

			type.addMethod("onCreate", "error('something', 'forbidden', 'nope')");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";
		RestAssured
			.given()
				.header(X_USER_HEADER,       ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER,   ADMIN_PASSWORD)
				.multiPart("file", "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(422)

			.when()
				.post(UPLOAD_SERVLET_PATH);
	}

	@Test
	public void testForbiddenProperties() {

		String folderId = null;

		// setup
		try (final Tx tx = app.tx()) {

			createAdminUser();

			folderId = app.create(StructrTraits.FOLDER, "upload").getUuid();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		// setting isTemplate is very forbidden
		RestAssured
			.given()
			.header(X_USER_HEADER,       ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER,   ADMIN_PASSWORD)
			.multiPart("file", "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")
			.multiPart("isTemplate", "true")

			.expect()
			.statusCode(422)

			.when()
			.post(UPLOAD_SERVLET_PATH);

		// setting the parent folder is allowed
		RestAssured
			.given()
			.header(X_USER_HEADER,       ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER,   ADMIN_PASSWORD)
			.multiPart("file", "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")
			.multiPart("parent", folderId)

			.expect()
			.statusCode(200)

			.when()
			.post(UPLOAD_SERVLET_PATH);

		// setting the parent folder via ID is allowed
		RestAssured
			.given()
			.header(X_USER_HEADER,       ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER,   ADMIN_PASSWORD)
			.multiPart("file", "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")
			.multiPart("parentId", folderId)

			.expect()
			.statusCode(200)

			.when()
			.post(UPLOAD_SERVLET_PATH);
	}

	@Test
	public void testDefaultUploadFolderSetting() {

		final Setting<String> defaultUploadFolderSetting = Settings.DefaultUploadFolder;

		defaultUploadFolderSetting.setValue("");
		assertEquals("Default upload folder setting should be the default value if set to ''. (Prevent uploading to root folder)", defaultUploadFolderSetting.getDefaultValue(), defaultUploadFolderSetting.getValue());
		defaultUploadFolderSetting.setValue("/");
		assertEquals("Default upload folder setting should be the default value if set to ''. (Prevent uploading to root folder)", defaultUploadFolderSetting.getDefaultValue(), defaultUploadFolderSetting.getValue());
		defaultUploadFolderSetting.setValue("/../");
		assertEquals("Default upload folder setting should be the default value if set to ''. (Prevent uploading to root folder)", defaultUploadFolderSetting.getDefaultValue(), defaultUploadFolderSetting.getValue());
		defaultUploadFolderSetting.setValue("/.././");
		assertEquals("Default upload folder setting should be the default value if set to ''. (Prevent uploading to root folder)", defaultUploadFolderSetting.getDefaultValue(), defaultUploadFolderSetting.getValue());
		defaultUploadFolderSetting.setValue("/../.././");
		assertEquals("Default upload folder setting should be the default value if set to ''. (Prevent uploading to root folder)", defaultUploadFolderSetting.getDefaultValue(), defaultUploadFolderSetting.getValue());

		defaultUploadFolderSetting.setValue("test");
		assertEquals("Default upload folder setting should convert to absolute path.", "/test", defaultUploadFolderSetting.getValue());
		defaultUploadFolderSetting.setValue("test/test2");
		assertEquals("Default upload folder setting should convert to absolute path.", "/test/test2", defaultUploadFolderSetting.getValue());

		defaultUploadFolderSetting.setValue("/test/.././");
		assertEquals("Default upload folder setting should clean relative parts.", "/test", defaultUploadFolderSetting.getValue());
		defaultUploadFolderSetting.setValue("/../test/.././");
		assertEquals("Default upload folder setting should clean relative parts.", "/test", defaultUploadFolderSetting.getValue());
		defaultUploadFolderSetting.setValue("/./test/.././");
		assertEquals("Default upload folder setting should clean relative parts.", "/test", defaultUploadFolderSetting.getValue());

	}

	@Test
	public void testUploadParameters() {

		// setup
		final String uploadUsername = "test_uploader";
		final String uploadPassword = "test";

		RestAssured.basePath = "/";

		Settings.DefaultUploadFolder.setValue("/our_custom_upload_folder");
		Settings.UploadAllowAnonymous.setValue(true);

		String folderId_not_under_default_upload_folder                                          = null;
		String folderId_not_under_default_upload_folder_visible_to_auth                          = null;
		String folderId_not_under_default_upload_folder_visible_to_auth_writable_for_upload_user = null;
		String folderId_underneath_default_upload_folder_not_visible_to_upload_user              = null;
		String folderId_underneath_default_upload_folder_but_visible_to_public                   = null;

		final byte[] defaultFileUploadContents = "This is a test!".getBytes(Charset.forName("utf-8"));

		try (final Tx tx = app.tx()) {

			// create non-admin user for upload tests
			final User uploadUser = app.create(StructrTraits.USER,
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), uploadUsername),
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), uploadPassword)
			).as(User.class);

			folderId_not_under_default_upload_folder = app.create(StructrTraits.FOLDER, "folder_not_in_default_upload_folder_1").getUuid();

			final Folder folder2 = app.create(StructrTraits.FOLDER, "folder_not_in_default_upload_folder_2").as(Folder.class);
			folder2.setVisibleToAuthenticatedUsers(true);
			folderId_not_under_default_upload_folder_visible_to_auth = folder2.getUuid();

			final Folder folder3 = app.create(StructrTraits.FOLDER, "folder_not_in_default_upload_folder_3").as(Folder.class);
			folder3.setVisibleToAuthenticatedUsers(true);
			folder3.as(AccessControllable.class).grant(Permission.write, uploadUser);
			folderId_not_under_default_upload_folder_visible_to_auth_writable_for_upload_user = folder3.getUuid();

			final NodeInterface folder4 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), Settings.DefaultUploadFolder.getValue() + "/folder_under_default_upload_folder");
			folderId_underneath_default_upload_folder_not_visible_to_upload_user = folder4.getUuid();

			final Folder folder5 = app.create(StructrTraits.FOLDER, "folder_not_in_default_upload_folder_but_visible_to_public").as(Folder.class);
			folder5.setVisibleToPublicUsers(true);
			folderId_underneath_default_upload_folder_but_visible_to_public = folder5.getUuid();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		// Tests for anonymous users
		{
			// anonymous users can not see the folder
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", NodeServiceCommand.getNextUuid())
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// anonymous users can not see the folder
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_not_under_default_upload_folder)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// anonymous users can not see the folder
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_not_under_default_upload_folder_visible_to_auth)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// anonymous users can not see the folder
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_not_under_default_upload_folder_visible_to_auth_writable_for_upload_user)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// anonymous users can not see the folder
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_underneath_default_upload_folder_not_visible_to_upload_user)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// anonymous users can see the folder, but are not allowed to write
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_underneath_default_upload_folder_but_visible_to_public)
				.expect().statusCode(HttpServletResponse.SC_FORBIDDEN)
				.when().post(UPLOAD_SERVLET_PATH);

			// anonymous users can upload in the default upload folder
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
				.expect().statusCode(HttpServletResponse.SC_OK)
				.when().post(UPLOAD_SERVLET_PATH);
		}

		// Tests for "parent"
		{
			// "parent" can not be found (because UUID does not exist)
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", NodeServiceCommand.getNextUuid())
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// Not Found "parent" (because user does not have read rights... can not see the folder)
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_not_under_default_upload_folder)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// "parent" can be found, but it is not underneath default upload folder and user has NO write rights
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_not_under_default_upload_folder_visible_to_auth)
				.expect().statusCode(HttpServletResponse.SC_FORBIDDEN)
				.when().post(UPLOAD_SERVLET_PATH);

			// "parent" can be found, is not underneath default upload folder, but user HAS write rights
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_not_under_default_upload_folder_visible_to_auth_writable_for_upload_user)
				.expect().statusCode(HttpServletResponse.SC_OK)
				.when().post(UPLOAD_SERVLET_PATH);

			// "parent" can not be found, IS underneath default upload folder, user has NO write rights
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parent", folderId_underneath_default_upload_folder_not_visible_to_upload_user)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);
		}

		// Tests for "parentId" (identical to "parent")
		{
			// "parent" can not be found (because UUID does not exist)
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parentId", NodeServiceCommand.getNextUuid())
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// Not Found "parent" (because user does not have read rights... can not see the folder)
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parentId", folderId_not_under_default_upload_folder)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);

			// "parent" can be found, but it is not underneath default upload folder and user has NO write rights
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parentId", folderId_not_under_default_upload_folder_visible_to_auth)
				.expect().statusCode(HttpServletResponse.SC_FORBIDDEN)
				.when().post(UPLOAD_SERVLET_PATH);

			// "parent" can be found, is not underneath default upload folder, but user HAS write rights
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parentId", folderId_not_under_default_upload_folder_visible_to_auth_writable_for_upload_user)
				.expect().statusCode(HttpServletResponse.SC_OK)
				.when().post(UPLOAD_SERVLET_PATH);

			// "parent" can be found, IS underneath default upload folder, user has NO write rights
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("parentId", folderId_underneath_default_upload_folder_not_visible_to_upload_user)
				.expect().statusCode(HttpServletResponse.SC_NOT_FOUND)
				.when().post(UPLOAD_SERVLET_PATH);
		}

		// Tests for "uploadFolderPath"
		{
			// 1. Folder path is underneath default upload folder
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("uploadFolderPath", Settings.DefaultUploadFolder.getValue() + "/this/is/a/lot/of/folders/")
				.expect().statusCode(HttpServletResponse.SC_OK)
				.when().post(UPLOAD_SERVLET_PATH);

			// 2. Folder path is NOT underneath default upload folder
			RestAssured
				.given()
					.header(X_USER_HEADER,       uploadUsername)
					.header(X_PASSWORD_HEADER,   uploadPassword)
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("uploadFolderPath", "/not/in/default/upload/folder/")
				.expect().statusCode(HttpServletResponse.SC_FORBIDDEN)
				.when().post(UPLOAD_SERVLET_PATH);

			// 3. Folder path is underneath default upload folder (but anonymous user is not allowed to use this feature)
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("uploadFolderPath", Settings.DefaultUploadFolder.getValue() + "/this/is/a/lot/of/folders/")
				.expect().statusCode(HttpServletResponse.SC_FORBIDDEN)
				.when().post(UPLOAD_SERVLET_PATH);

			// 4. Folder path is NOT underneath default upload folder (AND anonymous user is not allowed to use this feature)
			RestAssured
				.given()
					.multiPart("file", "test.txt", defaultFileUploadContents, "text/plain")
					.multiPart("uploadFolderPath", "/not/in/default/upload/folder/")
				.expect().statusCode(HttpServletResponse.SC_FORBIDDEN)
				.when().post(UPLOAD_SERVLET_PATH);
		}
	}
}
