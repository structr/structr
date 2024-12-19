package org.structr.core.entity;

import org.structr.core.traits.NodeTrait;

public interface Localization extends NodeTrait {

	String getLocalizedName();
	String getLocale();
	String getDomain();

	boolean isImported();
}
