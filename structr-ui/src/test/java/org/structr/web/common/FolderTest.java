/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
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
 * @author Axel Morgner
 */
public class FolderTest extends StructrTest {
	
	public void testCreateFolder() {
		
		try (final Tx tx = app.tx()) {
			
			Folder test = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/b/c");
						
			tx.success();
			
		} catch (FrameworkException ex) {
			Logger.getLogger(FolderTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {
			
			Folder a = (Folder) FileHelper.getFileByAbsolutePath("/a");
			assertNotNull(a);
			assertNull(a.getProperty(Folder.parent));
			
			Folder b = (Folder) FileHelper.getFileByAbsolutePath("/a/b");
			assertNotNull(b);
			assertEquals(a, b.getProperty(Folder.parent));
			
			Folder c = (Folder) FileHelper.getFileByAbsolutePath("/a/b/c");
			assertNotNull(c);
			assertEquals(b, c.getProperty(Folder.parent));
						
		} catch (FrameworkException ex) {
			Logger.getLogger(FolderTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		
	}

}
