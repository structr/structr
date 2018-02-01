/*
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
var StructrModel = {
	objects: {},
	callbacks: {},
	obj: function(id) {
		return StructrModel.objects[id];
	},

	ensureObject: function (entity) {
		if (!entity || entity.id === undefined) {
			return false;
		}
		return StructrModel.obj(entity.id);
	},

	createSearchResult: function(data) {

		var obj = new StructrSearchResult(data);

		// Store a reference of this object
		StructrModel.objects[data.id] = obj;

		// Check if the object is already contained in page
		var el = $('#id_' + obj.id);
		if (el && el.length) {
			return obj;
		}

		StructrModel.append(obj);

		return obj;

	},
	/**
	 * Create a new object in the model and potentially append a UI element
	 * If refId is set, insert before this node
	 */
	create: function(data, refId, append) {

		_Logger.log(_LogType.MODEL, "StructrModel.create", data);

		if (!data || !data.id) {
			return;
		}

		var keys = Object.keys(data);

		if (keys.length === 1 && keys[0] === 'id') {
			Command.get(data.id, null, function(data) {
				return StructrModel.createFromData(data, refId, append);
			});
		} else {
			return StructrModel.createFromData(data, refId, append);
		}

	},
	createFromData: function(data, refId, append) {

		if (!data || !data.id) {
			return;
		}

		var obj;

		if (data.isPage) {

			obj = new StructrPage(data);

		} else if (data.isWidget) {

			obj = new StructrWidget(data);

		} else if (data.isContent) {

			obj = new StructrContent(data);

		} else if (data.isResourceAccess) {

			obj = new StructrResourceAccess(data);

		} else if (data.isGroup) {

			obj = new StructrGroup(data, refId);

		} else if (data.isUser) {

			obj = new StructrUser(data, refId);

		} else if (data.isImage) {

			obj = new StructrImage(data);

		} else if (data.isFolder) {

			obj = new StructrFolder(data);

		} else if (data.isFile) {

			obj = new StructrFile(data);

		} else {

			obj = new StructrElement(data);

		}

		// Store a reference of this object
		StructrModel.objects[data.id] = obj;

		// Check if the object is already contained in page
		if (obj) {

			if (obj.exists && obj.exists()) {
				var el = Structr.node(obj.id);
				if (el.parent().prop('id') === 'elementsArea') {
					el.remove();
				} else {
					_Logger.log(_LogType.MODEL, 'obj exists');
					return obj;
				}
			}

		}

		if (refId || append === undefined || append) {
			StructrModel.append(obj, refId);
		}

		return obj;

	},
	/**
	 * Append and check expand status
	 */
	append: function(obj, refId) {

		if (!obj) return;

		if (obj.content) {
			// only show the first 40 characters for content elements
			obj.content = obj.content.substring(0, 40);
		}

		obj.append(refId);

	},
	/**
	 * Check expand status
	 */
	expand: function(element, obj) {

		_Logger.log(_LogType.MODEL, 'StructrModel.expand', element, obj);

		if (element) {

			if (Structr.isExpanded(obj.id)) {
				_Entities.ensureExpanded(element);
			}

			var parent = element.parent();

			if (parent && parent.hasClass('node') && parent.children('.node') && parent.children('.node').length) {

				_Logger.log(_LogType.MODEL, 'parent of last appended object has children');

				var ent = Structr.entityFromElement(parent);
				_Entities.ensureExpanded(parent);
				_Logger.log(_LogType.MODEL, 'entity', ent);
				_Entities.appendExpandIcon(parent, ent, true, true);

			}
		}
	},
	/**
	 * Deletes an object from the UI.
	 *
	 * If object is page, remove preview and tab. If tab was the active tab,
	 * activate the tab to the left before removing it.
	 */
	del: function(id) {

		if (!id) return;

		var node = Structr.node(id);
		if (node && !node.hasClass("schema")) {
			node.remove();
		}

		// Since users/groups are not displayed as '#id_'-elements anymore, Structr.node() does not find (all of) them.
		// therefor we let the object itself handle its removal in this case.
		var obj = StructrModel.obj(id);
		if (obj) {
			obj.remove();
		}

		if (graphBrowser) {
			try {
				graphBrowser.graph.dropElement(id);
			} catch (e) { }
		}

	},
	/**
	 * Update the model with the given data.
	 *
	 * This function is usually triggered by a websocket message
	 * and will trigger a UI refresh.
	 **/
	update: function(data) {

		if (!data || !data.id) {
			return;
		}

		var obj = StructrModel.obj(data.id);

		if (obj && data.modifiedProperties && data.modifiedProperties.length) {

			$.each(data.modifiedProperties, function(i, key) {
				_Logger.log(_LogType.MODEL, 'update model', key, data.data[key]);
				obj[key] = data.data[key];
			});

			StructrModel.refresh(obj.id);
		}

		return obj;

	},
	updateKey: function(id, key, value) {
		_Logger.log(_LogType.MODEL, 'StructrModel.updateKey', id, key, value);
		var obj = StructrModel.obj(id);

		if (obj) {
			obj[key] = value;
		}

	},
	/**
	 * Refresh the object's UI representation with
	 * the current model value for the given key
	 */
	refreshKey: function(id, key, width) {

		var w = width || 200;

		var obj = StructrModel.obj(id);
		if (!obj) {
			return;
		}

		var element = Structr.node(id);

		if (!element) {
			return;
		}

		var inputElement = $('td.' + key + '_ input', element);
		_Logger.log(_LogType.MODEL, inputElement);
		var newValue = obj[key];
		_Logger.log(_LogType.MODEL, key, newValue, typeof newValue);

		var attrElement = element.children('.' + key + '_');

		if (attrElement && $(attrElement).length) {
			var tag = $(attrElement).get(0).tagName.toLowerCase();

			if (typeof newValue === 'boolean') {

				_Entities.changeBooleanAttribute(attrElement, newValue);

			} else {

				blinkGreen(attrElement);

				if (attrElement && tag === 'select') {
					attrElement.val(newValue);
				} else {
					_Logger.log(_LogType.MODEL, key, newValue);
					if (key === 'name') {
						attrElement.attr('title', newValue).html(fitStringToWidth(newValue, w));
					}
				}

				if (inputElement) {
					inputElement.val(newValue);
				}

				if (key === 'content') {

					_Logger.log(_LogType.MODEL, attrElement.text(), newValue);

					attrElement.text(newValue);

					if (Structr.isModuleActive(_Pages)) {
						_Pages.reloadIframe(obj.pageId);
					}
				}
			}
		}

		_Logger.log(_LogType.MODEL, key, Structr.getClass(element));

		if (key === 'name') {

			if (Structr.getClass(element) === 'page') {

				if (Structr.isModuleActive(_Pages)) {
					// update tab and reload iframe
					var tabNameElement = $('#show_' + id).children('.name_');

					blinkGreen(tabNameElement);

					tabNameElement.attr('title', newValue).html(fitStringToWidth(newValue, w));

					_Logger.log(_LogType.MODEL, 'Model: Reload iframe', id, newValue);
					_Pages.reloadIframe(id);
				}

			} else if (Structr.getClass(element) === 'folder') {

				if (Structr.isModuleActive(_Files)) {
					_Files.refreshTree();
				}
			}
		}
	},
	/**
	 * Refresh the object's UI representation
	 * with the current object data from the model.
	 */
	refresh: function(id) {

		var obj = StructrModel.obj(id);
		_Logger.log(_LogType.MODEL, 'Model refresh, updated object', obj);

		if (obj) {

			_DuplicateFinder.reactToUpdateNotification(obj);

			var element = Structr.node(id);

			if (graphBrowser) {
				graphBrowser.updateNode(id, obj, ['name', 'tag', 'id', 'type'], {label: 'name', nodeType: 'type'});
			}

			if (!element) {
				return;
			}

			_Logger.log(_LogType.MODEL, obj, id, element);

			// update values with given key
			$.each(Object.keys(obj), function(i, key) {
				StructrModel.refreshKey(id, key);
			});

			// update HTML 'class' and 'id' attributes
			if (isIn('_html_id', Object.keys(obj)) || isIn('_html_class', Object.keys(obj))) {

				var classIdAttrsEl = element.children('.class-id-attrs');
				if (classIdAttrsEl.length) {
					classIdAttrsEl.remove();
				}

				var classIdString = _Elements.classIdString(obj._html_id, obj._html_class);
				var idEl = element.children('.id');
				if (idEl.length) {
					element.children('.id').after(classIdString);
				}
			}

			// update icon
			var icon = undefined;
			if (element.hasClass('element')) {

				icon = _Elements.getElementIcon(obj);

			} else if (element.hasClass('content')) {

				icon = _Elements.getContentIcon(obj);

			} else if (element.hasClass('file')) {

				icon = _Icons.getFileIconClass(obj);

			} else if (element.hasClass('folder')) {

				if (Structr.isModuleActive(_Files)) {
					_Files.refreshTree();
				}

			}

			var iconEl = element.children('.typeIcon');
			if (icon && iconEl.length) {
				_Icons.updateSpritasdeClassTo(iconEl[0], _Icons.getSpriteClassOnly(icon));
			}

			// check if key icon needs to be displayed (in case of nodes not visible to public/auth users)
			var isProtected = !obj.visibleToPublicUsers || !obj.visibleToAuthenticatedUsers;
			var keyIcon = element.children('.key_icon');
			if (!keyIcon.length) {
				// Images have a special subnode containing the icons
				keyIcon = $('.icons', element).children('.key_icon');
			}
			if (isProtected) {
				keyIcon.showInlineBlock();
				keyIcon.addClass('donthide');
			} else {
				keyIcon.hide();
				keyIcon.removeClass('donthide');
			}

			var displayName = getElementDisplayName(obj);

			if (obj.hasOwnProperty('name')) {

				// Did name change from null?
				if ((obj.type === 'Template' || obj.isContent)) {
					if (obj.name) {
						element.children('.content_').replaceWith('<b title="' + displayName + '" class="tag_ name_">' + displayName + '</b>');
						element.children('.content_').off('click').on('click', function (e) {
							e.stopPropagation();
							_Entities.makeNameEditable(element, 200);
						});

						element.children('.name_').replaceWith('<b title="' + displayName + '" class="tag_ name_">' + displayName + '</b>');
						element.children('b.name_').off('click').on('click', function (e) {
							e.stopPropagation();
							_Entities.makeNameEditable(element, 200);
						});
					} else {
						element.children('.name_').html(escapeTags(obj.content));
					}
				} else {
					element.children('.name_').attr('title', displayName).html(fitStringToWidth(displayName, 200));
				}
			}
		}
	},
	/**
	 * Fetch data from server. This will trigger a refresh of the model.
	 */
	fetch: function(id) {
		Command.get(id);
	},
	/**
	 * Save model data to server. This will trigger a refresh of the model.
	 */
	save: function(id) {
		var obj = StructrModel.obj(id);
		_Logger.log(_LogType.MODEL, 'StructrModel.save', obj);

		// Filter out object type data
		var data = {};
		$.each(Object.keys(obj), function(i, key) {

			var value = obj[key];

			if (typeof value !== 'object') {
				data[key] = value;
			}

		});
		Command.setProperties(id, data);
	},

	callCallback: function(callback, entity, resultSize, errorOccurred) {
		if (callback) {
			_Logger.log(_LogType.MODEL, 'Calling callback', callback, 'on entity', entity, resultSize);
			var callbackFunction = StructrModel.callbacks[callback];
			if (callbackFunction) {
				_Logger.log(_LogType.MODEL, callback, callbackFunction.toString());
				try {
					StructrModel.callbacks[callback](entity, resultSize, errorOccurred);
				} catch (e) {
					//console.log('Exception catched: ', e, ', callback:', StructrModel.callbacks[callback], entity);
				}
			}
			StructrModel.clearCallback(callback);
		}
	},

	clearCallback: function(callback) {
		if (callback && StructrModel.callbacks[callback]) {
			delete StructrModel.callbacks[callback];
			callback = undefined;
			delete callback;
		}
	},

	copyDataToObject: function (data, target) {
		$.each(Object.keys(data), function(i, key) {
			target[key] = data[key];
		});
	}
};


