/*
 *  Copyright (C) 2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.files.ftp;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.FtpTest;

/**
 * Tests for FTP files.
 * 
 * @author Axel Morgner
 */
public class FtpFilesTest extends FtpTest {

	private static final Logger logger = Logger.getLogger(FtpFilesTest.class.getName());
	
	public void test01ListFiles() {
		
		FTPClient ftp = setupFTPClient();
		try {
			
			FTPFile[] files = ftp.listFiles();
			
			assertNotNull(files);
			assertEquals(0, files.length);
			
			String name1 = "file1";
			
			// Create files by API methods
			createFTPFile(null, name1);
			String[] fileNames = ftp.listNames();
			
			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name1, fileNames[0]);

			String name2 = "file2";
			
			// Create second file in /
			createFTPFile(null, name2);
			fileNames = ftp.listNames();
			
			assertNotNull(fileNames);
			assertEquals(2, fileNames.length);
			assertEquals(name1, fileNames[0]);
			assertEquals(name2, fileNames[1]);
			
			ftp.disconnect();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.SEVERE, "Error while listing FTP files", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}
	
	public void test02RenameFile() {
		
		FTPClient ftp = setupFTPClient();
		try {
			
			FTPFile[] files = ftp.listFiles();
			
			assertNotNull(files);
			assertEquals(0, files.length);
			
			String name1 = "file1";
			
			// Create files by API methods
			createFTPFile(null, name1);
			
			String name2 = "file2";
			
			ftp.rename(name1, name2);
			
			String[] fileNames = ftp.listNames();
			
			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name2, fileNames[0]);

			ftp.disconnect();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.SEVERE, "Error while renaming FTP file", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	public void test03MoveFile() {
		
		FTPClient ftp = setupFTPClient();
		try {
			
			FTPFile[] files = ftp.listFiles();
			
			assertNotNull(files);
			assertEquals(0, files.length);
			
			String name1 = "file1";
			
			// Create files by API methods
			createFTPFile(null, name1);

			String name2 = "dir1";
			
			// Create folder in /
			createFTPDirectory(null, name2);
			
			 // Move file to dir
			ftp.rename("/" + name1, "/" + name2 + "/" + name1);
			
			ftp.changeWorkingDirectory("/" + name2);
			
			String[] fileNames = ftp.listNames();
			
			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name1, fileNames[0]);
			
			ftp.disconnect();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.SEVERE, "Error while moving FTP file", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	public void test04MoveFileToRoot() {
		
		FTPClient ftp = setupFTPClient();
		try {
			
			FTPFile[] files = ftp.listFiles();
			
			assertNotNull(files);
			assertEquals(0, files.length);
			
			String name1 = "file1";
			
			// Create files by API methods
			createFTPFile(null, name1);

			String name2 = "dir1";
			
			// Create folder in /
			createFTPDirectory(null, name2);
			
			 // Move file to dir
			ftp.rename("/" + name1, "/" + name2 + "/" + name1);
			
			ftp.changeWorkingDirectory("/" + name2);
			
			String[] fileNames = ftp.listNames();
			
			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name1, fileNames[0]);
			
			// Move file back to /
			ftp.rename("/" + name2 + "/" + name1, "/" + name1);
			
			ftp.changeWorkingDirectory("/");
			
			fileNames = ftp.listNames();
			
			assertNotNull(fileNames);
			assertEquals(2, fileNames.length);
			assertEquals(name2, fileNames[0]);
			assertEquals(name1, fileNames[1]);
			
			
			ftp.disconnect();
			
		} catch (IOException | FrameworkException ex) {
			logger.log(Level.SEVERE, "Error while moving FTP file", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

}
