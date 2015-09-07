package org.structr.files.cmis;

import java.math.BigInteger;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.spi.RelationshipService;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public class CMISRelationshipService extends AbstractStructrCmisService implements RelationshipService {

	public CMISRelationshipService(final SecurityContext securityContext) {
		super(securityContext);
	}

	@Override
	public ObjectList getObjectRelationships(String repositoryId, String objectId, Boolean includeSubRelationshipTypes, RelationshipDirection relationshipDirection, String typeId, String filter, Boolean includeAllowableActions, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
