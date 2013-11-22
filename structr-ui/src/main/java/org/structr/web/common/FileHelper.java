/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.common;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import org.apache.commons.io.FileUtils;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.Path;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.PropertyMap;
import org.structr.util.Base64;
import org.structr.web.entity.AbstractFile;
import static org.structr.web.servlet.HtmlServlet.searchNodesAsSuperuser;

//~--- classes ----------------------------------------------------------------

/**
 * File Utility class.
 *
 * @author Axel Morgner
 */
public class FileHelper {

	private static final String UNKNOWN_MIME_TYPE = "application/octet-stream";
	private static final Logger logger            = Logger.getLogger(FileHelper.class.getName());

	//~--- methods --------------------------------------------------------

	//~--- methods --------------------------------------------------------

	/**
	 * Transform an existing file into the target class.
	 *
	 * @param securityContext
	 * @param uuid
	 * @param fileType
	 * @return
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static org.structr.web.entity.File transformFile(final SecurityContext securityContext, final String uuid, final Class<? extends org.structr.web.entity.File> fileType) throws FrameworkException, IOException {
		
		AbstractFile existingFile = getFileByUuid(uuid);
		
		if (existingFile != null) {
			
			existingFile.unlockReadOnlyPropertiesOnce();
			existingFile.setProperty(AbstractNode.type, fileType == null ? org.structr.web.entity.File.class.getSimpleName() : fileType.getSimpleName());
			
			existingFile = getFileByUuid(uuid);
			
			return fileType != null ? fileType.cast(existingFile) : (org.structr.web.entity.File) existingFile;
		}
		
		return null;
	}
	
	/**
	 * Create a new image node from image data encoded in base64 format.
	 * 
	 * If the given string is an uuid of an existing file, transform it into the target class.
	 *
	 * @param securityContext
	 * @param rawData
	 * @param fileType defaults to File.class if null
	 * @return
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static org.structr.web.entity.File createFileBase64(final SecurityContext securityContext, final String rawData, final Class<? extends org.structr.web.entity.File> fileType) throws FrameworkException, IOException {

		Base64URIData uriData = new Base64URIData(rawData);

		return createFile(securityContext, uriData.getBinaryData(), uriData.getContentType(), fileType);

	}

	/**
	 * Create a new file node from the given file data
	 *
	 * @param securityContext
	 * @param fileData
	 * @param contentType
	 * @param fileType defaults to File.class if null
	 * @return
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static org.structr.web.entity.File createFile(final SecurityContext securityContext, final byte[] fileData, final String contentType, final Class<? extends org.structr.web.entity.File> fileType)
		throws FrameworkException, IOException {

		CreateNodeCommand<org.structr.web.entity.File> createNodeCommand = Services.command(securityContext, CreateNodeCommand.class);
		PropertyMap props                                                = new PropertyMap();
		
		props.put(AbstractNode.type, fileType == null ? org.structr.web.entity.File.class.getSimpleName() : fileType.getSimpleName());

		org.structr.web.entity.File newFile = createNodeCommand.execute(props);

		setFileData(newFile, fileData, contentType);
		
		return newFile;

	}

	/**
	 * Decodes base64-encoded raw data into binary data and writes it to
	 * the given file.
	 * 
	 * @param file
	 * @param rawData
	 * @throws FrameworkException
	 * @throws IOException 
	 */
	public static void decodeAndSetFileData(final org.structr.web.entity.File file, final String rawData) throws FrameworkException, IOException {

		Base64URIData uriData = new Base64URIData(rawData);
		setFileData(file, uriData.getBinaryData(), uriData.getContentType());

	}

	/**
	 * Write image data to the given file node and set checksum and size.
	 * 
	 * @param file
	 * @param fileData
	 * @param contentType
	 * @throws FrameworkException
	 * @throws IOException 
	 */
	public static void setFileData(final org.structr.web.entity.File file, final byte[] fileData, final String contentType)
		throws FrameworkException, IOException {

		FileHelper.writeToFile(file, fileData);
		file.setContentType(contentType);
		file.setChecksum(FileHelper.getChecksum(file));
		file.setSize(FileHelper.getSize(file));
		
	}

