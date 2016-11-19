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
package org.structr.web.entity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.cmis.info.CMISPolicyInfo;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.cmis.info.CMISSecondaryInfo;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import static org.structr.common.fulltext.Indexable.extractedContent;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.files.cmis.config.StructrFileActions;
import org.structr.schema.action.JavaScriptSource;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.relation.Folders;
import org.structr.web.entity.relation.MinificationSource;
import org.structr.web.entity.relation.UserFavoriteFile;
import org.structr.web.property.FileDataProperty;

/**
 *
 *
 */
public class FileBase extends AbstractFile implements Indexable, Linkable, JavaScriptSource, CMISInfo, CMISDocumentInfo {

	private static final Logger logger = LoggerFactory.getLogger(FileBase.class.getName());

	public static final Property<String> relativeFilePath                        = new StringProperty("relativeFilePath").systemInternal();
	public static final Property<Long> size                                      = new LongProperty("size").indexed().systemInternal();
	public static final Property<String> url                                     = new StringProperty("url");
	public static final Property<Long> checksum                                  = new LongProperty("checksum").indexed().unvalidated().systemInternal();
	public static final Property<Integer> cacheForSeconds                        = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                                = new IntProperty("version").indexed().systemInternal();
	public static final Property<String> base64Data                              = new FileDataProperty<>("base64Data");
	public static final Property<Boolean> isFile                                 = new ConstantBooleanProperty("isFile", true);
	public static final Property<List<AbstractMinifiedFile>> minificationTargets = new StartNodes<>("minificationTarget", MinificationSource.class);
	public static final Property<List<User>> favoriteOfUsers                     = new StartNodes<>("favoriteOfUsers", UserFavoriteFile.class);

	public static final View publicView = new View(FileBase.class, PropertyView.Public,
		type, name, size, url, owner, path, isFile, visibleToPublicUsers, visibleToAuthenticatedUsers
	);

	public static final View uiView = new View(FileBase.class, PropertyView.Ui,
		type, relativeFilePath, size, url, parent, checksum, version, cacheForSeconds, owner, isFile, hasParent
	);

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			final PropertyMap changedProperties = new PropertyMap();

			if ("true".equals(StructrApp.getConfigurationValue(Services.APPLICATION_FILESYSTEM_ENABLED, "false")) && !getProperty(AbstractFile.hasParent)) {

				final Folder workingOrHomeDir = getCurrentWorkingDir();
				if (workingOrHomeDir != null && getProperty(AbstractFile.parent) == null) {

					changedProperties.put(AbstractFile.parent, workingOrHomeDir);
				}
			}

			changedProperties.put(hasParent, getProperty(parentId) != null);

			setProperties(securityContext, changedProperties);

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			synchronized (this) {

				// save current security context
				final SecurityContext previousSecurityContext = securityContext;

				// replace with SU context
				this.securityContext = SecurityContext.getSuperUserInstance();

				// set property as super user
				setProperties(this.securityContext, new PropertyMap(hasParent, getProperty(parentId) != null));

				// restore previous security context
				this.securityContext = previousSecurityContext;
			}

			triggerMinificationIfNeeded(modificationQueue);

