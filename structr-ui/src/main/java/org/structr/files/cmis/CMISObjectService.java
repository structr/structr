package org.structr.files.cmis;

import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;

/**
 *
 * @author Christian Morgner
 */
public class CMISObjectService extends AbstractStructrCmisService implements ObjectService {

	public CMISObjectService(final SecurityContext securityContext) {
		super(securityContext);
	}

	private static final Logger logger = Logger.getLogger(CMISObjectService.class.getName());

	@Override
	public String createDocument(String repositoryId, Properties properties, String folderId, ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String createDocumentFromSource(String repositoryId, String sourceId, Properties properties, String folderId, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String createFolder(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

		System.out.println("getAllowableActions");

		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ObjectData getObject(String repositoryId, final String objectId, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final ObjectData data = CMISObjectWrapper.wrap(app.get(objectId));

			tx.success();

			return data;

		} catch (Throwable t) {
			t.printStackTrace();
		}

		throw new CmisObjectNotFoundException("Object with ID " + objectId + " does not exist");
	}



	@Override
	public Properties getProperties(String repositoryId, String objectId, String filter, ExtensionsData extension) {
		System.out.println("getAllowableActions");

		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public List<RenditionData> getRenditions(String repositoryId, String objectId, String renditionFilter, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

		System.out.println("getAllowableActions");

		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ObjectData getObjectByPath(String repositoryId, String path, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {

		final App app     = StructrApp.getInstance();
		ObjectData result = null;

		try (final Tx tx = app.tx()) {

			final AbstractFile file = app.nodeQuery(AbstractFile.class).and(AbstractFile.path, path).getFirst();
			if (file != null) {

				result = CMISObjectWrapper.wrap(file);
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

		final App app                              = StructrApp.getInstance();
		ContentStreamImpl result                   = null;

		System.out.println("getContentStream(" + objectId + ", " + streamId + ", " + offset + ", " + length + ")");

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
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public FailedToDeleteData deleteTree(String repositoryId, String folderId, Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

}
