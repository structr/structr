package org.structr.cmis.wrapper;

import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.info.CMISSecondaryInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class CMISSecondaryWrapper extends CMISObjectWrapper<CMISSecondaryInfo> {

	public CMISSecondaryWrapper(final String propertyFilter, final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_SECONDARY, propertyFilter, includeAllowableActions);
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final FilteredPropertyList properties) {
	}

	@Override
	public void initializeFrom(final CMISSecondaryInfo info) throws FrameworkException {
		super.initializeFrom(info);
	}
}
