/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.files.cmis;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.files.cmis.wrapper.CMISContentStream;
import org.structr.files.cmis.wrapper.CMISPagingListWrapper;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.File;

/**
 *
 *
 */
public class CMISObjectService extends AbstractStructrCmisService implements ObjectService {

	private static final Logger logger = LoggerFactory.getLogger(CMISObjectService.class.getName());

	public CMISObjectService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	@Override
	public String createDocument(final String repositoryId, final Properties properties, final String folderId, final ContentStream contentStream, final VersioningState versioningState, final List<String> policies, final Acl addAces, final Acl removeAces, final ExtensionsData extension) {

		final App app = StructrApp.getInstance(securityContext);
		File newFile  = null;
		String uuid       = null;

		try (final Tx tx = app.tx()) {

			final String objectTypeId = getStringValue(properties, PropertyIds.OBJECT_TYPE_ID);
			final String fileName     = getStringValue(properties, PropertyIds.NAME);
			final Class type          = typeFromObjectTypeId(objectTypeId, BaseTypeId.CMIS_DOCUMENT, File.class);

			// check if type exists
			if (type != null) {

				// check that base type is cmis:folder
				final BaseTypeId baseTypeId = getBaseTypeId(type);
				if (baseTypeId != null && BaseTypeId.CMIS_DOCUMENT.equals(baseTypeId)) {

					final String mimeType = contentStream != null ? contentStream.getMimeType() : null;

					// create file
					newFile = FileHelper.createFile(securityContext, new byte[0], mimeType, type, fileName, false);
					if (newFile != null) {

						// find and set parent if it exists
						if (!CMISInfo.ROOT_FOLDER_ID.equals(folderId)) {

							final Folder parent = app.get(Folder.class, folderId);
							if (parent != null) {

								newFile.setParent(parent);

							} else {

								throw new CmisObjectNotFoundException("Folder with ID " + folderId + " does not exist");
							}
						}

						uuid = newFile.getUuid();

						if (contentStream != null) {

							final InputStream inputStream = contentStream.getStream();
							if (inputStream != null) {

								// copy file and update metadata
								try (final OutputStream outputStream = newFile.getOutputStream(false, false)) {
									IOUtils.copy(inputStream, outputStream);
								}

								inputStream.close();

								FileHelper.updateMetadata(newFile);
							}
						}
					}

				} else {

					throw new CmisConstraintException("Cannot create cmis:document of type " + objectTypeId);
				}

			} else {

				throw new CmisObjectNotFoundException("Type with ID " + objectTypeId + " does not exist");
			}

			tx.success();

		} catch (Throwable t) {

			throw new CmisRuntimeException("New document could not be created: " + t.getMessage());
		}

		// start indexing after transaction is finished
		if (newFile != null) {
			newFile.notifyUploadCompletion();
		}

		return uuid;
	}

