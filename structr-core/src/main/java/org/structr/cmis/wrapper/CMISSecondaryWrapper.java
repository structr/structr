package org.structr.cmis.wrapper;

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.info.CMISSecondaryInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class CMISSecondaryWrapper extends CMISObjectWrapper<CMISSecondaryInfo> {

	public CMISSecondaryWrapper(final Boolean includeAllowableActions) {
		super(BaseTypeId.CMIS_SECONDARY, includeAllowableActions);
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final List<PropertyData<?>> properties) {
	}

	@Override
	public void initializeFrom(final CMISSecondaryInfo info) throws FrameworkException {
		super.initializeFrom(info);
	}
}
