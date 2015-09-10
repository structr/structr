package org.structr.files.cmis;

import java.io.OutputStream;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
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
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.files.cmis.wrapper.CMISPagingListWrapper;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class CMISObjectService extends AbstractStructrCmisService implements ObjectService {

	public CMISObjectService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	private static final Logger logger = Logger.getLogger(CMISObjectService.class.getName());

	@Override
	public String createDocument(final String repositoryId, final Properties properties, final String folderId, final ContentStream contentStream, final VersioningState versioningState, final List<String> policies, final Acl addAces, final Acl removeAces, final ExtensionsData extension) {

		final App app = StructrApp.getInstance(securityContext);
		File newFile  = null;
		String uuid   = null;

		try (final Tx tx = app.tx()) {

			final String objectTypeId = getStringValue(properties, PropertyIds.OBJECT_TYPE_ID);
			final String fileName     = getStringValue(properties, PropertyIds.NAME);
			final Class type          = typeFromObjectTypeId(objectTypeId, BaseTypeId.CMIS_DOCUMENT, File.class);

			// check if type exists
			if (type != null) {

				// check that base type is cmis:folder
				final BaseTypeId baseTypeId = getBaseTypeId(type);
				if (baseTypeId != null && BaseTypeId.CMIS_DOCUMENT.equals(baseTypeId)) {

					// create file
					newFile = FileHelper.createFile(securityContext, new byte[0], contentStream.getMimeType(), type, fileName);

					// find and set parent if it exists
					if (!CMISInfo.ROOT_FOLDER_ID.equals(folderId)) {

						final Folder parent = app.get(Folder.class, folderId);
						if (parent != null) {

							newFile.setProperty(Folder.parent, parent);

						} else {

							throw new CmisObjectNotFoundException("Folder with ID " + folderId + " does not exist");
						}
					}

					uuid = newFile.getUuid();

					// copy file and update metadata
					try (final OutputStream outputStream = newFile.getOutputStream(false)) {
						IOUtils.copy(contentStream.getStream(), outputStream);
					}

					FileHelper.updateMetadata(newFile);

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
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
					final NodeInterface newFolder = app.create(type, PropertyMap.cmisTypeToJavaType(securityContext, type, properties));

					// find and set parent if it exists
					if (!CMISInfo.ROOT_FOLDER_ID.equals(folderId)) {

						final Folder parent = app.get(Folder.class, folderId);
						if (parent != null) {

							newFolder.setProperty(Folder.parent, parent);

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
	public ObjectData getObject(String repositoryId, final String objectId, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final GraphObject obj = app.get(objectId);
			if (obj != null) {

				final ObjectData data = CMISObjectWrapper.wrap(obj, includeAllowableActions);

				tx.success();

				return data;
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public Properties getProperties(String repositoryId, String objectId, String filter, ExtensionsData extension) {

		final ObjectData obj = getObject(repositoryId, objectId, filter, false, IncludeRelationships.NONE, null, false, false, extension);
		if (obj != null) {

			return obj.getProperties();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public List<RenditionData> getRenditions(String repositoryId, String objectId, String renditionFilter, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

		final ObjectData obj = getObject(repositoryId, objectId, renditionFilter, false, IncludeRelationships.NONE, null, false, false, extension);
		if (obj != null) {

			return new CMISPagingListWrapper<>(obj.getRenditions(), maxItems, skipCount).getPagedList();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public ObjectData getObjectByPath(String repositoryId, String path, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {

		final App app     = StructrApp.getInstance();
		ObjectData result = null;

		try (final Tx tx = app.tx()) {

			final AbstractFile file = app.nodeQuery(AbstractFile.class).and(AbstractFile.path, path).getFirst();
			if (file != null) {

				result = CMISObjectWrapper.wrap(file, includeAllowableActions);
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		if (result != null) {
			return result;
		}

		throw new CmisObjectNotFoundException("Object with path " + path + " does not exist");
	}

	@Override
	public ContentStream getContentStream(String repositoryId, String objectId, String streamId, BigInteger offset, BigInteger length, ExtensionsData extension) {

		final App app            = StructrApp.getInstance();
		ContentStreamImpl result = null;

		try (final Tx tx = app.tx()) {

			final FileBase file = app.get(FileBase.class, objectId);
			if (file != null) {

				result = new ContentStreamImpl();

				result.setFileName(file.getName());
				result.setLength(BigInteger.valueOf(file.getProperty(FileBase.size)));
				result.setMimeType(file.getProperty(FileBase.contentType));
				result.setStream(file.getInputStream());
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		if (result != null) {
			return result;
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}

	@Override
	public void updateProperties(String repositoryId, Holder<String> objectId, Holder<String> changeToken, Properties properties, ExtensionsData extension) {

		final App app   = StructrApp.getInstance();
		final String id = objectId.getValue();

		try (final Tx tx = app.tx()) {

			final GraphObject obj = app.get(id);
			if (obj != null) {

				final PropertyMap propertyMap = PropertyMap.cmisTypeToJavaType(securityContext, obj.getClass(), properties);
				if (propertyMap != null) {

					for (final Entry<PropertyKey, Object> entry : propertyMap.entrySet()) {

						obj.setProperty(entry.getKey(), entry.getValue());
					}
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
	public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(String repositoryId, List<BulkUpdateObjectIdAndChangeToken> objectIdsAndChangeTokens, Properties properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void moveObject(String repositoryId, Holder<String> objectId, String targetFolderId, String sourceFolderId, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void deleteObject(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {

		final App app             = StructrApp.getInstance(securityContext);
		final Principal principal = securityContext.getUser(false);

		try (final Tx tx = app.tx()) {

			final GraphObject obj = app.get(objectId);
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

			fex.printStackTrace();
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

				for (final AbstractFile child : toDelete.getProperty(AbstractFile.children)) {

					recursivelyCheckAndDeleteFiles(app, result, child, continueOnFailure);
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
}
