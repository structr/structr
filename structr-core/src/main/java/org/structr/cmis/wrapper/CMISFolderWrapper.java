package org.structr.cmis.wrapper;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.structr.cmis.info.CMISFolderInfo;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.CMISInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class CMISFolderWrapper extends CMISObjectWrapper<CMISFolderInfo> {

	private String changeToken = null;
	private String parentId    = null;
	private String path        = null;

	public CMISFolderWrapper(final String propertyFilter, final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_FOLDER, propertyFilter, includeAllowableActions);
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setChangeToken(String changeToken) {
		this.changeToken = changeToken;
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final FilteredPropertyList properties) {

		properties.add(factory.createPropertyStringData(PropertyIds.PARENT_ID, parentId == null && !isRootFolder() ? CMISInfo.ROOT_FOLDER_ID : parentId));
		properties.add(factory.createPropertyStringData(PropertyIds.PATH, path));
		properties.add(factory.createPropertyStringData(PropertyIds.CHANGE_TOKEN, changeToken));

		// added for specification compliance
		properties.add(factory.createPropertyIdData(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, (String)null));
		properties.add(factory.createPropertyIdData(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, (String)null));
	}

	@Override
	public void initializeFrom(final CMISFolderInfo info) throws FrameworkException {

		super.initializeFrom(info);

		this.changeToken = info.getChangeToken();
		this.parentId    = info.getParentId();
		this.path        = info.getPath();
	}

	// ----- protected methods -----
	protected boolean isRootFolder() {
		return false;
	}
}
