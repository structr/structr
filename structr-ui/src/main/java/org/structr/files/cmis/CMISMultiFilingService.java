package org.structr.files.cmis;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;

/**
 *
 * @author Christian Morgner
 */
public class CMISMultiFilingService implements MultiFilingService {

	@Override
	public void addObjectToFolder(String repositoryId, String objectId, String folderId, Boolean allVersions, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeObjectFromFolder(String repositoryId, String objectId, String folderId, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