/**************************************
 * Structr Folder
 **************************************/

function StructrFolder(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrFolder.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrFolder.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrFolder.prototype.remove = function() {
	if (Structr.isModuleActive(_Files)) {
		_Files.refreshTree();
		_DuplicateFinder.reactToDeleteNotification(this.id);
	}
};

StructrFolder.prototype.append = function() {

	if (Structr.isModuleActive(_Files)) {
		_Files.fileOrFolderCreationNotification(this);
		_Files.refreshTree();
	}
};

StructrFolder.prototype.exists = function() {
	var el = Structr.node(this.id);
	return el && el.length > 0;
};

/**************************************
 * Structr File
 **************************************/

function StructrFile(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrFile.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrFile.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, false, callback);
};

StructrFile.prototype.remove = function() {
	if (Structr.isModuleActive(_Files)) {
		_DuplicateFinder.reactToDeleteNotification(this.id);
	}
};

StructrFile.prototype.append = function() {
	var file = this;
	if (file.parent) {
		var parentFolder = StructrModel.obj(file.parent.id);
		if (parentFolder) {
			if (!parentFolder.files) {
				parentFolder.files = [];
			}
			parentFolder.files.push(file);
		}
	}

	if (Structr.isModuleActive(_Files)) {
		_Files.fileOrFolderCreationNotification(this);
	}
};


