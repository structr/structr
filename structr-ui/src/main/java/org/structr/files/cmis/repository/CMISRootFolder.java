package org.structr.files.cmis.repository;

import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.wrapper.CMISFolderWrapper;
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
		setType(Folder.class.getSimpleName());

		setPath(CMISInfo.ROOT_FOLDER_ID);
		setParentId(null);
	}


	@Override
	public AllowableActions getAllowableActions() {
		return StructrRootFolderActions.getInstance();
	}

	// ----- public methods -----
	public TypeDefinition getTypeDefinition() {
		return typeDefinition;
	}
}
