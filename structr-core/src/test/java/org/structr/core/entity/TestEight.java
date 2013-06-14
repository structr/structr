package org.structr.core.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */
public class TestEight extends AbstractNode {

	private long onCreationTimestamp        = 0L;
	private long onModificationTimestamp    = 0L;
	private long onDeletionTimestamp        = 0L;
	private long afterCreationTimestamp     = 0L;
	private long afterModificationTimestamp = 0L;
	private long afterDeletionTimestamp     = 0L;

	@Override
	public boolean onCreation(SecurityContext securityContext1, ErrorBuffer errorBuffer) throws FrameworkException {
		
		this.onCreationTimestamp = System.currentTimeMillis();
		return true;
	}

	@Override
	public boolean onModification(SecurityContext securityContext1, ErrorBuffer errorBuffer) throws FrameworkException {
		
		this.onModificationTimestamp = System.currentTimeMillis();
		return true;
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext1, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		
		this.onDeletionTimestamp = System.currentTimeMillis();
		return true;
	}

	@Override
	public void afterCreation(SecurityContext securityContext1) {
		
		this.afterCreationTimestamp = System.currentTimeMillis();
	}

	@Override
	public void afterModification(SecurityContext securityContext1) {
		
		this.afterModificationTimestamp = System.currentTimeMillis();
	}
	
	@Override
	public void afterDeletion(SecurityContext securityContext1) {
		
		this.afterDeletionTimestamp = System.currentTimeMillis();
	}
	
	public void resetTimestamps() {
		
		onCreationTimestamp        = 0L;
		onModificationTimestamp    = 0L;
		onDeletionTimestamp        = 0L;
		afterCreationTimestamp     = 0L;
		afterModificationTimestamp = 0L;
		afterDeletionTimestamp     = 0L;
	}

	public long getOnCreationTimestamp() {
		return onCreationTimestamp;
	}

	public long getOnModificationTimestamp() {
		return onModificationTimestamp;
	}

	public long getOnDeletionTimestamp() {
		return onDeletionTimestamp;
	}

	public long getAfterCreationTimestamp() {
		return afterCreationTimestamp;
	}

	public long getAfterModificationTimestamp() {
		return afterModificationTimestamp;
	}

	public long getAfterDeletionTimestamp() {
		return afterDeletionTimestamp;
	}
}
