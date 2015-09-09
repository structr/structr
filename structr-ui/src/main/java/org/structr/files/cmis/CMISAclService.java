package org.structr.files.cmis;

import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public class CMISAclService extends AbstractStructrCmisService implements AclService {

	public CMISAclService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	@Override
	public Acl getAcl(String repositoryId, String objectId, Boolean onlyBasicPermissions, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Acl applyAcl(String repositoryId, String objectId, Acl addAces, Acl removeAces, AclPropagation aclPropagation, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
