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
package org.structr.web.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.web.entity.Folder;

/**
 *
 *
 */
public class FolderTest extends StructrTest {
	
	public void testCreateFolder() {
		
		try (final Tx tx = app.tx()) {
			
			Folder test4 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/a");
			Folder test3 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/c/b/a");
			Folder test2 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/b/a");
			Folder test1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/b/c");
						
			tx.success();
			
		} catch (FrameworkException ex) {
			Logger.getLogger(FolderTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {
			
			Folder a = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a");
			assertNotNull(a);
			assertEquals(FileHelper.getFolderPath(a), "/a");
			
			Folder b = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b");
			assertNotNull(b);
			assertEquals(FileHelper.getFolderPath(b), "/a/b");
			
			Folder c = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b/c");
			assertNotNull(c);
			assertEquals(FileHelper.getFolderPath(c), "/a/b/c");
						
		} catch (FrameworkException ex) {
			Logger.getLogger(FolderTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		
	}

}
