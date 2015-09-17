package org.structr.cmis.wrapper;

import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class CMISItemWrapper extends CMISObjectWrapper<CMISItemInfo> {

	public CMISItemWrapper(final String propertyFilter, final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_ITEM, propertyFilter, includeAllowableActions);
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final FilteredPropertyList properties) {
	}

	@Override
	public void initializeFrom(final CMISItemInfo info) throws FrameworkException {
		super.initializeFrom(info);
	}
}