	@Override
	public String createDocumentFromSource(String repositoryId, String sourceId, Properties properties, String folderId, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {

		// copy existing document
		final App app = StructrApp.getInstance(securityContext);
		String uuid   = null;

		try (final Tx tx = app.tx()) {

			final File existingDocument = app.get(File.class, sourceId);
			if (existingDocument != null) {

				try (final InputStream inputStream = existingDocument.getInputStream()) {

					final ContentStreamImpl copyContentStream = new ContentStreamImpl();
					copyContentStream.setFileName(existingDocument.getName());
					copyContentStream.setMimeType(existingDocument.getContentType());
					copyContentStream.setLength(BigInteger.valueOf(existingDocument.getSize()));
					copyContentStream.setStream(inputStream);

					uuid = createDocument(repositoryId, properties, folderId, copyContentStream, versioningState, policies, addAces, removeAces, extension);
				}

			} else {

				throw new CmisObjectNotFoundException("Document with ID " + sourceId + " does not exist");
			}

			tx.success();

		} catch (Throwable t) {

			throw new CmisRuntimeException("New document could not be created: " + t.getMessage());
		}

		return uuid;
	}

	@Override
	public String createFolder(final String repositoryId, final Properties properties, final String folderId, final List<String> policies, final Acl addAces, final Acl removeAces, final ExtensionsData extension) {

		final App app = StructrApp.getInstance(securityContext);
		String uuid   = null;

		try (final Tx tx = app.tx()) {

			final String objectTypeId = getStringValue(properties, PropertyIds.OBJECT_TYPE_ID);
			final Class type          = typeFromObjectTypeId(objectTypeId, BaseTypeId.CMIS_FOLDER, Folder.class);

			// check if type exists
			if (type != null) {

				// check that base type is cmis:folder
				final BaseTypeId baseTypeId = getBaseTypeId(type);
				if (baseTypeId != null && BaseTypeId.CMIS_FOLDER.equals(baseTypeId)) {

					// create folder
					final AbstractFile newFolder = (AbstractFile)app.create(type, PropertyMap.cmisTypeToJavaType(securityContext, type, properties));

					// find and set parent if it exists
					if (!CMISInfo.ROOT_FOLDER_ID.equals(folderId)) {

						final Folder parent = app.get(Folder.class, folderId);
						if (parent != null) {

							newFolder.setParent(parent);

						} else {

							throw new CmisObjectNotFoundException("Folder with ID " + folderId + " does not exist");
						}
					}

					uuid = newFolder.getUuid();

				} else {

					throw new CmisConstraintException("Cannot create cmis:folder of type " + objectTypeId);
				}

			} else {

				throw new CmisObjectNotFoundException("Type with ID " + objectTypeId + " does not exist");
			}

			tx.success();

		} catch (Throwable t) {

			throw new CmisRuntimeException("New folder could not be created: " + t.getMessage());
		}

		return uuid;
	}

	@Override
	public String createRelationship(String repositoryId, Properties properties, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String createPolicy(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String createItem(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public AllowableActions getAllowableActions(String repositoryId, String objectId, ExtensionsData extension) {

		final ObjectData obj = getObject(repositoryId, objectId, null, true, IncludeRelationships.NONE, null, false, false, extension);
		if (obj != null) {

			return obj.getAllowableActions();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public ObjectData getObject(final String repositoryId, final String objectId, final String propertyFilter, final Boolean includeAllowableActions, final IncludeRelationships includeRelationships, final String renditionFilter, final Boolean includePolicyIds, final Boolean includeAcl, final ExtensionsData extension) {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final AbstractNode obj = app.get(AbstractNode.class, objectId);
			if (obj != null) {

				final ObjectData data = CMISObjectWrapper.wrap(obj, propertyFilter, includeAllowableActions);

				tx.success();

				return data;
			}

		} catch (Throwable t) {
			logger.warn("", t);
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public Properties getProperties(final String repositoryId, final String objectId, final String propertyFilter, final ExtensionsData extension) {

		final ObjectData obj = getObject(repositoryId, objectId, propertyFilter, false, IncludeRelationships.NONE, null, false, false, extension);
		if (obj != null) {

			return obj.getProperties();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public List<RenditionData> getRenditions(final String repositoryId, final String objectId, final String renditionFilter, final BigInteger maxItems, final BigInteger skipCount, final ExtensionsData extension) {

		final ObjectData obj = getObject(repositoryId, objectId, renditionFilter, false, IncludeRelationships.NONE, null, false, false, extension);
		if (obj != null) {

			return new CMISPagingListWrapper<>(obj.getRenditions(), maxItems, skipCount).getPagedList();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public ObjectData getObjectByPath(final String repositoryId, final String path, final String propertyFilter, final Boolean includeAllowableActions, final IncludeRelationships includeRelationships, final String renditionFilter, final Boolean includePolicyIds, final Boolean includeAcl, final ExtensionsData extension) {

		final PropertyKey<String> pathKey = StructrApp.key(AbstractFile.class, "path");
		final App app                     = StructrApp.getInstance();
		ObjectData result                 = null;

		try (final Tx tx = app.tx()) {

			final AbstractFile file = app.nodeQuery(AbstractFile.class).and(pathKey, path).getFirst();
			if (file != null) {

				result = CMISObjectWrapper.wrap(file, propertyFilter, includeAllowableActions);
			}

			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}

		if (result != null) {
			return result;
		}

		throw new CmisObjectNotFoundException("Object with path " + path + " does not exist");
	}

	@Override
	public ContentStream getContentStream(final String repositoryId, final String objectId, final String streamId, final BigInteger offset, final BigInteger length, final ExtensionsData extension) {

		final App app            = StructrApp.getInstance();
		ContentStreamImpl result = null;

		try (final Tx tx = app.tx()) {

			final File file = app.get(File.class, objectId);
			if (file != null) {

				return new CMISContentStream(file, offset, length);
			}

			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}

		if (result != null) {
			return result;
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public void updateProperties(final String repositoryId, final Holder<String> objectId, final Holder<String> changeToken, final Properties properties, final ExtensionsData extension) {

		final App app   = StructrApp.getInstance();
		final String id = objectId.getValue();

		try (final Tx tx = app.tx()) {

			final AbstractNode obj = app.get(AbstractNode.class, id);
			if (obj != null) {

				final PropertyMap propertyMap = PropertyMap.cmisTypeToJavaType(securityContext, obj.getClass(), properties);
				if (propertyMap != null) {

					obj.setProperties(securityContext, propertyMap);
				}

			} else {

				throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
			}

			tx.success();

		} catch (FrameworkException fex) {

			throw new CmisConstraintException(fex.getMessage(), fex);
		}
	}

	@Override
	public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(final String repositoryId, final List<BulkUpdateObjectIdAndChangeToken> objectIdsAndChangeTokens, final Properties properties, final List<String> addSecondaryTypeIds, final List<String> removeSecondaryTypeIds, final ExtensionsData extension) {

		final List<BulkUpdateObjectIdAndChangeToken> result = new LinkedList<>();
		final App app                                       = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			for (final BulkUpdateObjectIdAndChangeToken token : objectIdsAndChangeTokens) {

				final AbstractNode obj = app.get(AbstractNode.class, token.getId());
				if (obj != null) {

					final PropertyMap propertyMap = PropertyMap.cmisTypeToJavaType(securityContext, obj.getClass(), properties);
					if (propertyMap != null) {

						obj.setProperties(securityContext, propertyMap);
					}

					result.add(token);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			throw new CmisConstraintException(fex.getMessage(), fex);
		}

		return result;
	}

	@Override
	public void moveObject(String repositoryId, final Holder<String> objectId, final String targetFolderId, final String sourceFolderId, final ExtensionsData extension) {

		if (sourceFolderId != null && targetFolderId != null) {

			if (sourceFolderId.equals(targetFolderId)) {
				return;
			}

			final App app = StructrApp.getInstance(securityContext);
			try (final Tx tx = app.tx()) {

				final File file = get(app, File.class, objectId.getValue());
				final Folder parent = file.getParent();

				// check if the file to be moved is filed in the root folder (=> null parent)
				if (CMISInfo.ROOT_FOLDER_ID.equals(sourceFolderId) && parent != null) {
					throw new CmisInvalidArgumentException("Object with ID " + objectId.getValue() + " is not filed in folder with ID " + sourceFolderId);
				}

				// check if the file to be moved is filed in the given source folder
				if (parent != null && !sourceFolderId.equals(parent.getUuid())) {
					throw new CmisInvalidArgumentException("Object with ID " + objectId.getValue() + " is not filed in folder with ID " + sourceFolderId);
				}

				// check if the target folder is the root folder
				if (CMISInfo.ROOT_FOLDER_ID.equals(targetFolderId)) {

					// root folder => null parent
					file.setParent(null);

				} else {

					// get will throw an exception if the folder doesn't exist
					file.setParent(get(app, Folder.class, targetFolderId));

				}

				tx.success();

			} catch (FrameworkException fex) {

				throw new CmisConstraintException(fex.getMessage(), fex);
			}

		} else {

			throw new CmisInvalidArgumentException("Source and target folder must be set");
		}
	}

	@Override
	public void deleteObject(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {

		final App app             = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final Principal principal = securityContext.getUser(false);

			final AbstractNode obj = app.get(AbstractNode.class, objectId);
			if (obj != null) {

				if (principal.isGranted(Permission.delete, securityContext)) {

					if (obj.isNode()) {

						// getSyncNode() returns the node or null
						app.delete(obj.getSyncNode());

					} else {

						// getSyncRelationship() return the relationship or null
						app.delete(obj.getSyncRelationship());
					}

				} else {

					throw new CmisPermissionDeniedException("Cannot delete object with ID " + objectId);
				}

			} else {

				throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
			}

			tx.success();

		} catch (FrameworkException fex) {

			throw new CmisConstraintException(fex.getMessage(), fex);
		}
	}

	@Override
	public FailedToDeleteData deleteTree(final String repositoryId, final String folderId, final Boolean allVersions, final UnfileObject unfileObjects, final Boolean continueOnFailure, final ExtensionsData extension) {

		/**
		 * - allVersions can be ignored as long as we don't support versioning
		 * - unfileObjects
		 */

		if (UnfileObject.UNFILE.equals(unfileObjects)) {
			throw new CmisNotSupportedException("Unfiling not supported");
		}

		final App app                       = StructrApp.getInstance(securityContext);
		final FailedToDeleteDataImpl result = new FailedToDeleteDataImpl();

		result.setIds(new LinkedList<String>());

		try (final Tx tx = app.tx()) {

			final Folder folder = app.get(Folder.class, folderId);
			if (folder != null) {

				recursivelyCheckAndDeleteFiles(app, result, folder, continueOnFailure);

			} else {

				throw new CmisObjectNotFoundException("Folder with ID " + folderId + " does not exist");
			}

			tx.success();


		} catch (final FrameworkException fex) {

			logger.warn("", fex);
		}


		return result;
	}

	@Override
	public void setContentStream(String repositoryId, Holder<String> objectId, Boolean overwriteFlag, Holder<String> changeToken, ContentStream contentStream, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void deleteContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void appendContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken, ContentStream contentStream, boolean isLastChunk, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- private methods -----
	private void recursivelyCheckAndDeleteFiles(final App app, final FailedToDeleteDataImpl result, final AbstractFile toDelete, final Boolean continueOnFailure) throws FrameworkException {

		if (toDelete != null) {

			final Principal owner = toDelete.getOwnerNode();
			if (owner == null || owner.isGranted(Permission.delete, securityContext)) {

				app.delete(toDelete);

				if (toDelete instanceof Folder) {

					final Folder folderToDelete = (Folder)toDelete;

					for (final AbstractFile child : folderToDelete.getChildren()) {

						recursivelyCheckAndDeleteFiles(app, result, child, continueOnFailure);
					}
				}

			} else {

				if (continueOnFailure) {

					result.getIds().add(toDelete.getUuid());

				} else {

					throw new CmisPermissionDeniedException("Cannot delete object with ID " + toDelete.getUuid());
				}
			}
		}
	}

	private <T extends GraphObject> T get(final App app, final Class<T> type, final String id) throws FrameworkException {

		final T obj = app.get(type, id);
		if (obj != null) {

			return obj;
		}

		throw new CmisObjectNotFoundException("Object with ID " + id + " does not exist");
	}
}