	//~--- get methods ----------------------------------------------------

	public static String getBase64String(final org.structr.web.entity.File file) {
		
		try {

			return Base64.encodeToString(IOUtils.toByteArray(file.getInputStream()), false);

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not get base64 string from file ", ex);
		}

		return null;
	}
	
	//~--- inner classes --------------------------------------------------

	public static class Base64URIData {

		private String contentType;
		private String data;

		//~--- constructors -------------------------------------------

		public Base64URIData(final String rawData) {

			String[] parts = StringUtils.split(rawData, ",");

			data        = parts[1];
			contentType = StringUtils.substringBetween(parts[0], "data:", ";base64");

		}

		//~--- get methods --------------------------------------------

		public String getContentType() {

			return contentType;

		}

		public String getData() {

			return data;

		}

		public byte[] getBinaryData() {

			return Base64.decode(data);

		}

	}


	/**
	 * Write binary data to a file and reference the file on disk at the given file node
	 *
	 * @param fileNode
	 * @param data
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void writeToFile(final org.structr.web.entity.File fileNode, final byte[] data) throws FrameworkException, IOException {

		String uuid = fileNode.getProperty(AbstractNode.uuid);
		if (uuid == null) {

			final String newUuid = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
			final App app        = StructrApp.getInstance(fileNode.getSecurityContext());
			uuid                 = newUuid;

			try {

				app.beginTx();
				fileNode.unlockReadOnlyPropertiesOnce();
				fileNode.setProperty(AbstractNode.uuid, newUuid);
				app.commitTx();
				
			} finally {
				
				app.finishTx();
			}

		}

		fileNode.setRelativeFilePath(org.structr.web.entity.File.getDirectoryPath(uuid) + "/" + uuid);

		java.io.File fileOnDisk = new java.io.File(Services.getFilesPath() + "/" + fileNode.getRelativeFilePath());

		fileOnDisk.getParentFile().mkdirs();
		FileUtils.writeByteArrayToFile(fileOnDisk, data);

	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Return mime type of given file
	 *
	 * @param file
	 * @return
	 */
	public static String getContentMimeType(final File file) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(file, false, true);

