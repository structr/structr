package org.structr.core.traits.operations.principal;

import org.structr.core.entity.Principal;
import org.structr.core.traits.operations.LifecycleMethod;

public interface OnAuthenticate extends LifecycleMethod {

	void onAuthenticate(final Principal principal);
}
