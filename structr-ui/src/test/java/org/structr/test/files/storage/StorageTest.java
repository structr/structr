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
package org.structr.test.files.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.storage.providers.memory.InMemoryStorageProvider;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.StorageConfiguration;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Scanner;

import static org.testng.AssertJUnit.*;


public class StorageTest extends StructrUiTest {
	private static final Logger logger = LoggerFactory.getLogger(StorageTest.class);

	@Test
	public void testStorageBinaryMigrationForFolders() {

		try (Tx tx = app.tx()){

			final StorageConfiguration local  = StorageProviderFactory.createConfig("local",  LocalFSStorageProvider.class, Map.of());
			final StorageConfiguration memory = StorageProviderFactory.createConfig("memory", InMemoryStorageProvider.class, Map.of());

			PropertyMap folderProps = new PropertyMap();
			folderProps.put(StructrApp.key(Folder.class, "name"), "local");
			folderProps.put(StructrApp.key(Folder.class, "storageConfiguration"), local);
			Folder folder = app.create(Folder.class, folderProps);

			PropertyMap folderProps2 = new PropertyMap();
			folderProps2.put(StructrApp.key(Folder.class, "name"), "memory");
			folderProps2.put(StructrApp.key(Folder.class, "storageConfiguration"), memory);
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

		try (Tx tx = app.tx()) {

			final StorageConfiguration local  = StorageProviderFactory.createConfig("local",  LocalFSStorageProvider.class, null);
			final StorageConfiguration memory = StorageProviderFactory.createConfig("memory", InMemoryStorageProvider.class, null);

			final PropertyKey<StorageConfiguration> storageConfigurationKey = StructrApp.key(File.class, "storageConfiguration");

			PropertyMap fileProps = new PropertyMap();
			fileProps.put(StructrApp.key(File.class, "name"), "testFile.txt");
			File file = app.create(File.class, fileProps);

			final String payload = "test payload written to this file";

			OutputStream os = file.getOutputStream();
			os.write(payload.getBytes());
			os.flush();
			os.close();

			// Check if data was successfully written
			InputStream is = file.getInputStream();
			String result = new Scanner(is).useDelimiter("\\A").next();
			is.close();
			assertEquals(payload, result);

			// Move from default (local) to local storage provider
			file.setProperty(storageConfigurationKey, local);
			var localStorageProvider = StorageProviderFactory.getStorageProvider(file);

			// Check if data is still equal after migration
			is = file.getInputStream();
			result = new Scanner(is).useDelimiter("\\A").next();
			is.close();
			assertEquals(payload, result);

			// Move from local to memory storage provider
			file.setProperty(storageConfigurationKey, memory);
			var memoryStorageProvider = StorageProviderFactory.getStorageProvider(file);

			assertFalse(localStorageProvider.equals(memoryStorageProvider));
			assertEquals(localStorageProvider.getProviderName(), "local");
			assertEquals(memoryStorageProvider.getProviderName(), "memory");

			// Check if data is still equal after migration
			is = file.getInputStream();
			result = new Scanner(is).useDelimiter("\\A").next();
			is.close();
			assertEquals(payload, result);

			// Move from memory to default storage provider
			file.setProperty(storageConfigurationKey, null);
			assertEquals("default", StorageProviderFactory.getStorageProvider(file).getProviderName());
			assertEquals(LocalFSStorageProvider.class, StorageProviderFactory.getStorageProvider(file).getClass());

			// Check if data is still equal after migration
			is = file.getInputStream();
			result = new Scanner(is).useDelimiter("\\A").next();
			is.close();
			assertEquals(payload, result);

			tx.success();
		} catch (FrameworkException | IOException ex) {
			fail("Unexpected exception: " + ex);
		}
	}

}