			return match.getMimeType();

		} catch (Exception e) {

			logger.log(Level.WARNING, "Could not determine content type");

		}

		return UNKNOWN_MIME_TYPE;

	}

	/**
	 * Return mime type of given byte array.
	 *
	 * Use on streams.
	 *
	 * @param bytes
	 * @return
	 */
	public static String getContentMimeType(final byte[] bytes) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(bytes, true);

			return match.getMimeType();

		} catch (Exception e) {

			logger.log(Level.SEVERE, null, e);

		}

		return UNKNOWN_MIME_TYPE;

	}

	/**
	 * Return mime type of given file
	 *
	 * @param file
	 * @return
	 */
	public static String[] getContentMimeTypeAndExtension(final File file) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(file, false, true);

			return new String[] { match.getMimeType(), match.getExtension() };

		} catch (Exception e) {

			logger.log(Level.SEVERE, null, e);

		}

		return new String[] { UNKNOWN_MIME_TYPE, ".bin" };

	}

	/**
	 * Return mime type of given byte array.
	 *
	 * Use on streams.
	 *
	 * @param bytes
	 * @return
	 */
	public static String[] getContentMimeTypeAndExtension(final byte[] bytes) {

		MagicMatch match;

		try {

			match = Magic.getMagicMatch(bytes, true);

			return new String[] { match.getMimeType(), match.getExtension() };

		} catch (Exception e) {

			logger.log(Level.SEVERE, null, e);

		}

		return new String[] { UNKNOWN_MIME_TYPE, ".bin" };

	}

	/**
	 * Calculate CRC32 checksum of given file
	 * 
	 * @param file
	 * @return 
	 */
	public static Long getChecksum(final org.structr.web.entity.File file) {

		String relativeFilePath = file.getRelativeFilePath();

		if (relativeFilePath != null) {

			String filePath         = Services.getFilePath(Path.Files, relativeFilePath);

			try {
			
				java.io.File fileOnDisk = new java.io.File(filePath);
				Long checksum = FileUtils.checksumCRC32(fileOnDisk);

				logger.log(Level.FINE, "Checksum of file {0} ({1}): {2}", new Object[] { file.getUuid(), filePath, checksum });

				return checksum;

			} catch (Exception ex) {

				logger.log(Level.WARNING, "Could not calculate checksum of file {0}", filePath);

			}

		}
		
		return null;
		
	}
	
	/**
	 * Return size of file on disk, or -1 if not possible
	 * 
	 * @param file
	 * @return 
	 */
	public static long getSize(final org.structr.web.entity.File file) {
		
		String path = file.getRelativeFilePath();

		if (path != null) {

			String filePath         = Services.getFilePath(Path.Files, path);

			try {

				java.io.File fileOnDisk = new java.io.File(filePath);
				long fileSize           = fileOnDisk.length();

				logger.log(Level.FINE, "File size of node {0} ({1}): {2}", new Object[] { file.getUuid(), filePath, fileSize });

				return fileSize;
				
			} catch (Exception ex) {

				logger.log(Level.WARNING, "Could not calculate file size{0}", filePath);

			}


		}

		return -1;
		
	}

	/**
	 * Find a file by its absolute ancestor path.
	 * 
	 * File may not be hidden or deleted.
	 * @param absolutePath
	 * @return 
	 */
	public static AbstractFile getFileByAbsolutePath(final String absolutePath) {
		
		String[] parts = PathHelper.getParts(absolutePath);
		
		if (parts == null || parts.length == 0) return null;
		
		// Find root folder
		if (parts[0].length() == 0) return null;

		AbstractFile currentFile = getFileByName(parts[0]); 
		if (currentFile == null) return null;
		
		for (int i=1; i<parts.length; i++) {

			List<AbstractFile> children = currentFile.getProperty(AbstractFile.children);
			
			currentFile = null;
			
			for (AbstractFile child : children) {
				
				if (child.getProperty(AbstractFile.name).equals(parts[i])) {
					
					// Child with matching name found
					currentFile = child;
					break;
				}
				
			}
			
			if (currentFile == null) return null;
			
		}
		
		return currentFile;
		
	}
	
	public static AbstractFile getFileByUuid(final String uuid) {

		logger.log(Level.FINE, "Search for file with uuid: {0}", uuid);

		List<SearchAttribute> searchAttrs = new LinkedList<>();

		searchAttrs.add(Search.andExactUuid(uuid));
		searchAttrs.add(Search.orExactTypeAndSubtypes(org.structr.web.entity.AbstractFile.class));

		
		try {
			Result<AbstractFile> results = searchNodesAsSuperuser.execute(false, false, searchAttrs);
			logger.log(Level.FINE, "{0} files found", results.size());
			if (results.isEmpty()) return null;
		
			return (AbstractFile) results.get(0);
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return null;
	}
		
	public static AbstractFile getFileByName(final String name) {

		logger.log(Level.FINE, "Search for file with name: {0}", name);

		List<SearchAttribute> searchAttrs = new LinkedList<>();

		searchAttrs.add(Search.andExactName(name));
		searchAttrs.add(Search.orExactTypeAndSubtypes(org.structr.web.entity.AbstractFile.class));

		
		try {
			Result<AbstractFile> results = searchNodesAsSuperuser.execute(false, false, searchAttrs);
			logger.log(Level.FINE, "{0} files found", results.size());
			if (results.isEmpty()) return null;
		
			return (AbstractFile) results.get(0);
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return null;
	}
	
	/**
	 * Return the virtual folder path of any {@link org.structr.web.entity.File} or {@link org.structr.web.entity.Folder}
	 * @param file
	 * @return 
	 */
	public static String getFolderPath(final AbstractFile file) {
		
		LinkedTreeNode parentFolder = file.getProperty(AbstractFile.parent);
		
		String folderPath = file.getProperty(AbstractFile.name);
		
		if (folderPath == null) folderPath = file.getProperty(AbstractNode.uuid);
		
		while (parentFolder != null) {
			folderPath = parentFolder.getName().concat("/").concat(folderPath);
			parentFolder = parentFolder.getProperty(AbstractFile.parent);
		}

		return "/".concat(folderPath);
	}
	
}
