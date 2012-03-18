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
import java.util.List;

/**
 * An asynchronous connector that fetches a list of entities from a REST server. The
 * execute method takes at least two parameters, the entity type and the path to
 * load the entities from.
 * 
 * <p>The following example shows how you can use this class in your activity.</p>
 * <pre>
 * new PathCollectionLoader(new CollectionHandler() {
 *
 *	public void handleProgress(Progress... progress) {
 *		// handle progress / exception
 *	}
 *
 *	public void handleResults(List&lt;? extends StructrObject&gt; results) {
 *		// handle results
 *	}
 * 
 * }).execute(Example.class, "/examples?attr1=foo&attr2=bar&sort=date&order=asc");
 * </pre>
 * 
 * @author Christian Morgner
 */
public class PathCollectionLoader extends StructrConnector<List<? extends StructrObject>> {

	private CollectionHandler updater = null;

	public PathCollectionLoader(CollectionHandler updater) {
		this.updater = updater;
	}

	@Override
	protected List<? extends StructrObject> doInBackground(Object... parameters) {

		StringBuilder path = new StringBuilder();
		Class type = null;
		
		try {
			for(Object obj : parameters) {
				if(obj instanceof Class) {
					type = (Class)obj;
				} else {
					path.append(obj.toString());
				}
			}

			return StructrObject.dbList(type, path.toString());

		} catch(Throwable t) {
			publishProgress(new Progress(t));
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(Progress... progress) {
		updater.handleProgress(progress);
	}

	@Override
	protected void onPostExecute(List<? extends StructrObject> list) {
		updater.handleResults(list);
	}

}
