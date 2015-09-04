package org.structr.files.cmis;

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.spi.PolicyService;

/**
 *
 * @author Christian Morgner
 */
public class CMISPolicyService implements PolicyService {

	@Override
	public void applyPolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removePolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public List<ObjectData> getAppliedPolicies(String repositoryId, String objectId, String filter, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