			return true;
		}

		return false;
	}

	@Override
	public void onNodeCreation() {

		final String uuid = getUuid();
		final String filePath = getDirectoryPath(uuid) + "/" + uuid;

		try {
			unlockSystemPropertiesOnce();
			setProperties(securityContext, new PropertyMap(relativeFilePath, filePath));

		} catch (Throwable t) {

			logger.warn("Exception while trying to set relative file path {}: {}", new Object[]{filePath, t});

		}
	}

	@Override
	public void onNodeDeletion() {

		String filePath = null;
		try {
			final String path = getRelativeFilePath();

			if (path != null) {

				filePath = FileHelper.getFilePath(path);

				java.io.File toDelete = new java.io.File(filePath);

				if (toDelete.exists() && toDelete.isFile()) {

					toDelete.delete();
				}
			}

		} catch (Throwable t) {

			logger.debug("Exception while trying to delete file {}: {}", new Object[]{filePath, t});

		}

	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			final String filesPath = Services.getInstance().getConfigurationValue(Services.FILES_PATH);
			java.io.File fileOnDisk = new java.io.File(filesPath + "/" + getRelativeFilePath());

			if (fileOnDisk.exists()) {
				return;
			}

			fileOnDisk.getParentFile().mkdirs();

			try {

				fileOnDisk.createNewFile();

			} catch (IOException ex) {

				logger.error("Could not create file", ex);
				return;
			}

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(checksum, FileHelper.getChecksum(FileBase.this));
			changedProperties.put(version, 0);

			long fileSize = FileHelper.getSize(FileBase.this);
			if (fileSize > 0) {
				changedProperties.put(size, fileSize);
			}

			unlockSystemPropertiesOnce();
			setProperties(securityContext, changedProperties);

		} catch (FrameworkException ex) {

			logger.error("Could not create file", ex);

		}

	}

	@Override
	public String getPath() {
		return FileHelper.getFolderPath(this);
	}

	@Export
	@Override
	public GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(extractedContent);
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	public void notifyUploadCompletion() {

		try {
			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			indexer.addToFulltextIndex(this);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index " + this, fex);
		}
	}

	public String getRelativeFilePath() {

		return getProperty(FileBase.relativeFilePath);

	}

	public String getUrl() {

		return getProperty(FileBase.url);

	}

	@Override
	public String getContentType() {

		return getProperty(FileBase.contentType);

	}

	@Override
	public Long getSize() {

		return getProperty(size);

	}

	public Long getChecksum() {

		return getProperty(checksum);

	}

	public String getFormattedSize() {

		return FileUtils.byteCountToDisplaySize(getSize());

	}

	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(FileBase.version);

		unlockSystemPropertiesOnce();
		if (_version == null) {

			setProperties(securityContext, new PropertyMap(FileBase.version, 1));

		} else {

			setProperties(securityContext, new PropertyMap(FileBase.version, _version + 1));
		}
	}

	public void triggerMinificationIfNeeded(ModificationQueue modificationQueue) throws FrameworkException {

		final List<AbstractMinifiedFile> targets = getProperty(minificationTargets);

		if (!targets.isEmpty()) {

			// only run minification if the file version changed
			boolean versionChanged = false;
			for (ModificationEvent modState : modificationQueue.getModificationEvents()) {

				if (getUuid().equals(modState.getUuid())) {

					versionChanged = versionChanged ||
							modState.getRemovedProperties().containsKey(FileBase.version) ||
							modState.getModifiedProperties().containsKey(FileBase.version) ||
							modState.getNewProperties().containsKey(FileBase.version);
				}
			}

			if (versionChanged) {

				for (AbstractMinifiedFile minifiedFile : targets) {

					try {
						minifiedFile.minify();
					} catch (IOException ex) {
						logger.warn("Could not automatically update minification target: ".concat(minifiedFile.getName()), ex);
					}

				}

			}

		}

	}

	@Override
	public InputStream getInputStream() {

		final String path = getRelativeFilePath();

		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			FileInputStream fis = null;
			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file input stream and save checksum and size after closing
				fis = new FileInputStream(fileOnDisk);

				return fis;

			} catch (FileNotFoundException e) {
				logger.debug("File not found: {}", new Object[]{path});

				if (fis != null) {

					try {

						fis.close();

					} catch (IOException ignore) {
					}

				}
			}
		}

		return null;
	}

	public FileOutputStream getOutputStream() {
		return getOutputStream(true, false);
	}

	public FileOutputStream getOutputStream(final boolean notifyIndexerAfterClosing, final boolean append) {

		final String path = getRelativeFilePath();
		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			try {

				final java.io.File fileOnDisk = new java.io.File(filePath);
				fileOnDisk.getParentFile().mkdirs();

				// Return file output stream and save checksum and size after closing
				final FileOutputStream fos = new FileOutputStream(fileOnDisk, append) {

					private boolean closed = false;

					@Override
					public void close() throws IOException {

						if (closed) {
							return;
						}

						try (Tx tx = StructrApp.getInstance().tx()) {

							super.close();

							final String _contentType = FileHelper.getContentMimeType(FileBase.this);

							final PropertyMap changedProperties = new PropertyMap();
							changedProperties.put(checksum, FileHelper.getChecksum(FileBase.this));
							changedProperties.put(size, FileHelper.getSize(FileBase.this));
							changedProperties.put(contentType, _contentType);

							if (StringUtils.startsWith(_contentType, "image") || ImageHelper.isImageType(getProperty(name))) {
								changedProperties.put(NodeInterface.type, Image.class.getSimpleName());
							}

							unlockSystemPropertiesOnce();
							setProperties(securityContext, changedProperties);

							increaseVersion();

							if (notifyIndexerAfterClosing) {
								notifyUploadCompletion();
							}

							tx.success();

						} catch (Throwable ex) {

							logger.error("Could not determine or save checksum and size after closing file output stream", ex);

						}

						closed = true;
					}
				};

				return fos;

			} catch (FileNotFoundException e) {

				logger.error("File not found: {}", path);
			}

		}

		return null;

	}

	public java.io.File getFileOnDisk() {

		final String path = getRelativeFilePath();
		if (path != null) {

			return new java.io.File(FileHelper.getFilePath(path));
		}

		return null;
	}

	public Path getPathOnDisk() {

		final String path = getRelativeFilePath();
		if (path != null) {

			return Paths.get(FileHelper.getFilePath(path));
		}

		return null;
	}

	// ----- private methods -----
	/**
	 * Returns the Folder entity for the current working directory,
	 * or the user's home directory as a fallback.
	 * @return
	 */
	private Folder getCurrentWorkingDir() {

		final Principal _owner  = getProperty(owner);
		Folder workingOrHomeDir = null;

		if (_owner != null && _owner instanceof User) {

			workingOrHomeDir = _owner.getProperty(User.workingDirectory);
			if (workingOrHomeDir == null) {

				workingOrHomeDir = _owner.getProperty(User.homeDirectory);
			}
		}

		return workingOrHomeDir;
	}

	private int flushWordBuffer(final StringBuilder lineBuffer, final StringBuilder wordBuffer, final boolean prepend) {

		int wordCount = 0;

		if (wordBuffer.length() > 0) {

			final String word = wordBuffer.toString().replaceAll("[\\n\\t]+", " ");
			if (StringUtils.isNotBlank(word)) {

				if (prepend) {

					lineBuffer.insert(0, word);

				} else {

					lineBuffer.append(word);
				}

				// increase word count
				wordCount = 1;
			}

			wordBuffer.setLength(0);
		}

		return wordCount;
	}

	// ----- interface Syncable -----
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {

		final List<GraphObject> data = super.getSyncData();

		// nodes
		data.add(getProperty(parent));
		data.add(getIncomingRelationship(Folders.class));

		return data;
	}

	// ----- interface JavaScriptSource -----
	@Override
	public String getJavascriptLibraryCode() {

		try (final InputStream is = getInputStream()) {

			return IOUtils.toString(is);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		return null;
	}

	// ----- CMIS support -----
	@Override
	public CMISInfo getCMISInfo() {
		return this;
	}

	@Override
	public BaseTypeId getBaseTypeId() {
		return BaseTypeId.CMIS_DOCUMENT;
	}

	@Override
	public CMISFolderInfo getFolderInfo() {
		return null;
	}

	@Override
	public CMISDocumentInfo getDocumentInfo() {
		return this;
	}

	@Override
	public CMISItemInfo geItemInfo() {
		return null;
	}

	@Override
	public CMISRelationshipInfo getRelationshipInfo() {
		return null;
	}

	@Override
	public CMISPolicyInfo getPolicyInfo() {
		return null;
	}

	@Override
	public CMISSecondaryInfo getSecondaryInfo() {
		return null;
	}

	@Override
	public String getParentId() {
		return getProperty(FileBase.parentId);
	}

	@Override
	public AllowableActions getAllowableActions() {
		return new StructrFileActions(isImmutable());
	}

	@Override
	public String getChangeToken() {

		// versioning not supported yet.
		return null;
	}

	@Override
	public boolean isImmutable() {

		final Principal _owner = getOwnerNode();
		if (_owner != null) {

			return !_owner.isGranted(Permission.write, securityContext);
		}

		return true;
	}
}
