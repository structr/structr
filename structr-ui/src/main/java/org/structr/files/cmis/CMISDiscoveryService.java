package org.structr.files.cmis;

import java.math.BigInteger;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.structr.common.SecurityContext;
import org.structr.files.cmis.wrapper.CMISObjectListWrapper;

/**
 *
 * @author Christian Morgner
 */
public class CMISDiscoveryService extends AbstractStructrCmisService implements DiscoveryService {

	public CMISDiscoveryService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	private static final Logger logger = Logger.getLogger(CMISDiscoveryService.class.getName());

	@Override
	public ObjectList query(String repositoryId, String statement, Boolean searchAllVersions, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		return new CMISObjectListWrapper(maxItems, skipCount);
	}

	@Override
	public ObjectList getContentChanges(String repositoryId, Holder<String> changeLogToken, Boolean includeProperties, String filter, Boolean includePolicyIds, Boolean includeAcl, BigInteger maxItems, ExtensionsData extension) {
		return new CMISObjectListWrapper(maxItems, BigInteger.ZERO);
	}
}
