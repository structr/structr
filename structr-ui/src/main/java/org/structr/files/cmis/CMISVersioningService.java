package org.structr.files.cmis;

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public class CMISVersioningService extends AbstractStructrCmisService implements VersioningService {

	public CMISVersioningService(final StructrCMISService parentService, final SecurityContext securityContext) {
		super(parentService, securityContext);
	}

	@Override
	public void checkOut(String repositoryId, Holder<String> objectId, ExtensionsData extension, Holder<Boolean> contentCopied) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void cancelCheckOut(String repositoryId, String objectId, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void checkIn(String repositoryId, Holder<String> objectId, Boolean major, Properties properties, ContentStream contentStream, String checkinComment, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ObjectData getObjectOfLatestVersion(String repositoryId, String objectId, String versionSeriesId, Boolean major, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Properties getPropertiesOfLatestVersion(String repositoryId, String objectId, String versionSeriesId, Boolean major, String filter, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public List<ObjectData> getAllVersions(String repositoryId, String objectId, String versionSeriesId, String filter, Boolean includeAllowableActions, ExtensionsData extension) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
