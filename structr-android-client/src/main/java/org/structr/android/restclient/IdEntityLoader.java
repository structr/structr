/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.android.restclient;

import android.app.Activity;

/**
 * An asynchronous connector that fetches an entity with a given ID from a structr
 * REST server. The execute method takes exactly two parameters, namely the type of
 * the entity to fetch (a Class instance) and the String-based ID of the
 * desired entitiy.
 * 
 * <p>The following example shows how you can use this class in your activity.</p>
 * <pre>
 * new IdEntityLoader(new EntityHandler() {
 *
 *	public void handleProgress(Progress... progress) {
 *		// handle progress / exception
 *	}
 *
 *	public void handleResults(StructrObject result) {
 *		// handle result
 *	}
 * 
 * }).execute(Example.class, id");
 * </pre>
 * 
 * @author Christian Morgner
 */
public class IdEntityLoader extends StructrConnector<StructrObject> {

	private EntityHandler updater = null;

	public IdEntityLoader(Activity activity) {
		this(new ThrowableToaster(activity));
	}
	
	public IdEntityLoader(EntityHandler updater) {
		this.updater = updater;
	}

	@Override
	protected StructrObject doInBackground(Object... parameters) {

		Class type = null;
		String id = null;
		
		for(Object obj : parameters) {
			
			if(obj instanceof Class) {
				type = (Class)obj;
			} else if(obj instanceof String) {
				id = (String)obj;
			}
		}
		
		try {
			return StructrObject.dbGet(type, id);

		} catch(Throwable t) {
			publishProgress(new Progress(t));
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(Progress... progress) {
		if(updater != null) {
			updater.handleProgress(progress);
		}
	}

	@Override
	protected void onPostExecute(StructrObject entity) {
		if(updater != null) {
			updater.handleResult(entity);
		}
	}
}
