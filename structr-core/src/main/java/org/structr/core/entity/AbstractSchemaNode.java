package org.structr.core.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.validator.GlobalPropertyUniquenessValidator;
import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractSchemaNode extends AbstractNode {
	
	public static final Property<String> className   = new StringProperty("className", new GlobalPropertyUniquenessValidator()).indexed();
	public static final Property<Long>   accessFlags = new LongProperty("accessFlags").indexed();

	public String getClassName() {
		return getProperty(className);
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (SchemaHelper.reloadSchema(errorBuffer)) {

			final String signature = getResourceSignature();
			final Long flags       = getProperty(accessFlags);

			SchemaHelper.createGrant(signature, flags);
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (SchemaHelper.reloadSchema(errorBuffer)) {

			final String signature = getResourceSignature();
			final Long flags       = getProperty(accessFlags);

			SchemaHelper.createGrant(signature, flags);
			return true;
		}
		
		return false;
	}

	@Override
	public void onNodeDeletion() {
		Services.getInstance().getConfigurationProvider().unregisterEntityType(getClassName());
		SchemaHelper.removeGrant(getResourceSignature());
	}
	
	private String getResourceSignature() {
		return SchemaHelper.normalizeEntityName(getProperty(className));	
	}
}
