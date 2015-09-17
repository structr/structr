package org.structr.cmis.wrapper;

import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class CMISRelationshipWrapper extends CMISObjectWrapper<CMISRelationshipInfo> {

	public CMISRelationshipWrapper(final String propertyFilter, final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_RELATIONSHIP, propertyFilter, includeAllowableActions);
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final FilteredPropertyList properties) {
	}

	@Override
	public void initializeFrom(final CMISRelationshipInfo info) throws FrameworkException {
		super.initializeFrom(info);
	}
}
