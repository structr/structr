package org.structr.files.cmis.wrapper;

import java.math.BigInteger;
import java.util.List;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;

/**
 *
 * @author Christian Morgner
 */
public class CMISTypeDefinitionListWrapper extends CMISPagingListWrapper<TypeDefinition> implements TypeDefinitionList {

	public CMISTypeDefinitionListWrapper() {
		super();
	}

	public CMISTypeDefinitionListWrapper(final BigInteger maxItems, final BigInteger skipCount) {
		super(maxItems, skipCount);
	}

	@Override
	public List<TypeDefinition> getList() {
		return getPagedList();
	}
}
