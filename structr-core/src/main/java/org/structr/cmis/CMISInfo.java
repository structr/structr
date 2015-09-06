package org.structr.cmis;

import org.structr.cmis.info.CMISFolderInfo;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.cmis.info.CMISPolicyInfo;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.cmis.info.CMISSecondaryInfo;

/**
 * Optional interface for CMIS support in Structr core.
 *
 * @author Christian Morgner
 */
public interface CMISInfo {

	public static final String ROOT_FOLDER_ID = "/";

	public BaseTypeId getBaseTypeId();

	public CMISFolderInfo getFolderInfo();
	public CMISDocumentInfo getDocumentInfo();
	public CMISItemInfo geItemInfo();
	public CMISRelationshipInfo getRelationshipInfo();
	public CMISPolicyInfo getPolicyInfo();
	public CMISSecondaryInfo getSecondaryInfo();

}
