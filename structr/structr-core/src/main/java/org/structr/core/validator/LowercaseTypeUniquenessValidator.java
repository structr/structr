package org.structr.core.validator;

import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.LowercaseUniqueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.search.SearchUserCommand;


/**
 * Validator that checks the uniqueness after a given lowercase transformation of the property value.
 *
 * @author Bastian Knerr
 */
public class LowercaseTypeUniquenessValidator extends PropertyValidator<String> {


	private final PropertyKey propertyKey;
	private final NodeIndex nodeIndex;

	public LowercaseTypeUniquenessValidator(final NodeIndex indexKey, final PropertyKey key) {

		nodeIndex = indexKey;
		propertyKey = key;
	}


	@Override
	public boolean isValid(final GraphObject object, final String key,
	                       final String value, final ErrorBuffer errorBuffer) {

		final AbstractNode result = lookup(nodeIndex, propertyKey, value);
		if (result == null) {
			return true;
		}

		final String id = result.getUuid();
		errorBuffer.add(object.getType(), new LowercaseUniqueToken(id, key, value));

		return false;
	}


	private static AbstractNode lookup(final NodeIndex index, final PropertyKey key, final String value) {
		try {
			return
				(AbstractNode) Services.command(SecurityContext.getSuperUserInstance(), SearchUserCommand.class).execute(value, key, index);

		} catch (final FrameworkException fex) {
			fex.printStackTrace();
		}
		return null;
	}
}