/**************************************
 * Structr Image
 **************************************/

function StructrImage(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrImage.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrImage.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, false, callback);
};

StructrImage.prototype.remove = function() {
	if (Structr.isModuleActive(_Files)) {
		_DuplicateFinder.reactToDeleteNotification(this.id);
	}
};

StructrImage.prototype.append = function() {
	var image = this;
	if (image.parent) {
		var parentFolder = StructrModel.obj(image.parent.id);
		if (parentFolder) {
			parentFolder.files.push(image);
		}
	}

	if (Structr.isModuleActive(_Files)) {
		_Files.fileOrFolderCreationNotification(this);
	}
};


/**************************************
 * Structr User
 **************************************/

function StructrUser(data, refId) {
	StructrModel.copyDataToObject(data, this);
}

StructrUser.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrUser.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, false, callback);
};

StructrUser.prototype.remove = function(groupId) {
	if (groupId) {
		var group = StructrModel.obj(groupId);
		if (group) {
			group.removeUser(this.id);
		}
	} else {
		if (Structr.isModuleActive(_Security)) {
			var userEl = Structr.node(this.id, '.userid_');
			if (userEl && userEl.length) {
				userEl.remove();
			}
		}
	}
};

StructrUser.prototype.append = function(groupId) {
	if (Structr.isModuleActive(_Security)) {
		if (groupId) {
			var grpContainer = $('.groupid_' + groupId, _Security.groups);
			$('.userid_' + this.id, grpContainer).remove();
			_UsersAndGroups.appendMemberToGroup(this, StructrModel.obj(groupId), grpContainer);
		} else {
			_UsersAndGroups.appendUserToUserList(this);
		}
	}
};

