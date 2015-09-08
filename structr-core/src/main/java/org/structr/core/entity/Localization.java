package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class Localization extends AbstractNode {

	public static final Property<String>  localizedName = new StringProperty("localizedName").cmis().indexed();
	public static final Property<String>  domain        = new StringProperty("domain").cmis().indexed();
	public static final Property<String>  locale        = new StringProperty("locale").cmis().indexed();
	public static final Property<Boolean> imported      = new BooleanProperty("imported").cmis().indexed();

	public static final View defaultView = new View(Localization.class, PropertyView.Public,
		domain, name, locale, localizedName, imported
	);

	public static final View uiView = new View(Localization.class, PropertyView.Ui,
		domain, name, locale, localizedName, imported
	);

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		setProperty(visibleToPublicUsers, true);
		setProperty(visibleToAuthenticatedUsers, true);

		return true;
	}
}
