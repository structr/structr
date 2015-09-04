package org.structr.files.cmis;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.files.cmis.wrapper.StructrCmisWrapper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class CMISNavigationService implements NavigationService {

	private static final Logger logger = Logger.getLogger(CMISNavigationService.class.getName());

	@Override
	public ObjectInFolderList getChildren(String repositoryId, String folderId, String filter, String orderBy, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

		final App app = StructrApp.getInstance();

		if (folderId != null && folderId.equals(CMISInfo.ROOT_FOLDER_ID)) {

			try (final Tx tx = app.tx()) {

				final StructrCmisWrapper wrapper = new StructrCmisWrapper(CMISInfo.ROOT_FOLDER_ID) {

					@Override
					protected ObjectData wrap(final GraphObject item) throws FrameworkException {
						return CMISObjectWrapper.wrap(item);
					}
				};

				wrapper.wrap(app.nodeQuery(Folder.class).and(Folder.parent, null).getAsList());
				wrapper.wrap(app.nodeQuery(FileBase.class).and(FileBase.parent, null).getAsList());

				tx.success();

				return wrapper;

			} catch (Throwable t) {
				t.printStackTrace();
			}

		} else {

			try (final Tx tx = app.tx()) {

				final Folder parent        = app.get(Folder.class, folderId);
				StructrCmisWrapper wrapper = null;
				if (parent != null) {

					wrapper = new StructrCmisWrapper(parent.getProperty(Folder.path)) {

						@Override
						protected ObjectData wrap(final GraphObject item) throws FrameworkException {
							return CMISObjectWrapper.wrap(item);
						}
					};

					wrapper.wrap(parent.getProperty(Folder.folders));
					wrapper.wrap(parent.getProperty(Folder.files));
				}

				tx.success();

				return wrapper;

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		throw new CmisObjectNotFoundException("Folder with ID " + folderId + " does not exist");
	}

	@Override
	public List<ObjectInFolderContainer> getDescendants(String repositoryId, String folderId, BigInteger depth, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
		return null;
	}

	@Override
	public List<ObjectInFolderContainer> getFolderTree(String repositoryId, String folderId, BigInteger depth, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
		return null;
	}

	@Override
	public List<ObjectParentData> getObjectParents(String repositoryId, String objectId, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includeRelativePathSegment, ExtensionsData extension) {

		final App app  = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final List<ObjectParentData> data = new LinkedList<>();
			final GraphObject graphObject     = app.get(objectId);

			if (graphObject instanceof AbstractFile) {

				final Folder parent = ((AbstractFile)graphObject).getProperty(AbstractFile.parent);
				if (parent != null) {

					final CMISObjectWrapper element = CMISObjectWrapper.wrap(parent);
					final ObjectParentDataImpl impl = new ObjectParentDataImpl(element);

					impl.setRelativePathSegment(parent.getProperty(Folder.path));

					data.add(impl);
				}
			}

			tx.success();

			return data;

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}

	@Override
	public ObjectData getFolderParent(String repositoryId, String folderId, String filter, ExtensionsData extension) {

		final App app     = StructrApp.getInstance();
		ObjectData result = null;

		try (final Tx tx = app.tx()) {

			final GraphObject graphObject = app.get(folderId);

			if (graphObject != null && graphObject instanceof AbstractFile) {

				final Folder parent = ((AbstractFile)graphObject).getProperty(AbstractFile.parent);
				if (parent != null) {

					result = CMISObjectWrapper.wrap(parent);
				}
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		if (result != null) {
			return result;
		}

		return null;
	}

	@Override
	public ObjectList getCheckedOutDocs(String repositoryId, String folderId, String filter, String orderBy, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return null;
	}
}
