package org.structr.core.entity;

import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import static org.structr.core.entity.AbstractNode.name;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.schema.ReloadSchema;
import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractSchemaNode extends AbstractNode {
	
	public static final Property<Long> accessFlags                      = new LongProperty("accessFlags").indexed();
	

	static {
		
		AbstractNode.name.addValidator(new TypeUniquenessValidator<String>(AbstractSchemaNode.class));
	}
	
	public String getClassName() {
		return getProperty(name);
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.onCreation(securityContext, errorBuffer)) {

			// check if type already exists and raise an error if yes.
//			if (Services.getInstance().getConfigurationProvider().getNodeEntityClass(getClassName()) != null) {
//			
//				errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidSchemaToken(getProperty(name), "type_already_exists"));
//				return false;
//			}
			

			// register transaction post processing that recreates the schema information
			TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

			return true;
		}
		
		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.onModification(securityContext, errorBuffer)) {
			
			// register transaction post processing that recreates the schema information
			TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

			return true;
		}
		
		return false;
	}

	@Override
	public void onNodeDeletion() {

		Services.getInstance().getConfigurationProvider().unregisterEntityType(getClassName());
		
		final String signature = getResourceSignature();
		if (StringUtils.isNotBlank(signature)) {
			
			SchemaHelper.removeDynamicGrants(getResourceSignature());
		}
	}
	
	public String getResourceSignature() {
		return SchemaHelper.normalizeEntityName(getProperty(name));
	}
}