/**************************************
 * Structr Group
 **************************************/

function StructrGroup(data, refId) {
	StructrModel.copyDataToObject(data, this);
}

StructrGroup.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrGroup.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrGroup.prototype.append = function(refId) {
	if (Structr.isModuleActive(_Security)) {
		var container = _Security.groups;
		if (refId) {
			var grpContainer = $('.groupid_' + refId, container);
			if (grpContainer.length) {
				container = grpContainer;
				$('.groupid_' + this.id, container).remove();
			}
		}
		StructrModel.expand(_UsersAndGroups.appendGroupElement(container, this), this);
	}
};

StructrGroup.prototype.remove = function() {
	if (Structr.isModuleActive(_Security)) {
		var groupEl = Structr.node(this.id, '.groupid_');
		if (groupEl && groupEl.length) {
			groupEl.remove();
		}
	}
};

StructrGroup.prototype.removeUser = function(userId) {
	this.members = this.members.filter(function (user) {
		return user.id !== userId;
	});

	if (Structr.isModuleActive(_Security)) {
		var groupEl = Structr.node(this.id, '.groupid_');
		if (groupEl && groupEl.length) {
			$('.userid_' + userId, groupEl).remove();

			if (this.members.length === 0) {
				_Entities.removeExpandIcon(groupEl);
			}
		}
	}
};

