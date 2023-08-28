/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
let StructrModel = {
	objects: {},
	callbacks: {},
	obj: (id) => StructrModel.objects[id],

	ensureObject: function(entity) {
		if (!entity || entity.id === undefined) {
			return false;
		}
		return StructrModel.obj(entity.id);
	},

	createSearchResult: function(data) {

		let obj = new StructrSearchResult(data);

		// Store a reference of this object
		StructrModel.objects[data.id] = obj;

		// Check if the object is already contained in page
		let el = $('#id_' + obj.id);
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

		if (!data || !data.id) {
			return;
		}

		let keys = Object.keys(data);

		if (keys.length === 1 && keys[0] === 'id') {

			let existingObj = StructrModel.obj(data.id);

			if (existingObj) {
				return existingObj;
			} else {

				Command.get(data.id, null, function(data) {
					return StructrModel.createFromData(data, refId, append);
				});
			}

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

		} else if (data.isCorsSetting) {

			obj = new StructrCorsSetting(data);

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
	 * Create a new model object from data - if it already exists update all keys in the dataset.
	 * This prevents possible deletion of previously loaded object if it was fetched another time with fewer properties or a different view.
	 */
	createOrUpdateFromData: (data, refId, append) => {

		let modelObj = StructrModel.obj(data.id);
		if (!modelObj) {
			StructrModel.createFromData(data, refId, append);
		} else {
			StructrModel.copyDataToObject(data, modelObj);
		}
	},
	/**
	 * Append and check expand status
	 */
	append: function(obj, refId) {

		if (!obj) {
			return;
		}

		obj.append(refId);
	},
	/**
	 * Check expand status
	 */
	expand: function(element, obj) {

		if (element) {

			if (Structr.isExpanded(obj.id)) {
				_Entities.ensureExpanded(element);
			}

			let parent = element.parent();

			if (parent && parent.hasClass('node') && parent.children('.node') && parent.children('.node').length) {

				let entity = Structr.entityFromElement(parent);

				_Entities.appendExpandIcon($(parent.children('.node-container')), entity, true, true);
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

		if (!id) {
			return;
		}

		let node = Structr.node(id);
		if (node && node.remove && !node.hasClass("schema")) {
			_Helpers.fastRemoveElement(node[0]);
		}

		// Let element handle its own removal from the UI
		// Especially (but not only) important for users/groups. Those are not displayed as '#id_'-elements anymore, Structr.node() does not find (all of) them.
		// therefore we let the object itself handle its removal in this case.
		let obj = StructrModel.obj(id);
		if (obj && obj.remove) {
			obj.remove();
		}

		if (_Graph.graphBrowser) {
			try {
				_Graph.graphBrowser.graph.dropElement(id);
			} catch (e) {}
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

		let obj = StructrModel.obj(data.id);

		if (obj && data.modifiedProperties && data.modifiedProperties.length) {

			let callback = (newProperties) => {

				Object.assign(obj, newProperties);
				StructrModel.refresh(obj.id);

				StructrModel.callCallback(data.callback, obj);
			};

			let refreshKeys = Object.keys(obj);
			data.modifiedProperties.forEach((p) => {
				if (!refreshKeys.includes(p)) {
					refreshKeys.push(p);
				}
			});

			if (data.relData && data.relData.sourceId) {
				Command.getRelationship(data.id, data.relData.sourceId, refreshKeys.join(','), callback);
			} else {
				Command.get(data.id, refreshKeys.join(','), callback);
			}

		} else {

			// call callback anyway even if there is no obj
			StructrModel.callCallback(data.callback, obj);
		}

		return obj;
	},
	updateKey: function(id, key, value) {
		var obj = StructrModel.obj(id);

		if (obj) {
			obj[key] = value;
		}
	},
	/**
	 * Refresh the object's UI representation with
	 * the current model value for the given key
	 */
	refreshKey: function(id, key) {

		let obj = StructrModel.obj(id);
		if (!obj) {
			return;
		}

		let element = Structr.node(id);

		if (!element) {
			return;
		}

		let inputElement = $('td.' + key + '_ input', element);
		let newValue     = obj[key];
		let attrElement  = element.children(':not(.node)').find(`.${key}_`);

		if (attrElement && $(attrElement).length) {

			let tag = $(attrElement).get(0).tagName.toLowerCase();

			if (typeof newValue === 'boolean') {

				_Entities.changeBooleanAttribute(attrElement, newValue);

			} else {

				_Helpers.blinkGreen(attrElement);

				if (attrElement && tag === 'select') {

					attrElement.val(newValue);

				} else {

					if (key === 'name') {
						attrElement.attr('title', newValue).html(newValue);
					}
				}

				if (inputElement) {
					inputElement.val(newValue);
				}

				if (key === 'content') {

					if (newValue) {
						attrElement.text(newValue);
					} else {
						attrElement.html('&nbsp;');
					}
				}

				if (key === 'position') {

					attrElement.text(newValue);
				}
			}
		}

		if (key === 'name') {

			if (Structr.getClass(element) === 'folder') {

				if (Structr.isModuleActive(_Files)) {
					_Files.refreshNode(id, newValue);
				}
			}
		}
	},
	/**
	 * Refresh the object's UI representation
	 * with the current object data from the model.
	 */
	refresh: function(id) {

		let obj = StructrModel.obj(id);

		if (obj) {

			// let current module handle update (if it wants to)
			Structr.getActiveModule().handleNodeRefresh?.(obj);

			let element = Structr.node(id);

			if (_Graph.graphBrowser) {
				_Graph.graphBrowser.updateNode(id, obj, ['name', 'tag', 'id', 'type'], {label: 'name', nodeType: 'type'});
			}

			if (!element) {
				return;
			}

			// update values with given key
			for (let key in obj) {
				StructrModel.refreshKey(id, key);
			}

			if (Structr.isModuleActive(_Pages) && obj.pageId) {
				_Pages.previews.modelForPageUpdated(obj.pageId);
			}

			// update HTML 'class' and 'id' attributes
			if (_Helpers.isIn('_html_id', Object.keys(obj)) || _Helpers.isIn('_html_class', Object.keys(obj))) {

				let classIdAttrsEl = element.children('.node-container').find('.class-id-attrs');
				if (classIdAttrsEl.length) {
					classIdAttrsEl.replaceWith(_Elements.classIdString(obj));
				}
			}

			if (obj.hidden === true) {
				element.addClass('is-hidden');
			} else {
				element.removeClass('is-hidden');
			}

			// update icon
			let icon   = undefined;
			let iconEl = element.children('.typeIcon');
			if (element.hasClass('element')) {

				icon   = _Icons.getSvgIconForElementNode(obj, (!obj.children || obj.children.length === 0) ? [_Icons.typeIconClassNoChildren] : []);
				iconEl = element.children('.node-container').children('.typeIcon');

			} else if (element.hasClass('content')) {

				let iconClasses = ['typeIcon'];
				if (!obj.children || obj.children.length === 0) {
					iconClasses.push(_Icons.typeIconClassNoChildren)
				}

				icon   = _Icons.getSvgIconForContentNode(obj, iconClasses);
				iconEl = element.children('.node-container').children('.typeIcon');

			} else if (element.hasClass('file')) {

				icon   = _Icons.getSvgIcon(_Icons.getFileIconSVG(obj));
				iconEl = element.closest('tr').children('td.file-icon').find('svg');

				if (Structr.isModuleActive(_Files)) {
					let row = element.closest('tr');
					if (row.length) {
						$('td.size', row).text(_Helpers.formatBytes(obj.size, 0));
					}
				}

			} else if (element.hasClass('folder')) {

				if (Structr.isModuleActive(_Files)) {
					_Files.refreshNode(id, obj.name);
				}
			}

			if (icon && iconEl.length) {
				_Icons.replaceSvgElementWithRawSvg(iconEl[0], icon);
			}

			_Entities.updateNewAccessControlIconInElement(obj, element);

			let displayName = _Helpers.getElementDisplayName(obj);

			if (obj.hasOwnProperty('name')) {

				// Did name change from null?
				if ((obj.type === 'Template' || obj.isContent)) {
					if (obj.name) {
						element.children('.node-container').find('.content_').replaceWith(`<b title="${_Helpers.escapeForHtmlAttributes(displayName)}" class="tag_ name_">${displayName}</b>`);

						element.children('.node-container').find('.name_').replaceWith(`<b title="${_Helpers.escapeForHtmlAttributes(displayName)}" class="tag_ name_">${displayName}</b>`);

					} else {
						element.children('.node-container').find('.name_').html(_Helpers.escapeTags(obj.content));
					}
				} else {
					element.children('.node-container').find('.name_').attr('title', displayName).html(displayName);
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
		let obj = StructrModel.obj(id);

		// Filter out object type data
		let data = {};
		for (let key in obj) {

			let value = obj[key];

			if (typeof value !== 'object') {
				data[key] = value;
			}
		}

		Command.setProperties(id, data);
	},

	callCallback: function(callback, entity, resultSize, errorOccurred) {
		if (callback) {
			let callbackFunction = StructrModel.callbacks[callback];
			if (callbackFunction) {
				try {
					StructrModel.callbacks[callback](entity, resultSize, errorOccurred);
				} catch (e) {
					console.trace('Exception caught: ', e, ', callback:', StructrModel.callbacks[callback], entity);
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

	copyDataToObject: function(data, target) {
		for (let key in data) {
			target[key] = data[key];
		}
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
};

StructrFolder.prototype.append = function() {

	if (Structr.isModuleActive(_Files)) {
		_Files.refreshTree();
	}
};

StructrFolder.prototype.exists = function() {
	let el = Structr.node(this.id);
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
};

StructrFile.prototype.append = function() {
	let file = this;
	if (file.parent) {
		let parentFolder = StructrModel.obj(file.parent.id);
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
};

StructrImage.prototype.append = function() {
	let image = this;
	if (image.parent) {
		let parentFolder = StructrModel.obj(image.parent.id);
		if (parentFolder) {
			if (!parentFolder.files) {
				parentFolder.files = [];
			}
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

StructrUser.prototype.updatedModel = function() {
	if (Structr.isModuleActive(_Security)) {
		_UsersAndGroups.updateUserElementAfterModelChange(this);
	}
}

StructrUser.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, false, callback);
};

StructrUser.prototype.remove = function(groupId) {
	if (groupId) {
		let group = StructrModel.obj(groupId);
		if (group) {
			group.removeUser(this.id);
		}
	} else {
		if (Structr.isModuleActive(_Security)) {
			let userEl = Structr.node(this.id, '.userid_');
			if (userEl && userEl.length) {
				userEl.remove();
			}
		}
	}
};

StructrUser.prototype.append = function(groupId) {
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

StructrGroup.prototype.updatedModel = function() {
	if (Structr.isModuleActive(_Security)) {
		_UsersAndGroups.updateGroupElementAfterModelChange(this);
	}
}

StructrGroup.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrGroup.prototype.append = function(groupId) {
};

StructrGroup.prototype.remove = function() {
	if (Structr.isModuleActive(_Security)) {
		let groupEl = Structr.node(this.id, '.groupid_');
		if (groupEl && groupEl.length) {
			groupEl.remove();
		}
	}
};

StructrGroup.prototype.removeUser = function(userId) {
	this.members = this.members.filter(function(user) {
		return user.id !== userId;
	});

	if (Structr.isModuleActive(_Security)) {
		let groupEl = $('.groupid_' + this.id);
		if (groupEl && groupEl.length) {

			groupEl.children('.userid_' + userId).remove();

			if (this.members.length === 0) {
				for (let grpEl of groupEl) {
					_Entities.removeExpandIcon($(grpEl));
				}
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
 * Structr CorsSetting
 **************************************/

function StructrCorsSetting(data) {
	StructrModel.copyDataToObject(data, this);
}

StructrCorsSetting.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrCorsSetting.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrCorsSetting.prototype.append = function() {
	if (Structr.isModuleActive(_Security)) {
		_CorsSettings.appendCorsSettingToElement(this);
	}
};

StructrCorsSetting.prototype.remove = function() {
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

	if (_Entities?.selectedObject?.id === this.id) {
		_Pages.selectedObjectWasDeleted();
	}

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
		StructrModel.expand(_Widgets.appendWidgetElement(this, false), this);
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
	let self = this;
	self.children.push(el);
};

StructrElement.prototype.setProperty = function(key, value, recursive, callback) {
	Command.setProperty(this.id, key, value, recursive, callback);
};

StructrElement.prototype.removeAttribute = function(key) {
	let self = this;
	delete self[key];
};

StructrElement.prototype.save = function() {
	StructrModel.save(this.id);
};

StructrElement.prototype.remove = function() {

	if (this.parent) {

		// remove this element from its parent object in the model
		let modelEntity = StructrModel.obj(this.parent.id);
		if (modelEntity) {

			if (modelEntity.children) {
				modelEntity.children = modelEntity.children.filter(child => child.id === this.id);
			}

			if (modelEntity.childContainers) {
				modelEntity.childContainers = modelEntity.childContainers.filter(child => child.id === this.id);
			}

			if (modelEntity.childrenIds) {
				modelEntity.childrenIds = modelEntity.childrenIds.filter(childId => childId === this.id);
			}
		}
	}

	if (Structr.isModuleActive(_Pages)) {

		let element = Structr.node(this.id);
		if (this.parent) {
			var parent = Structr.node(this.parent.id);
		}

		if (element) {

			// reload the unused elements (if open)
			_Pages.unattachedNodes.reload();

			_Helpers.fastRemoveElement(element[0]);
		}

		if (_Entities?.selectedObject?.id === this.id && !(_Dragndrop.temporarilyRemovedElementUUID === this.id)) {
			_Pages.selectedObjectWasDeleted();
		}

		if (element && parent && !Structr.containsNodes(parent)) {
			_Entities.removeExpandIcon(parent);
		}
	}
};

StructrElement.prototype.append = function(refId) {

	if (Structr.isModuleActive(_Pages)) {

		let refNode = refId ? Structr.node(refId) : undefined;
		let div     = _Pages.appendElementElement(this, refNode);
		if (!div) {
			return;
		}

		StructrModel.expand(div, this);
	}
};

StructrElement.prototype.exists = function() {

	let obj               = this;
	let hasChildren       = obj.childrenIds && obj.childrenIds.length;
	let isComponent       = obj.syncedNodesIds && obj.syncedNodesIds.length;
	let isMasterComponent = (isComponent && hasChildren);

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
		|| this["eventMapping"]
		//Boolean attributes
		|| this["data-structr-append-id"] === true
		|| this["data-structr-confirm"] === true
		|| this["data-structr-reload"] === true
		//Collection attributes
		|| (this["triggeredActions"] ?? []).length
		;
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

		let element = Structr.node(this.id);
		if (this.parent) {
			var parent = Structr.node(this.parent.id);
		}

		if (element) {

			// reload the unused elements (if open)
			_Pages.unattachedNodes.reload();

			_Helpers.fastRemoveElement(element[0]);
		}

		if (_Entities?.selectedObject?.id === this.id && !(_Dragndrop.temporarilyRemovedElementUUID === this.id)) {
			_Pages.selectedObjectWasDeleted();
		}

		if (parent && !Structr.containsNodes(parent)) {
			_Entities.removeExpandIcon(parent);
		}
	}
};

StructrContent.prototype.append = function(refId) {

	if (Structr.isModuleActive(_Pages)) {

		let refNode = refId ? Structr.node(refId) : undefined;
		let div     = _Elements.appendContentElement(this, refNode);
		if (!div) {
			return;
		}

		StructrModel.expand(div, this);
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
