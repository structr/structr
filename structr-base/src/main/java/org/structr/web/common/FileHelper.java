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
package org.structr.web.common;

import net.openhft.hashing.Access;
import net.openhft.hashing.LongHashFunction;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.PathHelper;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.util.Base64;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * File utility class.
 */
public class FileHelper {

	private static final String UNKNOWN_MIME_TYPE         = "application/octet-stream";
	private static final Logger logger                    = LoggerFactory.getLogger(FileHelper.class.getName());
	private static final MimetypesFileTypeMap mimeTypeMap = new MimetypesFileTypeMap(FileHelper.class.getResourceAsStream("/mime.types"));

	/**
	 * Transform an existing file into the target class.
	 *
	 * @param securityContext
	 * @param uuid
	 * @param fileType
	 * @return transformed file
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static NodeInterface transformFile(final SecurityContext securityContext, final String uuid, final String fileType) throws FrameworkException {

		NodeInterface existingFile = getFileByUuid(securityContext, uuid);

		if (existingFile != null) {

			existingFile.unlockSystemPropertiesOnce();
			existingFile.setProperties(securityContext, new PropertyMap(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), fileType == null ? StructrTraits.FILE : fileType));

			return getFileByUuid(securityContext, uuid);
		}

		return null;
	}

	/**
	 * Create a new file node from file data encoded in base64 format.
	 *
	 * If the given string is an uuid of an existing file, transform it into
	 * the target class.
	 *
	 * @param securityContext
	 * @param rawData
	 * @param type defaults to File.class if null
	 * @return file
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static NodeInterface createFileBase64(final SecurityContext securityContext, final String rawData, final String type) throws FrameworkException, IOException {

		final Base64URIData uriData = new Base64URIData(rawData);

		return createFile(securityContext, uriData.getBinaryData(), uriData.getContentType(), type);

	}

	/**
	 * Create a new file node from the given input stream
	 *
	 * @param securityContext
	 * @param fileStream
	 * @param contentType
	 * @param fileType defaults to File.class if null
	 * @param name
	 * @return file
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static NodeInterface createFile(final SecurityContext securityContext, final InputStream fileStream, final String contentType, final String fileType, final String name) throws FrameworkException, IOException {

		return createFile(securityContext, fileStream, contentType, fileType, name, null);

	}

	/**
	 * Create a new file node from a given file on disk by moving existing file to referenced file location
	 *
	 * @param securityContext
	 * @param existingFileOnDisk
	 * @param contentType
	 * @param name
	 * @return
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static File createFile(final SecurityContext securityContext, final java.io.File existingFileOnDisk, final String contentType, final String name) throws FrameworkException, IOException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.FILE);

		props.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),        name);
		props.put(traits.key(FileTraitDefinition.CONTENT_TYPE_PROPERTY), contentType);

		final File newFile = StructrApp.getInstance(securityContext).create(StructrTraits.FILE, props).as(File.class);

		try (FileInputStream fis = new FileInputStream(existingFileOnDisk); OutputStream os = StorageProviderFactory.getStorageProvider(newFile).getOutputStream()) {

			IOUtils.copy(fis, os);
		}

		return newFile;
	}

	/**
	 * Create a new file node from the given input stream and sets the parentFolder
	 *
	 * @param securityContext
	 * @param fileStream
	 * @param contentType
	 * @param fileType defaults to File.class if null
	 * @param name
	 * @param parentFolder
	 * @return file
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static NodeInterface createFile(final SecurityContext securityContext, final InputStream fileStream, final String contentType, final String fileType, final String name, final Folder parentFolder) throws FrameworkException, IOException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.FILE);

		props.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
		props.put(traits.key(FileTraitDefinition.CONTENT_TYPE_PROPERTY), contentType);

		if (parentFolder != null) {

			props.put(traits.key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY), true);
			props.put(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY),    parentFolder);

		}

		return createFile(securityContext, fileStream, fileType, props);
	}

	/**
	 * Create a new file node from the given input stream and sets the parentFolder
	 *
	 * @param securityContext
	 * @param fileStream
	 * @param fileType defaults to File.class if null
	 * @param props
	 * @return file
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static NodeInterface createFile(final SecurityContext securityContext, final InputStream fileStream, final String fileType, final PropertyMap props) throws FrameworkException, IOException {

		final File newFile = StructrApp.getInstance(securityContext).create(fileType, props).as(File.class);

		setFileData(newFile, fileStream, props.get(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.CONTENT_TYPE_PROPERTY)));

		// schedule indexing
		newFile.notifyUploadCompletion();

		return newFile;
	}

	/**
	 * Create a new file node from the given byte array
	 *
	 * @param securityContext
	 * @param fileData
	 * @param contentType if null, try to auto-detect content type
	 * @param type
	 * @param name
	 * @param updateMetadata
	 * @return file
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static NodeInterface createFile(final SecurityContext securityContext, final byte[] fileData, final String contentType, String type, final String name, final boolean updateMetadata) throws FrameworkException, IOException {

		final PropertyMap props = new PropertyMap();

		props.put(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

		final File newFile = StructrApp.getInstance(securityContext).create(type, props).as(File.class);

		setFileData(newFile, fileData, contentType, updateMetadata);

		if (updateMetadata) {
			newFile.notifyUploadCompletion();
		}

		return newFile;
	}

	/**
	 * Create a new file node from the given byte array
	 *
	 * @param securityContext
	 * @param fileData
	 * @param contentType
	 * @param type defaults to File.class if null
	 * @return file
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static NodeInterface createFile(final SecurityContext securityContext, final byte[] fileData, final String contentType, final String type) throws FrameworkException, IOException {

		return createFile(securityContext, fileData, contentType, type, null, true);

	}

	/**
	 * Decodes base64-encoded raw data into binary data and writes it to the
	 * given file.
	 *
	 * @param file
	 * @param rawData
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void decodeAndSetFileData(final File file, final String rawData) throws FrameworkException, IOException {

		Base64URIData uriData = new Base64URIData(rawData);
		setFileData(file, uriData.getBinaryData(), uriData.getContentType(), true);
	}

	/**
	 * Write image data from the given byte[] to the given file node and set checksum and size.
	 *
	 * @param file
	 * @param fileData
	 * @param contentType if null, try to auto-detect content type
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void setFileData(final File file, final byte[] fileData, final String contentType) throws FrameworkException, IOException {
		FileHelper.setFileData(file, fileData, contentType, true);
	}

	/**
	 *
	 * @param file
	 * @param fileData
	 * @param contentType
	 * @param updateMetadata
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void setFileData(final File file, final byte[] fileData, final String contentType, final boolean updateMetadata) throws FrameworkException, IOException {

		FileHelper.writeToFile(file, fileData);

		if (updateMetadata) {
			setFilePropertiesOnCreation(file, contentType);
		}
	}

	/**
	 * Write image data from the given InputStream to the given file node and set checksum and size.
	 *
	 * @param file
	 * @param fileStream
	 * @param contentType if null, try to auto-detect content type
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void setFileData(final File file, final InputStream fileStream, final String contentType) throws FrameworkException, IOException {

		FileHelper.writeToFile(file, fileStream);
		setFilePropertiesOnCreation(file, contentType);
	}

	/**
	 * Set the contentType, checksum, size and version properties of the given fileNode on creation
	 *
	 * @param file
	 * @param contentType if null, try to auto-detect content type
	 * @throws FrameworkException
	 * @throws IOException
	 */
	private static void setFilePropertiesOnCreation(final File file, final String contentType) throws IOException, FrameworkException {

		final PropertyMap map = new PropertyMap();
		final Traits traits   = Traits.of(StructrTraits.FILE);

		map.put(traits.key(FileTraitDefinition.CONTENT_TYPE_PROPERTY), contentType != null ? contentType : FileHelper.getContentMimeType(file, file.getName()));
		map.put(traits.key(FileTraitDefinition.SIZE_PROPERTY),         FileHelper.getSize(file));
		map.put(traits.key(FileTraitDefinition.VERSION_PROPERTY),      1);

		map.putAll(getChecksums(file));

		if (file.is(StructrTraits.IMAGE)) {

			try {

				final BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
				final Traits imageTraits          = Traits.of(StructrTraits.IMAGE);

				if (bufferedImage != null) {

					map.put(imageTraits.key(ImageTraitDefinition.WIDTH_PROPERTY),  bufferedImage.getWidth());
					map.put(imageTraits.key(ImageTraitDefinition.HEIGHT_PROPERTY), bufferedImage.getHeight());
				}

			} catch (IOException ioe) {

				logger.warn("Unexpected IOException while reading image '{}'. Unable to extract width and height of image.", file.getPath());
			}
		}

		file.setProperties(file.getSecurityContext(), map, true);
	}

