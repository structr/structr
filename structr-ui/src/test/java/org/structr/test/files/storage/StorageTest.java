/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.test.files.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.storage.StorageProviderFactory;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.function.GetContentFunction;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import static org.testng.AssertJUnit.*;


public class StorageTest extends StructrUiTest {
	private static final Logger logger = LoggerFactory.getLogger(StorageTest.class);

	@Test
	public void testStorageBinaryMigrationForFolders() {

		try (Tx tx = app.tx()){

			PropertyMap folderProps = new PropertyMap();
			folderProps.put(StructrApp.key(Folder.class, "name"), "local");
			folderProps.put(StructrApp.key(Folder.class, "storageProvider"), "local");
			Folder folder = app.create(Folder.class, folderProps);

			PropertyMap folderProps2 = new PropertyMap();
			folderProps2.put(StructrApp.key(Folder.class, "name"), "memory");
			folderProps2.put(StructrApp.key(Folder.class, "storageProvider"), "memory");
			Folder folder2 = app.create(Folder.class, folderProps2);

			PropertyMap fileProps = new PropertyMap();
			fileProps.put(StructrApp.key(File.class, "name"), "testFile.txt");
			fileProps.put(StructrApp.key(File.class, "parent"), folder);
			File file = app.create(File.class, fileProps);

			final String payload = "test payload written to this file";

			OutputStream os = file.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			// Check if data was successfully written
			InputStream is = file.getInputStream();
			String result = new Scanner(is).useDelimiter("\\A").next();
			assertEquals(payload, result);

			// Move from in local to memory storage provider folder
			file.setProperty(StructrApp.key(File.class, "parent"), folder2);

			// Check if data is still equal after migration
			is = file.getInputStream();
			result = new Scanner(is).useDelimiter("\\A").next();
			assertEquals(payload, result);

			tx.success();
		} catch (FrameworkException | IOException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

	@Test
	public void testStorageBinaryMigrationForFile() {

		try (Tx tx = app.tx()){

			final PropertyKey<String> storageProviderKey = StructrApp.key(File.class, "storageProvider");

			PropertyMap fileProps = new PropertyMap();
			fileProps.put(StructrApp.key(File.class, "name"), "testFile.txt");
			File file = app.create(File.class, fileProps);

			final String payload = "test payload written to this file";

			OutputStream os = file.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			// Check if data was successfully written
			InputStream is = file.getInputStream();
			String result = new Scanner(is).useDelimiter("\\A").next();
			assertEquals(payload, result);

			// Move from default (local) to local storage provider
			file.setProperty(storageProviderKey, "local");
			var localStorageProvider = StorageProviderFactory.getStorageProvider(file);

			// Check if data is still equal after migration
			is = file.getInputStream();
			result = new Scanner(is).useDelimiter("\\A").next();
			assertEquals(payload, result);

			// Move from local to memory storage provider
			file.setProperty(storageProviderKey, "memory");
			var memoryStorageProvider = StorageProviderFactory.getStorageProvider(file);

			assert(!localStorageProvider.equals(memoryStorageProvider));
			assertEquals(localStorageProvider.getConfig().Name(), "local");
			assertEquals(memoryStorageProvider.getConfig().Name(), "memory");

			// Check if data is still equal after migration
			is = file.getInputStream();
			result = new Scanner(is).useDelimiter("\\A").next();
			assertEquals(payload, result);

			// Move from memory to default storage provider
			file.setProperty(storageProviderKey, null);

			// Check if data is still equal after migration
			is = file.getInputStream();
			result = new Scanner(is).useDelimiter("\\A").next();
			assertEquals(payload, result);

			tx.success();
		} catch (FrameworkException | IOException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

}
