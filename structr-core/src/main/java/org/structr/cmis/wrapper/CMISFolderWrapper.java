package org.structr.cmis.wrapper;

import java.util.List;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
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

	private String parentId = null;
	private String path     = null;

	public CMISFolderWrapper(final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_FOLDER, includeAllowableActions);
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final List<PropertyData<?>> properties) {

		properties.add(factory.createPropertyStringData(PropertyIds.PATH, path));
		properties.add(factory.createPropertyStringData(PropertyIds.PARENT_ID, parentId == null ? CMISInfo.ROOT_FOLDER_ID : parentId));
	}

	@Override
	public void initializeFrom(final CMISFolderInfo info) throws FrameworkException {

		super.initializeFrom(info);

		this.parentId = info.getParentId();
		this.path     = info.getPath();
	}
}
