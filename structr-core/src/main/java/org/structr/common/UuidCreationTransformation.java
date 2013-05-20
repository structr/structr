/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Transformation;
import org.structr.core.entity.AbstractNode;

/**
 * The internal post-processing transformation that assigns UUIDs to structr
 * entities. This transformation is registered for all structr entities.
 *
 * @author Christian Morgner
 */
public class UuidCreationTransformation extends Transformation<GraphObject> {

	//private static final ThreadLocalPattern pattern = new ThreadLocalPattern();
	
	@Override
	public void apply(SecurityContext securityContext, GraphObject obj) throws FrameworkException {

		// create uuid if not set
		String uuid = obj.getProperty(AbstractNode.uuid);
		if(uuid == null || uuid.isEmpty()) {
			
			synchronized(obj) {
				
				String nextUuid = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
				//nextUuid = pattern.get().matcher(nextUuid).replaceAll("");
				
				obj.unlockReadOnlyPropertiesOnce();
				obj.setProperty(AbstractNode.uuid, nextUuid);
			}
		}
	}

	@Override
	public int getOrder() {
		
		// first
		return 0;
	}
	
//	private static class ThreadLocalPattern extends ThreadLocal<Pattern> {
//		
//		@Override
//		protected Pattern initialValue() {
//			return Pattern.compile("[\\-]+");
//		}
//	}
}
