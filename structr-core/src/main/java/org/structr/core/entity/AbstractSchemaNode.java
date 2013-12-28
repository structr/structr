package org.structr.core.entity;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSchemaToken;
import org.structr.core.Services;
import static org.structr.core.entity.AbstractNode.name;
import org.structr.core.entity.relationship.SchemaNodeResourceAccess;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.EndNodes;
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
	
	public static final Property<Long> accessFlags = new LongProperty("accessFlags").indexed();
	public static final Property<List<ResourceAccess>> grants = new EndNodes("grants", SchemaNodeResourceAccess.class);

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
			if (Services.getInstance().getConfigurationProvider().getNodeEntityClass(getProperty(name)) != null) {
			
				errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidSchemaToken(getProperty(name), "type_already_exists"));
				return false;
			}
			
			final String signature = getResourceSignature();
			final Long flags       = getProperty(accessFlags);

			if (StringUtils.isNotBlank(signature)) {

				setProperty(grants, SchemaHelper.createGrants(signature, flags));
				
				// register transaction post processing that recreates the schema information
				TransactionCommand.postProcess("reloadSchema", new ReloadSchema());
				
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.onModification(securityContext, errorBuffer)) {
			
			final String signature = getResourceSignature();
			final Long flags       = getProperty(accessFlags);

			if (StringUtils.isNotBlank(signature)) {

				SchemaHelper.updateGrants(getProperty(grants), signature, flags);
				
				// register transaction post processing that recreates the schema information
				TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

				return true;
			}
		}
		
		return false;
	}

	@Override
	public void onNodeDeletion() {

		Services.getInstance().getConfigurationProvider().unregisterEntityType(getClassName());
		
		final String signature = getResourceSignature();
		if (StringUtils.isNotBlank(signature)) {
			
			SchemaHelper.removeGrants(getResourceSignature());
		}
	}
	
	private String getResourceSignature() {
		return SchemaHelper.normalizeEntityName(getProperty(name));
	}
}
