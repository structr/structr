package org.structr.core.traits;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;

public class GraphObjectTraitHandler implements GraphObjectTrait {

	private final GraphObjectTrait defaultHandler;

	public GraphObjectTraitHandler(final Traits traits) {
		this.defaultHandler = new GraphObjectTraitImplementation(traits);
	}

	@Override
	public boolean isGranted(final GraphObject graphObject, final Permission permission, final SecurityContext securityContext, final boolean isCreation) {
		return defaultHandler.isGranted(graphObject, permission, securityContext, isCreation);
	}

	@Override
	public boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {
		return defaultHandler.isValid(obj, errorBuffer);
	}

	@Override
	public void onCreation(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		defaultHandler.onCreation(obj, securityContext, errorBuffer);
	}

	@Override
	public void onModification(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		defaultHandler.onModification(obj, securityContext, errorBuffer, modificationQueue);
	}

	@Override
	public void onDeletion(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		defaultHandler.onDeletion(obj, securityContext, errorBuffer, properties);
	}

	@Override
	public void afterCreation(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException {
		defaultHandler.afterCreation(obj, securityContext);
	}

	@Override
	public void afterModification(final GraphObject obj, final SecurityContext securityContext) throws FrameworkException {
		defaultHandler.afterModification(obj, securityContext);
	}

	@Override
	public void afterDeletion(final GraphObject obj, final SecurityContext securityContext, final PropertyMap properties) {
		defaultHandler.afterDeletion(obj, securityContext, properties);
	}

	@Override
	public void ownerModified(final GraphObject obj, final SecurityContext securityContext) {
		defaultHandler.ownerModified(obj, securityContext);
	}

	@Override
	public void securityModified(final GraphObject obj, final SecurityContext securityContext) {
		defaultHandler.securityModified(obj, securityContext);
	}

	@Override
	public void locationModified(final GraphObject obj, final SecurityContext securityContext) {
		defaultHandler.locationModified(obj, securityContext);
	}

	@Override
	public void propagatedModification(final GraphObject obj, final SecurityContext securityContext) {
		defaultHandler.propagatedModification(obj, securityContext);
	}

	@Override
	public void indexPassiveProperties(final GraphObject obj) {
		defaultHandler.indexPassiveProperties(obj);
	}
}