	/**
	 * Set the uuid and the path of a newly created fileNode
	 *
	 * @param fileNode
	 * @throws FrameworkException
	 */
	private static void setFilePropertiesOnCreation(final NodeInterface fileNode) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();

		String id = fileNode.getUuid();
		if (id == null) {

			final String newUuid = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
			id = newUuid;

			fileNode.unlockSystemPropertiesOnce();
			properties.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), newUuid);
		}

		fileNode.unlockSystemPropertiesOnce();
		fileNode.setProperties(fileNode.getSecurityContext(), properties, true);

	}

	/**
	 * Calculate checksums that are configured in settings of parent folder.
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private static PropertyMap getChecksums(final File file) throws IOException {

		final PropertyMap propertiesWithChecksums = new PropertyMap();
		final Traits traits                       = Traits.of(StructrTraits.FILE);

		Folder parentFolder = file.getParent();
		String checksums = null;

		while (parentFolder != null && checksums == null) {

			checksums    = parentFolder.getEnabledChecksums();
			parentFolder = parentFolder.getParent();
		}

		if (checksums == null) {
			checksums = Settings.DefaultChecksums.getValue();
		}

		// New, very fast xxHash default checksum, will always be calculated
		propertiesWithChecksums.put(traits.key(FileTraitDefinition.CHECKSUM_PROPERTY), FileHelper.getChecksum(file));

		if (StringUtils.contains(checksums, "crc32"))	{
			propertiesWithChecksums.put(traits.key(FileTraitDefinition.CRC32_PROPERTY), FileHelper.getCRC32Checksum(file));
		}

		if (StringUtils.contains(checksums, "md5"))	{
			propertiesWithChecksums.put(traits.key(FileTraitDefinition.MD5_PROPERTY), FileHelper.getMD5Checksum(file));
		}

		if (StringUtils.contains(checksums, "sha1"))	{
			propertiesWithChecksums.put(traits.key(FileTraitDefinition.SHA1_PROPERTY), FileHelper.getSHA1Checksum(file));
		}

		if (StringUtils.contains(checksums, "sha512"))	{
			propertiesWithChecksums.put(traits.key(FileTraitDefinition.SHA512_PROPERTY), FileHelper.getSHA512Checksum(file));
		}

		return propertiesWithChecksums;
	}
	/**
	 * Update checksums, content type, size and additional properties of the given file
	 *
	 * @param file the file
	 * @param map  additional properties
	 * @throws FrameworkException
	 */
	public static void updateMetadata(final File file, final PropertyMap map) throws FrameworkException, IOException {
		updateMetadata(file, map, false);
	}

	/**
	 * Update checksums (optional), content type, size and additional properties of the given file
	 *
	 * @param file the file
	 * @param map  additional properties
	 * @param calcChecksums
	 * @throws FrameworkException
	 */
	public static void updateMetadata(final File file, final PropertyMap map, final boolean calcChecksums) throws FrameworkException {

		if (file != null) {

			try {

				final Traits traits                             = file.getTraits();
				final PropertyKey<Long> fileModificationDateKey = traits.key(FileTraitDefinition.FILE_MODIFICATION_DATE_PROPERTY);
				final PropertyKey<String> contentTypeKey        = traits.key(FileTraitDefinition.CONTENT_TYPE_PROPERTY);
				final PropertyKey<Long> sizeKey                 = traits.key(FileTraitDefinition.SIZE_PROPERTY);

				String contentType = file.getContentType();

				// Don't overwrite existing MIME type
				if (StringUtils.isBlank(contentType)) {

					try {

						contentType = getContentMimeType(file);
						map.put(contentTypeKey, contentType);

					} catch (IOException ex) {
						logger.debug("Unable to detect content MIME type", ex);
					}
				}

				map.put(fileModificationDateKey, file.getLastModifiedDate().getTime());

				if (calcChecksums) {
					map.putAll(getChecksums(file));
				}

				if (contentType != null) {

					// modify type when image type is detected AND the type is StructrTraits.FILE
					if (contentType.startsWith("image/") && !file.getTraits().contains(StructrTraits.IMAGE) && StructrTraits.FILE.equals(file.getType())) {
						map.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), StructrTraits.IMAGE);
					}
				}

				long fileSize = FileHelper.getSize(file);
				if (fileSize >= 0) {

					map.put(sizeKey, fileSize);
				}

				file.unlockSystemPropertiesOnce();
				file.setProperties(SecurityContext.getSuperUserInstance(), map);

			} catch (IOException ioex) {
				logger.warn("Unable to access {} on disk: {}", file.getPath(), ioex.getMessage());
			}
		}
	}

	/**
	 * Update checksums, content type and size of the given file
	 *
	 * @param file the file
	 * @throws FrameworkException
	 */
	public static void updateMetadata(final File file) throws FrameworkException, IOException {
		updateMetadata(file, new PropertyMap(), false);
	}

	/**
	 * Update checksums, content type and size of the given file
	 *
	 * @param file the file
	 * @param calcChecksums
	 * @throws FrameworkException
	 */
	public static void updateMetadata(final File file, final boolean calcChecksums) throws FrameworkException {
		updateMetadata(file, new PropertyMap(), calcChecksums);
	}

	public static String getBase64String(final File file) {


		try (final InputStream is = file.getInputStream()) {

			if (is != null) {

				return Base64.encodeToString(IOUtils.toByteArray(is), false);
			}

		} catch (IOException ex) {
			logger.error("Could not get base64 string from file ", ex);
		}

		return null;
	}

	public static class Base64URIData {

		private String contentType = null;
		private String data = null;

		public Base64URIData(final String rawData) {

			if (rawData.contains(",")) {

				String[] parts = StringUtils.split(rawData, ",");
				if (parts.length == 2) {

					contentType = StringUtils.substringBetween(parts[0], "data:", ";base64");
					data        = parts[1];
				}

			} else {

				data = rawData;
			}
		}

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
	 * Write binary data from byte[] to a file and reference the file on disk at the
	 * given file node
	 *
	 * @param fileNode
	 * @param data
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void writeToFile(final File fileNode, final byte[] data) throws FrameworkException, IOException {

		setFilePropertiesOnCreation(fileNode);

		try (final InputStream is = new ByteArrayInputStream(data); final OutputStream os = StorageProviderFactory.getStorageProvider(fileNode).getOutputStream()) {
			IOUtils.copy(is, os);
		}

	}

	/**
	 * Write binary data from FileInputStream to a file and reference the file on disk at the given file node
	 *
	 * @param fileNode
	 * @param data	The input stream from which to read the file data (Stream is not closed automatically - has to be handled by caller)
	 * @throws FrameworkException
	 * @throws IOException
	 */
	public static void writeToFile(final File fileNode, final InputStream data) throws FrameworkException, IOException {

		setFilePropertiesOnCreation(fileNode);

		try (final OutputStream out = StorageProviderFactory.getStorageProvider(fileNode).getOutputStream()) {

			IOUtils.copy(data, out);
		}
	}

	/**
	 * Return mime type of given file
	 *
	 * @param file
	 * @return content type
	 * @throws java.io.IOException
	 */
	public static String getContentMimeType(final File file) throws IOException {
		return getContentMimeType(file, file.getName());
	}

	/**
	 * Return mime type of given file
	 *
	 * @param file
	 * @param name
	 * @return content type
	 * @throws java.io.IOException
	 */
	public static String getContentMimeType(final File file, final String name) throws IOException {

		String mimeType;

		// try name first, if not null
		if (name != null) {
			mimeType = mimeTypeMap.getContentType(name);
			if (mimeType != null && !UNKNOWN_MIME_TYPE.equals(mimeType)) {
				return mimeType;
			}
		}

		try (final InputStream is = new BufferedInputStream(StorageProviderFactory.getStorageProvider(file).getInputStream())) {

			final MediaType mediaType = new DefaultDetector().detect(is, new Metadata());
			if (mediaType != null) {

				mimeType = mediaType.toString();
				if (mimeType != null) {

					return mimeType;
				}
			}

		} catch (NoClassDefFoundError t) {
			logger.warn("Unable to instantiate MIME type detector: {}", t.getMessage());
		}

		// no success :(
		return UNKNOWN_MIME_TYPE;
	}

	/**
	 * Return mime type of given file
	 *
	 * @param file
	 * @param name
	 * @return content type
	 * @throws java.io.IOException
	 */
	public static String getContentMimeType(final java.io.File file, final String name) throws IOException {

		String mimeType;

		// try name first, if not null
		if (name != null) {
			mimeType = mimeTypeMap.getContentType(name);
			if (mimeType != null && !UNKNOWN_MIME_TYPE.equals(mimeType)) {
				return mimeType;
			}
		}

		try (final InputStream is = new BufferedInputStream(new FileInputStream(file))) {

			final MediaType mediaType = new DefaultDetector().detect(is, new Metadata());
			if (mediaType != null) {

				mimeType = mediaType.toString();
				if (mimeType != null) {

					return mimeType;
				}
			}

		} catch (NoClassDefFoundError t) {
			logger.warn("Unable to instantiate MIME type detector: {}", t.getMessage());
		}

		// no success :(
		return UNKNOWN_MIME_TYPE;
	}

	/**
	 * Find a file by its absolute ancestor path.
	 *
	 * File may not be hidden or deleted.
	 *
	 * @param securityContext
	 * @param absolutePath
	 * @return file
	 */
	public static NodeInterface getFileByAbsolutePath(final SecurityContext securityContext, final String absolutePath) {

		try {

			final Traits traits = Traits.of(StructrTraits.ABSTRACT_FILE);

			return StructrApp.getInstance(securityContext).nodeQuery(StructrTraits.ABSTRACT_FILE).key(traits.key(AbstractFileTraitDefinition.PATH_PROPERTY), absolutePath).getFirst();

		} catch (FrameworkException ex) {
			logger.warn("File not found: {}", absolutePath);
		}

		return null;
	}

	public static NodeInterface getFileByUuid(final SecurityContext securityContext, final String uuid) {

		logger.debug("Search for file with uuid: {}", uuid);

		try {
			return StructrApp.getInstance(securityContext).getNodeById(StructrTraits.ABSTRACT_FILE, uuid);

		} catch (FrameworkException fex) {

			logger.warn("Unable to find a file by UUID {}: {}", uuid, fex.getMessage());
		}

		return null;
	}

	public static NodeInterface getFirstFileByName(final SecurityContext securityContext, final String name) {

		logger.debug("Search for file with name: {}", name);

		try {
			return StructrApp.getInstance(securityContext).nodeQuery(StructrTraits.ABSTRACT_FILE).name(name).getFirst();

		} catch (FrameworkException fex) {

			logger.warn("Unable to find a file for name {}: {}", name, fex.getMessage());
		}

		return null;
	}

	/**
	 * Find the first file with given name on root level (without parent folder).
	 *
	 * @param securityContext
	 * @param name
	 * @return file
	 */
	public static NodeInterface getFirstRootFileByName(final SecurityContext securityContext, final String name) {

		logger.debug("Search for file with name: {}", name);

		try {
			final List<NodeInterface> files = StructrApp.getInstance(securityContext).nodeQuery(StructrTraits.ABSTRACT_FILE).name(name).getAsList();

			for (final NodeInterface file : files) {

				if (file.as(File.class).getParent() == null) {
					return file;
				}

			}

		} catch (FrameworkException fex) {

			logger.warn("Unable to find a file for name {}: {}", name, fex.getMessage());
		}

		return null;
	}

	/**
	 * Create one folder per path item and return the last folder.
	 *
	 * F.e.: /a/b/c => Folder["name":"a"] --HAS_CHILD--> Folder["name":"b"]
	 * --HAS_CHILD--> Folder["name":"c"], returns Folder["name":"c"]
	 *
	 * @param securityContext
	 * @param path
	 * @return folder
	 * @throws FrameworkException
	 */
	public static NodeInterface createFolderPath(final SecurityContext securityContext, final String path) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		if (path == null) {

			return null;
		}

		NodeInterface folder = FileHelper.getFileByAbsolutePath(securityContext, path);

		if (folder != null) {

			return folder;
		}

		String[] parts = PathHelper.getParts(path);
		String partialPath = "";

		for (String part : parts) {

			// ignore ".." and "." in paths
			if ("..".equals(part) || ".".equals(part)) {
				continue;
			}

			NodeInterface parent = folder;

			partialPath += PathHelper.PATH_SEP + part;

			NodeInterface potentialFolder = FileHelper.getFileByAbsolutePath(securityContext, partialPath);

			folder = potentialFolder;

			if (folder == null) {

				folder = app.create(StructrTraits.FOLDER, part);

			}

			if (parent != null) {

				folder.as(Folder.class).setParent(parent.as(Folder.class));

			}
		}

		return folder;

	}

	/**
	 * Unarchives a file in the (optional) folder with the given folder id.
	 *
	 * @param securityContext
	 * @param file
	 * @param parentFolderId
	 * @throws ArchiveException
	 * @throws IOException
	 * @throws FrameworkException
	 */
	public static void unarchive(final SecurityContext securityContext, final File file, final String parentFolderId) throws ArchiveException, IOException, FrameworkException {

		if (file == null) {
			logger.error("Unable to unarchive file (file parameter was null).");
			return;
		}

		Folder existingParentFolder = null;
		final App app               = StructrApp.getInstance(securityContext);
		final String fileName       = file.getName();

		if (parentFolderId != null) {

			try (final Tx tx = app.tx(true, true, true)) {

				// search for existing parent folder
				final NodeInterface node = app.getNodeById(StructrTraits.FOLDER, parentFolderId);
				String parentFolderName = null;

				String msgString = "Unarchiving file {}";
				if (node != null) {

					existingParentFolder = node.as(Folder.class);

					parentFolderName = existingParentFolder.getName();
					msgString += " into existing folder {}.";
				}

				logger.info(msgString, fileName, parentFolderName);

				tx.success();
			}

		} else {

			existingParentFolder = file.getParent();
		}

		BufferedInputStream bufferedIs = null;

		try (final Tx tx = app.tx()) {

			bufferedIs = new BufferedInputStream(file.getInputStream());

			tx.success();
		}

		switch (ArchiveStreamFactory.detect(bufferedIs)) {

			// 7z doesn't support streaming
			case ArchiveStreamFactory.SEVEN_Z: {
				int overallCount = 0;

				logger.info("7-Zip archive format detected");

				try (final Tx outertx = app.tx()) {

					SevenZFile sevenZFile = new SevenZFile(StorageProviderFactory.getStorageProvider(file).getSeekableByteChannel());

					SevenZArchiveEntry sevenZEntry = sevenZFile.getNextEntry();

					while (sevenZEntry != null) {

						try (final Tx tx = app.tx(true, true, false)) {

							int count = 0;

							while (sevenZEntry != null && count++ < 50) {

								final String entryPath = "/" + PathHelper.clean(sevenZEntry.getName());
								logger.info("Entry path: {}", entryPath);

								if (sevenZEntry.isDirectory()) {

									handleDirectory(securityContext, existingParentFolder, entryPath);

								} else {

									byte[] buf = new byte[(int) sevenZEntry.getSize()];
									sevenZFile.read(buf, 0, buf.length);

									try (final ByteArrayInputStream in = new ByteArrayInputStream(buf)) {
										handleFile(securityContext, in, existingParentFolder, entryPath);
									}
								}

								sevenZEntry = sevenZFile.getNextEntry();

								overallCount++;
							}

							logger.info("Committing transaction after {} entries.", overallCount);
							tx.success();
						}

					}

					logger.info("Unarchived {} files.", overallCount);
					outertx.success();
				}

				break;
			}

			// ZIP needs special treatment to support "unsupported feature data descriptor"
			case ArchiveStreamFactory.ZIP: {

				logger.info("Zip archive format detected");

				try (final ZipArchiveInputStream in = new ZipArchiveInputStream(bufferedIs, null, false, true)) {

					handleArchiveInputStream(in, app, securityContext, existingParentFolder);
				}

				break;
			}

			default: {

				logger.info("Default archive format detected");

				try (final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(bufferedIs)) {

					handleArchiveInputStream(in, app, securityContext, existingParentFolder);
				}
			}
		}
	}

	private static void handleArchiveInputStream(final ArchiveInputStream in, final App app, final SecurityContext securityContext, final Folder existingParentFolder) throws FrameworkException, IOException {

		int overallCount = 0;

		ArchiveEntry entry = in.getNextEntry();

		while (entry != null) {

			try (final Tx tx = app.tx(true, true, false)) { // don't send notifications for bulk commands

				int count = 0;

				while (entry != null && count++ < 50) {

					final String entryPath = "/" + PathHelper.clean(entry.getName());
					logger.info("Entry path: {}", entryPath);

					if (entry.isDirectory()) {

						handleDirectory(securityContext, existingParentFolder, entryPath);

					} else {

						handleFile(securityContext, in, existingParentFolder, entryPath);
					}

					entry = in.getNextEntry();

					overallCount++;
				}

				logger.info("Committing transaction after {} entries.", overallCount);

				tx.success();
			}
		}

		logger.info("Unarchived {} entries.", overallCount);
	}

	private static void handleDirectory(final SecurityContext securityContext, final Folder existingParentFolder, final String entryPath) throws FrameworkException {

		final String folderPath = (existingParentFolder != null ? existingParentFolder.getPath() : "") + PathHelper.PATH_SEP + entryPath;

		FileHelper.createFolderPath(securityContext, folderPath);
	}

	private static void handleFile(final SecurityContext securityContext, final InputStream in, final Folder existingParentFolder, final String entryPath) throws FrameworkException, IOException {

		final Traits traits                        = Traits.of(StructrTraits.ABSTRACT_FILE);
		final PropertyKey<NodeInterface> parentKey = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final PropertyKey<Boolean> hasParentKey    = traits.key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY);
		final String filePath                      = (existingParentFolder != null ? existingParentFolder.getPath() : "") + PathHelper.PATH_SEP + PathHelper.clean(entryPath);
		final String name                          = PathHelper.getName(entryPath);
		final NodeInterface newFile                = ImageHelper.isImageType(name)
				? ImageHelper.createImage(securityContext, in, null, StructrTraits.IMAGE, name, false)
				: FileHelper.createFile(securityContext, in, null, StructrTraits.FILE, name);

		final String folderPath = StringUtils.substringBeforeLast(filePath, PathHelper.PATH_SEP);
		final NodeInterface parentFolder = FileHelper.createFolderPath(securityContext, folderPath);

		if (parentFolder != null) {

			final PropertyMap properties = new PropertyMap();

			properties.put(parentKey,    parentFolder);
			properties.put(hasParentKey, true);

			newFile.setProperties(securityContext, properties);
		}
	}


	public static String getDateString() {
		return new SimpleDateFormat("yyyy-MM-dd-HHmmssSSS").format(new Date());
	}


	public static Long getChecksum(final File file) throws IOException {
		StorageProvider sp = StorageProviderFactory.getStorageProvider(file);
		return getChecksum(sp.getInputStream(), sp.size());
	}

	public static Long getChecksum(final java.io.File file) throws IOException {
		return getChecksum(new FileInputStream(file), file.length());
	}

	public static Long getChecksum(final InputStream inputStream, long size) throws IOException {

		try (final BufferedInputStream is = new BufferedInputStream(inputStream, 131072)) {

			final long hash = LongHashFunction.xx().hash(is, new Access<BufferedInputStream>() {

				@Override
				public int getByte(BufferedInputStream input, long offset) {

					try { return input.read(); } catch (IOException ex) {}

					return -1;
				}

				@Override
				public ByteOrder byteOrder(BufferedInputStream input) {
					return ByteOrder.nativeOrder();
				}

			}, 0, size);

			return hash;

		} catch (final IOException ex) {
			logger.warn("Unable to calculate checksum for {}: {}", inputStream, ex.getMessage());
		}

		return null;
	}

	public static Long getCRC32Checksum(final File file) throws IOException {
		final CRC32 crc32 = new CRC32();
		final InputStream is = StorageProviderFactory.getStorageProvider(file).getInputStream();
		byte[] buf = new byte[1024];
		int length;
		while ((length = is.read(buf)) != -1) {
			crc32.update(buf, 0, length);
		}

		return crc32.getValue();
	}

	public static String getMD5Checksum(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return DigestUtils.md5Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate MD5 checksum of file represented by node " + file, ex);
		}

		return null;
	}

	public static String getMD5Checksum(final java.io.File fileOnDisk) {

		try (final InputStream is = FileUtils.openInputStream(fileOnDisk)) {

			return DigestUtils.md5Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate MD5 checksum of file " + fileOnDisk, ex);
		}

		return null;
	}

	public static String getSHA1Checksum(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return DigestUtils.sha1Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-1 checksum of file represented by node " + file, ex);
		}

		return null;
	}

	public static String getSHA1Checksum(final java.io.File fileOnDisk) {

		try (final InputStream is = FileUtils.openInputStream(fileOnDisk)) {

			return DigestUtils.sha1Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-1 checksum of file " + fileOnDisk, ex);
		}

		return null;
	}

	public static String getSHA256Checksum(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return DigestUtils.sha256Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-256 checksum of file represented by node " + file, ex);
		}

		return null;
	}

	public static String getSHA256Checksum(final java.io.File fileOnDisk) {

		try (final InputStream is = FileUtils.openInputStream(fileOnDisk)) {

			return DigestUtils.sha256Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-256 checksum of file " + fileOnDisk, ex);
		}

		return null;
	}

	public static String getSHA384Checksum(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return DigestUtils.sha384Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-384 checksum of file represented by node " + file, ex);
		}

		return null;
	}

	public static String getSHA384Checksum(final java.io.File fileOnDisk) {

		try (final InputStream is = FileUtils.openInputStream(fileOnDisk)) {

			return DigestUtils.sha384Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-384 checksum of file " + fileOnDisk, ex);
		}

		return null;
	}

	public static String getSHA512Checksum(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return DigestUtils.sha512Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-512 checksum of file represented by node " + file, ex);
		}

		return null;
	}

	public static String getSHA512Checksum(final java.io.File fileOnDisk) {

		try (final InputStream is = FileUtils.openInputStream(fileOnDisk)) {

			return DigestUtils.sha512Hex(is);

		} catch (final IOException ex) {
			logger.warn("Unable to calculate SHA-512 checksum of file " + fileOnDisk, ex);
		}

		return null;
	}

	public static long getSize(final File file) {

		try {

			return StorageProviderFactory.getStorageProvider(file).size();

		} catch (Exception ex) {

			logger.warn("Could not calculate file size of file {}: {}", file.getPath(), ex.getMessage());
		}

		return -1;

	}

	public static void prefetchFileData(final String uuid) {

		TransactionCommand.getCurrentTransaction().prefetch2(

			"MATCH (n:NodeInterface:AbstractFile { id: $id })<-[r:CONTAINS*]-(x) WITH collect(DISTINCT x) AS nodes, collect(DISTINCT last(r)) AS rels RETURN nodes, rels",

			Set.of(
				"all/OUTGOING/CONTAINS",
				"all/INCOMING/CONTAINS"
			),

			Set.of(
				"all/OUTGOING/CONTAINS",
				"all/INCOMING/CONTAINS"
			),

			uuid
		);

		TransactionCommand.getCurrentTransaction().prefetch(

			"(n:NodeInterface:AbstractFile)-[r:CONFIGURED_BY]->(m:StorageConfiguration)",

			Set.of(
				"AbstractFile/all/OUTGOING/CONFIGURED_BY",
				"AbstractFile/all/INCOMING/CONFIGURED_BY"
			)
		);
	}
}
