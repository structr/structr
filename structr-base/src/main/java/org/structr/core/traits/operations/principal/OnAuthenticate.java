package org.structr.core.traits.operations.principal;

import org.structr.core.entity.Principal;
import org.structr.core.traits.operations.ComposableOperation;

public interface OnAuthenticate extends ComposableOperation {

	void onAuthenticate(final Principal principal);
}
