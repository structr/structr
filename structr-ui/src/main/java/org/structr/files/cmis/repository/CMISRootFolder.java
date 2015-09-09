package org.structr.files.cmis.repository;

import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.wrapper.CMISFolderWrapper;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyMap;
import org.structr.files.cmis.config.StructrRootFolderActions;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class CMISRootFolder extends CMISFolderWrapper {

	private MutableTypeDefinition typeDefinition = null;

	public CMISRootFolder(final Boolean includeAllowableActions) {

		super(includeAllowableActions);

		typeDefinition = TypeDefinitionFactory.newInstance().createBaseFolderTypeDefinition(CmisVersion.CMIS_1_1);

		setId(CMISInfo.ROOT_FOLDER_ID);
		setName(CMISInfo.ROOT_FOLDER_ID);
		setDescription("Root Folder");
		setType(Folder.class.getSimpleName());

		try {
			final String superuserName = StructrApp.getConfigurationValue(Services.SUPERUSER_USERNAME);

			setLastModifiedBy(superuserName);
			setLastModificationDate(CMISInfo.ROOT_FOLDER_DATE);

			setCreatedBy(superuserName);
			setCreationDate(CMISInfo.ROOT_FOLDER_DATE);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		setPath(CMISInfo.ROOT_FOLDER_ID);
		setParentId(null);

		// dynamic properties
		dynamicPropertyMap = new PropertyMap();
		dynamicPropertyMap.put(Folder.includeInFrontendExport, false);
		dynamicPropertyMap.put(Folder.position, null);
	}


	@Override
	public AllowableActions getAllowableActions() {
		return StructrRootFolderActions.getInstance();
	}

	// ----- public methods -----
	public TypeDefinition getTypeDefinition() {
		return typeDefinition;
	}

	// ----- protected methods -----
	@Override
	protected boolean isRootFolder() {
		return true;
	}

	@Override
	protected String translateIdToUsername(final String id) throws FrameworkException {

		// prevent translation of ID values for this root folder entity
		return id;
	}
}