/**************************************
 * Structr ResourceAccess
 **************************************/

function StructrResourceAccess(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrResourceAccess.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrResourceAccess.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrResourceAccess.prototype.append = function() {
	if (Structr.isModuleActive(_Security)) {
		_ResourceAccessGrants.appendResourceAccessElement(this);
	}
};

StructrResourceAccess.prototype.remove = function() {
};

/**************************************
 * Structr Page
 **************************************/

function StructrPage(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrPage.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrPage.prototype.append = function() {
	if (Structr.isModuleActive(_Pages)) {
		StructrModel.expand(_Pages.appendPageElement(this), this);
	}
};

StructrPage.prototype.remove = function() {
	if (Structr.isModuleActive(_Pages)) {
		_Pages.removePage(this);
	}
};

/**************************************
 * Structr Widget
 **************************************/

function StructrWidget(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrWidget.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrWidget.prototype.append = function() {
	if (Structr.isModuleActive(_Pages)) {
		StructrModel.expand(_Widgets.appendWidgetElement(this), this);
	}
};

StructrWidget.prototype.remove = function() {
};

/**************************************
 * Structr Element
 **************************************/

function StructrElement(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrElement.prototype.appendChild = function(el) {
	var self = this;
	self.children.push(el);
};

StructrElement.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrElement.prototype.removeAttribute = function(key) {
	var self = this;
	delete self[key];
};

StructrElement.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrElement.prototype.remove = function() {

	if (this.parent) {
		// remove this element from its parent object in the model
		var modelEntity = StructrModel.obj(this.parent.id);
		if (modelEntity) {
			modelEntity.children = modelEntity.children.filter(function (child) {
				return child.id === this.id;
			});
			if (modelEntity.childrenIds) {
				modelEntity.childrenIds = modelEntity.childrenIds.filter(function (childId) {
					return childId === this.id;
				});
			}
		}
	}

	if (Structr.isModuleActive(_Pages)) {
		var element = Structr.node(this.id);
		if (this.parent) {
			var parent = Structr.node(this.parent.id);
		}

		if (element) {
			// If element is removed from page tree, reload elements area
			if (element.closest('#pages').length) {
				_Elements.reloadUnattachedNodes();
			}
			element.remove();
		}

		_Logger.log(_LogType.MODEL, this, element, parent, Structr.containsNodes(parent));

		if (element && parent && !Structr.containsNodes(parent)) {
			_Entities.removeExpandIcon(parent);
		}

		_Pages.reloadPreviews();
	}
};

StructrElement.prototype.append = function(refId) {
	if (Structr.isModuleActive(_Pages)) {
		var refNode = refId ? Structr.node(refId) : undefined;
		StructrModel.expand(_Pages.appendElementElement(this, refNode), this);
	}
};

StructrElement.prototype.exists = function() {

	var obj = this;

	var hasChildren = obj.childrenIds && obj.childrenIds.length;
	var isComponent = obj.syncedNodes && obj.syncedNodes.length;

	var isMasterComponent = (isComponent && hasChildren);

	return !isMasterComponent && Structr.node(obj.id);
};

StructrElement.prototype.isActiveNode = function() {
	return this.hideOnIndex || this.hideOnDetail || this.hideConditions || this.showConditions || this.dataKey || this.restQuery || this.cypherQuery || this.xpathQuery || this.functionQuery
		//String attributes
		|| this["data-structr-action"]
		|| this["data-structr-attr"]
		|| this["data-structr-attributes"]
		|| this["data-structr-custom-options-query"]
		|| this["data-structr-edit-class"]
		|| this["data-structr-hide"]
		|| this["data-structr-id"]
		|| this["data-structr-name"]
		|| this["data-structr-options-key"]
		|| this["data-structr-raw-value"]
		|| this["data-structr-return"]
		|| this["data-structr-type"]
		//Boolean attributes
		|| this["data-structr-append-id"]===true
		|| this["data-structr-confirm"]===true
		|| this["data-structr-reload"]===true;
};

/**************************************
 * Structr Content
 **************************************/

function StructrContent(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrContent.prototype.appendChild = function(el) {
	var self = this;
	self.children.push(el);
};

StructrContent.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrContent.prototype.removeAttribute = function(key) {
	var self = this;
	delete self[key];
};

StructrContent.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrContent.prototype.remove = function() {

	if (Structr.isModuleActive(_Pages)) {
		var element = Structr.node(this.id);
		if (this.parent) {
			var parent = Structr.node(this.parent.id);
		}

		if (element) {
			// If element is removed from page tree, reload elements area
			if (element.closest('#pages').length) {
				_Elements.reloadUnattachedNodes();
			}
			element.remove();
		}

		if (parent && !Structr.containsNodes(parent)) {
			_Entities.removeExpandIcon(parent);
		}
		_Pages.reloadPreviews();
	}
};

StructrContent.prototype.append = function(refId) {

	if (Structr.isModuleActive(_Pages)) {
		var id = this.id;
		var parentId;

		var parent;
		if (this.parent) {
			parentId = this.parent.id;
			parent = Structr.node(parentId);
		}

		var refNode = refId ? Structr.node(refId) : undefined;
		var div = _Elements.appendContentElement(this, refNode);
		if (!div) {
			return;
		}

		_Logger.log(_LogType.MODEL, 'appendContentElement div', div);

		StructrModel.expand(div, this);

		if (parent) {

			$('.button', div).on('mousedown', function(e) {
				e.stopPropagation();
			});

			$('.delete_icon', div).replaceWith('<i title="Remove content element from parent ' + parentId + '" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_content_icon) + '" />');
			$('.delete_icon', div).on('click', function(e) {
				e.stopPropagation();
				Command.removeChild(id);
			});
		}

		_Entities.setMouseOver(div);
	}
};

StructrContent.prototype.exists = function() {
	return Structr.node(this.id);
};

StructrContent.prototype.isActiveNode = function() {
	return this.hideOnIndex || this.hideOnDetail || this.hideConditions || this.showConditions || this.dataKey;
};

/**************************************
 * Search result
 **************************************/

function StructrSearchResult(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrSearchResult.prototype.append = function() {

	if (Structr.isModuleActive(_Graph)) {
		var obj = this;

		if (obj.hasOwnProperty('relType') && obj.hasOwnProperty('sourceId') && obj.hasOwnProperty('targetId')) {
			_Graph.drawRel(obj);
		} else {
			_Graph.drawNode(this);
		}
	}
};

function removeFromArray(array, obj) {
	var newArray = [];
	if (array && array.length) {
		$.each(array, function(i, el) {
			if (el.id !== obj.id && el !== obj.id) {
				newArray.push(el);
			}
		});
	}
	return newArray;
}
