package org.structr.files.cmis;

import java.util.Collections;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.spi.PolicyService;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public class CMISPolicyService extends AbstractStructrCmisService implements PolicyService {

	public CMISPolicyService(final SecurityContext securityContext) {
		super(securityContext);
	}

	@Override
	public void applyPolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
	}

	@Override
	public void removePolicy(String repositoryId, String policyId, String objectId, ExtensionsData extension) {
	}

	@Override
	public List<ObjectData> getAppliedPolicies(String repositoryId, String objectId, String filter, ExtensionsData extension) {
		return Collections.emptyList();
	}

}
