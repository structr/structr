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
package org.structr.ftp;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.common.FtpTest;

/**
 * Tests for FTP service.
 * 
 *
 */
public class FtpAccessTest extends FtpTest {
	
	private static final Logger logger = Logger.getLogger(FtpAccessTest.class.getName());
	
	public void test01LoginFailed() {
		
		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {
			
			FTPClient ftp = new FTPClient();

			ftp.connect("127.0.0.1", ftpPort);
			logger.log(Level.INFO, "Reply from FTP server:", ftp.getReplyString());
			
			int reply = ftp.getReplyCode();
			assertTrue(FTPReply.isPositiveCompletion(reply));
			
			boolean loginSuccess = ftp.login("jt978hasdl", "lj3498ha");
			logger.log(Level.INFO, "Try to login as jt978hasdl/lj3498ha:", loginSuccess);
			assertFalse(loginSuccess);
			
			ftp.disconnect();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.SEVERE, "Error in FTP test", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
		
	}
	
	public void test02LoginSuccess() {
		
		FTPClient ftp = setupFTPClient("ftpuser1");
		disconnect(ftp);
		
	}
	
	public void test03UserAccessToDirectory() {
		
		FTPClient ftp1 = setupFTPClient("ftpuser1");
		
		final String name1 = "FTPdir1";
		FTPFile[] dirs = null;
		
		try (final Tx tx = app.tx()) {

			dirs = ftp1.listDirectories();
			
			assertNotNull(dirs);
			assertEquals(0, dirs.length);

			// Create folder by API methods
			createFTPDirectory(null, name1);
			
			tx.success();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.WARNING, "", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {
			
			dirs = ftp1.listDirectories();
			
			assertNotNull(dirs);
			assertEquals(1, dirs.length);
			assertEquals(name1, dirs[0].getName());

			tx.success();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.WARNING, "", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
		
		// Try to access the firectory as another user, result should be empty
		
		FTPClient ftp2 = setupFTPClient("ftpuser2");
		
		try (final Tx tx = app.tx()) {
			
			dirs = ftp2.listDirectories();
			
			assertNotNull(dirs);
			assertEquals(0, dirs.length);
			
			tx.success();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.WARNING, "", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

	}
	
}
