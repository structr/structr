package org.structr.cmis.wrapper;

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class CMISRelationshipWrapper extends CMISObjectWrapper<CMISRelationshipInfo> {

	public CMISRelationshipWrapper() {
		super(BaseTypeId.CMIS_RELATIONSHIP);
	}

	@Override
	public void createProperties(final BindingsObjectFactory factory, final List<PropertyData<?>> properties) {
	}

	@Override
	public void initializeFrom(final CMISRelationshipInfo info) throws FrameworkException {
		super.initializeFrom(info);
	}
}
