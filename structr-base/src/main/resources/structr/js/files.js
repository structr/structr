/*
 * Copyright (C) 2010-2025 Structr GmbH
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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Files);
});

let _Files = {
	_moduleName: 'files',
	defaultFolderAttributes: 'id,name,type,owner,isFolder,path,visibleToPublicUsers,visibleToAuthenticatedUsers,ownerId,isMounted,parentId,foldersCount,filesCount,createdDate,lastModifiedDate',
	defaultFileAttributes: 'id,name,type,createdDate,lastModifiedDate,contentType,isFile,isImage,isThumbnail,isTemplate,tnSmall,tnMid,path,size,owner,visibleToPublicUsers,visibleToAuthenticatedUsers',
	currentWorkingDir: undefined,
	fileUploadList: undefined,
	chunkSize: 1024 * 64,
	fileSizeLimit: 1024 * 1024 * 1024,
	activeFileId: undefined,
	currentEditor: undefined,
	fileContents: {},
	fileHasUnsavedChanges: {},
	folderPageSize: 10000,
	folderPage: 1,
	filesViewModeKey: 'structrFilesViewMode_' + location.port,
	filesLastOpenFolderKey: 'structrFilesLastOpenFolder_' + location.port,
	filesResizerLeftKey: 'structrFilesResizerLeftKey_' + location.port,

	getViewMode: () => LSWrapper.getItem(_Files.filesViewModeKey, 'list'),
	setViewMode: viewMode => LSWrapper.setItem(_Files.filesViewModeKey, viewMode),
	isViewModeActive: viewMode => (viewMode === _Files.getViewMode()),
	init: () => {

		_Files.dateFormat = new Intl.DateTimeFormat(navigator.language, {
			//weekday: 'long',
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: 'numeric',
			minute: 'numeric',
			second: 'numeric'
		});

		_Files.setViewMode(_Files.getViewMode());

		Structr.adaptUiToAvailableFeatures();
	},
	resize: () => {
		_Files.moveResizer();
	},
	prevAnimFrameReqId_moveResizer: undefined,
	moveResizer: (left) => {

		// throttle
		_Helpers.requestAnimationFrameWrapper(_Files.prevAnimFrameReqId_moveResizer, () => {
			left = left || LSWrapper.getItem(_Files.filesResizerLeftKey) || 300;
			$('.column-resizer', _Files.getFilesMainElement()).css({ left: left });

			_Files.getFilesTree().css({ width: left - 14 + 'px' });
			$('#folder-contents-container').css({ width: `calc(100% - ${left + 14}px)` });
		});
	},
	getFilesTree: () => $('#file-tree'),
	getFilesMainElement: () => $('#files-main'),
	getFolderContentsElement: () => document.querySelector('#folder-contents'),
	onload: () => {

		Structr.setMainContainerHTML(_Files.templates.main());

		_Files.init();

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('files'));

		_Files.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', _Files.getFilesMainElement()), _Files.filesResizerLeftKey, 204, _Files.moveResizer);

		let initFunctionBar = async () => {

			let fileTypes   = await _Schema.getDerivedTypeNames('File', ['CsvFile']);
			let folderTypes = await _Schema.getDerivedTypeNames('Folder', ['Trash']);

			Structr.setFunctionBarHTML(_Files.templates.functions({ fileTypes: fileTypes, folderTypes: folderTypes }));

			// UISettings.showSettingsForCurrentModule();
			_Files.updateFunctionBarStatus();

			let fileTypeSelect   = Structr.functionBar.querySelector('select#file-type');
			let addFileButton    = Structr.functionBar.querySelector('#add-file-button');
			let folderTypeSelect = Structr.functionBar.querySelector('select#folder-type');
			let addFolderButton  = Structr.functionBar.querySelector('#add-folder-button');

			addFileButton.addEventListener('click', () => {
				Command.create({
					type: fileTypeSelect.value,
					size: 0,
					parentId: _Files.currentWorkingDir ? _Files.currentWorkingDir.id : null
				});
			});

			addFolderButton.addEventListener('click', () => {
				Command.create({
					type: folderTypeSelect.value,
					parentId: _Files.currentWorkingDir ? _Files.currentWorkingDir.id : null
				});
			});

			Structr.functionBar.querySelector('.mount_folder').addEventListener('click', _Files.openMountDialog);

			_Files.search.initSearch();
		};
		initFunctionBar(); // run async (do not await) so it can execute while jstree is initialized

		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		_Files.getFilesTree().on('ready.jstree', function () {

			_TreeHelper.makeAllTreeElementsDroppable(_Files.getFilesTree(), _Dragndrop.files.enableTreeElementDroppable);

			_Files.loadAndSetWorkingDir(function () {

				let lastOpenFolder = LSWrapper.getItem(_Files.filesLastOpenFolderKey);

				if (lastOpenFolder === 'favorites') {

					$('#favorites_anchor').click();

				} else if (_Files.currentWorkingDir) {

					_Files.deepOpen(_Files.currentWorkingDir);

				} else {

					let selectedNode = _Files.getFilesTree().jstree('get_selected');
					if (selectedNode.length === 0) {
						$('#root_anchor').click();
					}
				}
			});
		});

		_Files.getFilesTree().on('select_node.jstree', function (evt, data) {

			if (data.node.id === 'favorites') {

				_Files.displayFolderContents('favorites');

			} else {

				_Files.setWorkingDirectory(data.node.id);
				_Files.displayFolderContents(data.node.id, data.node.parent, data.node.original.path, data.node.parents);
			}
		});

		_Files.getFilesTree().on('after_open.jstree', (evt, data) => {
			_TreeHelper.makeAllTreeElementsDroppable(_Files.getFilesTree(), _Dragndrop.files.enableTreeElementDroppable);
		});

		_Files.getFilesTree().on('refresh_node.jstree', (evt, data) => {
			_TreeHelper.makeAllTreeElementsDroppable(_Files.getFilesTree(), _Dragndrop.files.enableTreeElementDroppable);
		});

		_TreeHelper.initTree(_Files.getFilesTree(), _Files.treeInitFunction, 'structr-ui-filesystem');

		_Files.activateUpload();

		Structr.mainMenu.unblock(100);

		Structr.resize();
		Structr.adaptUiToAvailableFeatures();
	},
	handleNodeRefresh: (node) => {

		if (node.isFolder && node.name) {

			let folderContents = _Files.getFolderContentsElement();

			// update breadcrumb
			if (folderContents.dataset['currentFolder'] === node.id) {
				folderContents.querySelector('#current-folder-name')?.replaceChildren(node.name);
			}

			// update tree element
			let treeNode = _Files.getFilesTree().jstree().get_node(node.id);

			if (treeNode) {
				_Files.getFilesTree().jstree().get_node(node.id).text = node.name;
				_Files.getFilesTree().jstree().refresh_node(node.id);
			}

		} else {

			let listModeActive  = _Files.isViewModeActive('list');
			let tilesModeActive = _Files.isViewModeActive('tiles');
			let imageModeActive = _Files.isViewModeActive('img');
			let container       = document.querySelector('#' + (listModeActive ? 'row' : 'tile') + node.id);

			if (container) {

				let size         = node.isFolder ? (node.foldersCount + node.filesCount) : node.size;
				let modifiedDate = _Files.getFormattedDate(node.lastModifiedDate);
				let name         = node.name ?? '[unnamed]';
				let iconSize     = (tilesModeActive || imageModeActive) ? 40 : 16;
				let fileIcon     = (node.isFolder ? _Icons.getFolderIconSVG(node) : _Icons.getFileIconSVG(node));
				let fileIconHTML = _Icons.getSvgIcon(fileIcon, iconSize, iconSize);
				let ownerString  = (node.owner ? (node.owner.name ? node.owner.name : '[unnamed]') : '');

				container.querySelector('[data-key=name]')?.replaceChildren(name);
				container.querySelector('[data-key=name]')?.setAttribute('title', name);
				container.querySelector('[data-key=lastModifiedDate]')?.replaceChildren(modifiedDate);
				container.querySelector('[data-key=size]')?.replaceChildren(size);
				container.querySelector('[data-key=contentType]')?.replaceChildren(node.contentType);
				container.querySelector('[data-key=owner]')?.replaceChildren(ownerString);

				let svgIcon = container.querySelector('.file-icon a svg');
				if (svgIcon) {
					_Icons.replaceSvgElementWithRawSvg(svgIcon, fileIconHTML);
				}
			}
		}
	},
	getContextMenuElements: (div, entity) => {

		const isFile         = entity.isFile;
		let selectedElements = document.querySelectorAll('.node.selected');

		// there is a difference when right-clicking versus clicking the kebab icon
		let fileNode = div;
		if (fileNode.classList.contains('icons-container')) {
			fileNode = div.closest('.node');
		} else if (!fileNode.classList.contains('node')) {
			fileNode = div.querySelector('.node');
		}

		if (fileNode && !fileNode.classList.contains('selected')) {

			for (let selNode of selectedElements) {
				selNode.classList.remove('selected');
			}
			fileNode.classList.add('selected');

			selectedElements = document.querySelectorAll('.node.selected');
		}

		let fileCount     = document.querySelectorAll('.node.file.selected').length;
		let isMultiSelect = selectedElements.length > 1;
		let elements      = [];
		let contentType   = entity.contentType || '';

		if (isFile) {

			if (entity.isImage && contentType !== 'text/svg' && !contentType.startsWith('image/svg')) {

				if (entity.isTemplate) {

					elements.push({
						icon: _Icons.getMenuSvgIcon(_Icons.iconPencilEdit),
						name: 'Edit source',
						clickHandler: () => {
							_Files.editFile(entity);
						}
					});

				} else {

					elements.push({
						icon: _Icons.getMenuSvgIcon(_Icons.iconPencilEdit),
						name: 'Edit Image',
						clickHandler: () => {
							_Files.editImage(entity);
						}
					});
				}

			} else {

				elements.push({
					icon: _Icons.getMenuSvgIcon(_Icons.iconPencilEdit),
					name: 'Edit File' + ((fileCount > 1) ? 's' : ''),
					clickHandler: () => {
						_Files.editFile(entity);
					}
				});
			}

			if (_Files.helpers.isDisplayingSearchResults()) {

				elements.push({
					icon: _Icons.getMenuSvgIcon(_Icons.iconEyeOpen),
					name: 'Show search result context',
					clickHandler: () => {

						let searchTerm = _Files.search.getCurrentSearchTerm();
						let fileIds = [...selectedElements].map(el => StructrModel.obj(Structr.getId(el))).filter(obj => obj.isFile).map(obj => obj.id);

						for (let fileId of fileIds) {
							_Files.search.getAndAppendSearchContext(fileId, searchTerm);
						}
					}
				});
			}


			_Elements.contextMenu.appendContextMenuSeparator(elements);
		}

		elements.push({
			name: 'General',
			clickHandler: () => {
				_Entities.showProperties(entity, 'general');
			}
		});

		elements.push({
			name: 'Advanced',
			clickHandler: () => {
				_Entities.showProperties(entity, 'ui');
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		if (isFile) {

			let clickedFileIsFavorite = _Favorites.isFavoriteFile(entity.id);

			if (_Files.helpers.isDisplayingFavorites() || clickedFileIsFavorite) {

				elements.push({
					icon: _Icons.getMenuSvgIcon(_Icons.iconRemoveFromFavorites),
					name: 'Remove from Favorites',
					clickHandler: () => {

						let prefix = _Files.isViewModeActive('list') ? '#row' : '#tile';
						let ids    = [...selectedElements].map(el => Structr.getId(el));

						_Favorites.removeFromFavorites(ids);

						if (_Files.helpers.isDisplayingFavorites()) {

							for (let id of ids) {
								_Helpers.fastRemoveElement(Structr.node(id, prefix)[0]);
							}
						}
					}
				});

			} else if (entity.isFile) {

				elements.push({
					icon: _Icons.getMenuSvgIcon(_Icons.iconAddToFavorites),
					name: 'Add to Favorites',
					clickHandler: () => {

						let fileIds = [...selectedElements].map(el => StructrModel.obj(Structr.getId(el))).filter(obj => obj.isFile).map(obj => obj.id);

						_Favorites.addToFavorites(fileIds);
					}
				});
			}

			if (fileCount === 1) {

				elements.push({
					name: 'Copy Download URL',
					clickHandler: async () => {

						// fake the a element so we do not need to look up the server
						let a = document.createElement('a');
						let possiblyUpdatedEntity = StructrModel.obj(entity.id);
						a.href = possiblyUpdatedEntity.path;
						await navigator.clipboard.writeText(a.href);
					}
				});

				elements.push({
					name: 'Download File',
					icon: _Icons.getMenuSvgIcon('download-icon'),
					clickHandler: () => {

						let a = document.createElement('a');
						let possiblyUpdatedEntity = StructrModel.obj(entity.id);
						a.href = `${possiblyUpdatedEntity.path}?filename=${possiblyUpdatedEntity.name}`;
						a.click();
					}
				});
			}

			if (fileCount === 1 && _Files.isArchive(entity)) {
				elements.push({
					name: 'Unpack archive',
					clickHandler: () => {
						_Files.unpackArchive(entity);
					}
				});
			}

			Structr.performActionAfterEnvResourceLoaded(() => {
				if (fileCount === 1 && contentType === 'text/csv') {
					elements.push({
						icon: _Icons.getMenuSvgIcon(_Icons.iconFileTypeCSV),
						name: 'Import CSV',
						clickHandler: () => {
							Importer.importCSVDialog(entity, false);
						}
					});
				}
			});

			Structr.performActionAfterEnvResourceLoaded(() => {
				if (fileCount === 1 && (contentType === 'text/xml' || contentType === 'application/xml')) {
					elements.push({
						icon: _Icons.getMenuSvgIcon(_Icons.iconFileTypeXML),
						name: 'Import XML',
						clickHandler: () => {
							Importer.importXMLDialog(entity, false);
						}
					});
				}
			});
		}

		if (!isMultiSelect) {

			_Elements.contextMenu.appendContextMenuSeparator(elements);

			_Elements.contextMenu.appendSecurityContextMenuItems(elements, entity, entity.isFolder);
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		if (fileNode) {

			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
				classes: ['menu-bolder', 'danger'],
				name: 'Delete ' + (isMultiSelect ? 'selected' : entity.type),
				clickHandler: () => {

					if (isMultiSelect) {

						let filesOrFolders = [...selectedElements].map(el => Structr.getId(el)).map(id => StructrModel.obj(id));
						let recursive      = filesOrFolders.some(file => file.isFolder);

						_Entities.deleteNodes(filesOrFolders, recursive, () => {
							// refresh handled via model
						});

					} else {

						_Entities.deleteNode(entity, entity.isFolder, () => {
							// refresh handled via model
						});
					}
				}
			});
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		return elements;
	},
	deepOpen: (d, dirs) => {

		_TreeHelper.deepOpen(_Files.getFilesTree(), d, dirs, 'parent', (_Files.currentWorkingDir ? _Files.currentWorkingDir.id : 'root'));
	},
	refreshTree: () => {

		_TreeHelper.refreshTree(_Files.getFilesTree(), () => {
		});
	},
	treeInitFunction: (obj, callback) => {

		switch (obj.id) {

			case '#':

				let defaultFilesystemEntries = [
					{
						id: 'favorites',
						text: 'Favorite Files',
						children: false,
						icon: _Icons.nonExistentEmptyIcon,
						data: { svgIcon: _Icons.getSvgIcon(_Icons.iconAddToFavorites, 18, 24) },
					},
					{
						id: 'root',
						text: '/',
						children: true,
						icon: _Icons.nonExistentEmptyIcon,
						data: { svgIcon: _Icons.getSvgIcon(_Icons.iconStructrSSmall, 18, 24) },
						path: '/',
						state: {
							opened: true
						}
					}
				];

				callback(defaultFilesystemEntries);
				break;

			case 'root':
				_Files.load(null, callback);
				break;

			default:
				_Files.load(obj.id, callback);
				break;
		}
	},
	unload: () => {
	},
	activateUpload: () => {

		if (window.File && window.FileReader && window.FileList && window.Blob) {

			let droppableArea = _Files.getFolderContentsElement();

			droppableArea.addEventListener('dragover', (event) => {
				event.preventDefault();
				event.dataTransfer.dropEffect = 'copy';
			});

			droppableArea.addEventListener('drop', (event) => {
				event.preventDefault();

				if (!event.dataTransfer) {
					return;
				}

				if (_Files.helpers.isDisplayingFavorites() === true) {
					new WarningMessage().text("Can't upload to virtual folder Favorites - please first upload file to destination folder and then drag to favorites.").show();
					return;
				}

				_Files.fileUploadList = event.dataTransfer.files;
				let filesToUpload = [];
				let tooLargeFiles = [];

				for (let file of _Files.fileUploadList) {

					if (file.size <= _Files.fileSizeLimit) {
						filesToUpload.push(file);
					} else {
						tooLargeFiles.push(file);
					}
				}

				if (filesToUpload.length < _Files.fileUploadList.length) {

					let errorText = `
						The following files are too large (limit ${_Files.fileSizeLimit / (1024 * 1024)} Mbytes):<br>
						${tooLargeFiles.map(tooLargeFile => `<b>${tooLargeFile.name}</b>: ${Math.round(tooLargeFile.size / (1024 * 1024))} Mbytes<br>`).join('')}
					`;

					new ErrorMessage().text(errorText).title('File(s) too large for upload').requiresConfirmation().show();
				}

				for (let fileToUpload of filesToUpload) {

					fileToUpload.parentId = _Files.currentWorkingDir ? _Files.currentWorkingDir.id : null;
					fileToUpload.hasParent = true; // Setting hasParent = true forces the backend to upload the file to the root dir even if parentId is null

					Command.createFile(fileToUpload, (createdFileNode) => {
						fileToUpload.id = createdFileNode.id;
						_Files.uploadFile(createdFileNode);
					});
				}

				return false;
			});
		}
	},
	uploadFile: (file) => {

		let worker = new Worker('js/upload-worker.js');
		worker.onmessage = function(e) {

			let binaryContent = e.data;
			let fileSize      = e.data.byteLength;
			let node          = Structr.node(file.id);
			node.find('.size').text(fileSize);

			// keep this code identical to "updateTextFile" code to ensure functionality
			let chunks = Math.ceil(fileSize / _Files.chunkSize);

			for (let c = 0; c < chunks; c++) {
				let start = c * _Files.chunkSize;
				let end   = (c + 1) * _Files.chunkSize;
				let chunk = window.btoa(String.fromCharCode.apply(null, new Uint8Array(binaryContent.slice(start, end))));
				Command.chunk(file.id, c, _Files.chunkSize, chunk, chunks);
			}
		};

		for (let fileObj of _Files.fileUploadList) {
			if (file.id === fileObj.id) {
				worker.postMessage(fileObj);
			}
		}
	},
	updateTextFile: (file, text, finishCallback) => {

		if (text === "") {

			Command.chunk(file.id, 0, _Files.chunkSize, "", 1, finishCallback);

		} else {

			let binaryContent = new TextEncoder().encode(text);
			let byteLength    = binaryContent.length;

			// keep this code identical to "uploadFile" code to ensure functionality
			let chunks = Math.ceil(byteLength / _Files.chunkSize);

			for (let c = 0; c < chunks; c++) {
				let start = c * _Files.chunkSize;
				let end   = (c + 1) * _Files.chunkSize;
				let chunk = window.btoa(String.fromCharCode.apply(null, new Uint8Array(binaryContent.slice(start, end))));
				Command.chunk(file.id, c, _Files.chunkSize, chunk, chunks, ((c+1 === chunks) ? finishCallback : undefined));
			}
		}
	},
	loadAndSetWorkingDir: (callback) => {

		Command.rest("/me/ui", (result) => {
			let me = result[0];
			if (me.workingDirectory) {
				_Files.currentWorkingDir = me.workingDirectory;
			} else {
				_Files.currentWorkingDir = null;
			}

			callback();
		});
	},
	load: (id, callback) => {

		Command.queryPromise('Folder', _Files.folderPageSize, _Files.folderPage, 'name', 'asc', { parent: id }, true, 'public', _Files.defaultFolderAttributes).then(folders => {

			return folders.map(d => {

				StructrModel.createOrUpdateFromData(d, null, false);

				return {
					id:       d.id,
					text:     d?.name ?? '[unnamed]',
					children: d.foldersCount > 0,
					icon:     _Icons.nonExistentEmptyIcon,
					data:     { svgIcon: _Icons.getSvgIcon(_Icons.getFolderIconSVG(d), 16, 24) },
					path:     d.path
				};
			});

		}).then(callback).catch(e => {
			//console.log('Caught error while refreshing tree');
		});
	},
	setWorkingDirectory: (id) => {

		if (id === 'root') {
			_Files.currentWorkingDir = null;
		} else {
			_Files.currentWorkingDir = { id: id };
		}

		fetch(`${Structr.rootUrl}me`, {
			method: 'PUT',
			body: JSON.stringify({ workingDirectory: _Files.currentWorkingDir })
		})
	},
	registerFolderLinks: () => {

		let openTargetNode = (targetId) => {
			_Files.getFilesTree().jstree('open_node', targetId, () => {
				_Files.getFilesTree().jstree('activate_node', targetId);
			});
		};

		for (let folderLink of _Files.getFolderContentsElement().querySelectorAll('.is-folder.file-icon')) {

			folderLink.addEventListener('click', (e) => {
				e.preventDefault();
				e.stopPropagation();

				let targetId = folderLink.dataset['targetId'];
				let parentId = folderLink.dataset['parentId'];

				if (!parentId || _Files.getFilesTree().jstree('is_open', parentId)) {
					openTargetNode(targetId);
				} else {
					_Files.getFilesTree().jstree('open_node', parentId, openTargetNode);
				}
			});
		}
	},
	updateFunctionBarStatus: () => {

		let addFolderButton   = document.getElementById('add-folder-button');
		let addFileButton     = document.getElementById('add-file-button');
		let mountDialogButton = document.getElementById('mount-folder-dialog-button');

		_Helpers.disableElements((_Files.helpers.isDisplayingFavorites() === true), addFolderButton, addFileButton, mountDialogButton);
	},
	displayFolderContents: (id, parentId, nodePath, parents) => {

		_Helpers.fastRemoveAllChildren(_Files.getFolderContentsElement());

		LSWrapper.setItem(_Files.filesLastOpenFolderKey, id);

		let isRootFolder           = (id === 'root');
		let parentIsRoot           = (parentId === '#');
		let listModeActive         = _Files.isViewModeActive('list');

		_Files.updateFunctionBarStatus();
		_Files.insertLayoutSwitches(id, parentId, nodePath, parents);

		// store current folder id so we can filter slow requests
		_Files.getFolderContentsElement().dataset['currentFolder'] = id;

		let handleFileChildren = (children) => {

			let currentFolder = _Files.getFolderContentsElement().dataset['currentFolder'];

			if (currentFolder === id) {

				for (let c of children) {
					_Files.appendFileOrFolder(c);
				}

				Structr.resize();
			}
		};

		if (_Files.helpers.isDisplayingFavorites() === true) {

			_Files.getFolderContentsElement().insertAdjacentHTML('beforeend', `
				<div class="folder-path truncate">${_Icons.getSvgIcon(_Icons.iconAddToFavorites)} Favorite Files</div>
				${listModeActive ? _Files.templates.folderContentsTableSkeleton() : _Files.templates.folderContentsTileContainerSkeleton()}
			`);

			_Favorites.getFavoritesList().then(data => {
				handleFileChildren(data.result);
			});

		} else {

			let handleFolderChildren = (folders) => {

				handleFileChildren(folders);

				_Files.registerFolderLinks();
			};

			Command.query('Folder', 1000, 1, 'name', 'asc', { parentId: (isRootFolder ? null : id) }, handleFolderChildren, true, null, _Files.defaultFolderAttributes);

			let pagerId = 'filesystem-files';
			_Pager.initPager(pagerId, 'File', 1, 25, 'name', 'asc');
			_Pager.page['File'] = 1;

			let filterOptions = {
				parentId: (parentIsRoot ? '' : id),
				hasParent: (!parentIsRoot)
			};

			_Pager.initFilters(pagerId, 'File', filterOptions, ['parentId', 'hasParent', 'isThumbnail']);

			let filesPager = _Pager.addPager(pagerId, _Files.getFolderContentsElement(), false, 'File', 'public', handleFileChildren, null, _Files.defaultFileAttributes, true, true);

			filesPager.cleanupFunction = () => {
				let toRemove = filesPager.el.querySelectorAll('.node.file');
				for (let item of toRemove) {
					_Helpers.fastRemoveElement(item.closest( (listModeActive ? 'tr' : '.tile') ));
				}
			};

			filesPager.appendFilterElements(`
				<span class="mr-1">Filter:</span>
				<input type="text" class="filter" data-attribute="name">
				<input type="text" class="filter" data-attribute="parentId" value="${(parentIsRoot ? '' : id)}" hidden>
				<input type="checkbox" class="filter" data-attribute="hasParent" ${(parentIsRoot ? '' : 'checked')} hidden>
			`);
			filesPager.activateFilterElements();
			filesPager.setIsPaused(false);
			filesPager.refresh();

			_Files.insertBreadCrumbNavigation(parents, nodePath, id);

			if (listModeActive) {

				_Files.getFolderContentsElement().insertAdjacentHTML('beforeend', _Files.templates.folderContentsTableSkeleton({
					tableContent: (isRootFolder ? '' : `
						<tr id="parent-folder-link" class="cursor-pointer">
							<td class="is-folder file-icon" data-target-id="${parentId}">${_Icons.getSvgIcon(_Icons.iconFolderClosed, 16, 16)}</td>
							<td>
								<div class="node folder flex items-center justify-between">
									<b class="name_ leading-8 truncate">..</b>
								</div>
							</td>
							<td></td>
							<td></td>
							<td></td>
							<td></td>
							<td></td>
						</tr>
					`)
				}));

			} else {

				_Files.getFolderContentsElement().insertAdjacentHTML('beforeend', _Files.templates.folderContentsTileContainerSkeleton({
					tilesContent: (isRootFolder ? '' : `
						<div id="parent-folder-link" class="tile${_Files.isViewModeActive('img') ? ' img-tile' : ''} cursor-pointer">
							<div class="node folder flex flex-col">
								<div class="is-folder file-icon" data-target-id="${parentId}">${_Icons.getSvgIcon(_Icons.iconFolderClosed, 40, 40)}</div>
								<b title=".." class="text-center">..</b>
							</div>
						</div>
					`)
				}));
			}

			if (!isRootFolder) {

				// allow drop on ".." element
				let parentObj = {
					id: parentId,
					type: (parentId === 'root') ? 'fake' : 'Folder'
				};

				_Dragndrop.files.enableDroppable(parentObj, _Files.getFolderContentsElement().querySelector('#parent-folder-link .node'));
			}
		}
	},
	insertBreadCrumbNavigation: (parents, nodePath, id) => {

		if (parents) {

			let modelObj = StructrModel.obj(id);
			if (modelObj && modelObj.path) {
				nodePath = modelObj.path;
			}

			parents = [].concat(parents).reverse().slice(1);

			let pathNames = (nodePath === '/') ? ['/'] : [''].concat(nodePath.slice(1).split('/'));

			_Files.getFolderContentsElement().insertAdjacentHTML('beforeend', `
				<div class="folder-path">
					${parents.map((parent, idx) => `<a class="breadcrumb-entry" data-folder-id="${parent}">${pathNames[idx]}/</a>`).join('')}<span id="current-folder-name">${pathNames.pop()}</span>
					<span class="context-menu-container"></span>
				</div>
			`);

			if (id != 'root') {

				Command.getPromise(id, null).then(obj => {
					let ctxMenuContainer = _Files.getFolderContentsElement().querySelector('.context-menu-container');
					_Entities.appendContextMenuIcon(ctxMenuContainer, obj, true);
				});
			}

			$('.breadcrumb-entry').click(function (e) {
				e.preventDefault();

				$('#' + $(this).data('folderId') + '_anchor').click();
			});
		}
	},
	insertLayoutSwitches: (id, parentId, nodePath, parents) => {

		let checkmark = _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green mr-2');

		_Files.getFolderContentsElement().insertAdjacentHTML('afterbegin',`
			<div id="switches" class="absolute flex top-4 right-2">
				<button class="switch ${(_Files.isViewModeActive('list') ? 'active' : 'inactive')} inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green" id="switch-list" data-view-mode="list">${(_Files.isViewModeActive('list') ? checkmark : '')} List</button>
				<button class="switch ${(_Files.isViewModeActive('tiles') ? 'active' : 'inactive')} inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green" id="switch-tiles" data-view-mode="tiles">${(_Files.isViewModeActive('tiles') ? checkmark : '')} Tiles</button>
				<button class="switch ${(_Files.isViewModeActive('img') ? 'active' : 'inactive')} inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green" id="switch-img" data-view-mode="img">${(_Files.isViewModeActive('img') ? checkmark : '')} Images</button>
			</div>
		`);

		let listSw  = $('#switch-list');
		let tilesSw = $('#switch-tiles');
		let imgSw   = $('#switch-img');

		let layoutSwitchFunction = function() {

			let state = $(this).hasClass('inactive');

			if (state) {
				let viewMode = $(this).data('viewMode');
				_Files.setViewMode(viewMode);

				_Entities.changeBooleanAttribute(listSw,  _Files.isViewModeActive('list'),  'List',   'List');
				_Entities.changeBooleanAttribute(tilesSw, _Files.isViewModeActive('tiles'), 'Tiles',  'Tiles');
				_Entities.changeBooleanAttribute(imgSw,   _Files.isViewModeActive('img'),   'Images', 'Images');

				_Files.displayFolderContents(id, parentId, nodePath, parents);
			}
		};

		listSw.on('click', layoutSwitchFunction);
		tilesSw.on('click', layoutSwitchFunction);
		imgSw.on('click', layoutSwitchFunction);
	},
	fileOrFolderCreationNotification: (newFileOrFolder) => {

		if ((_Files.currentWorkingDir === undefined || _Files.currentWorkingDir === null) && newFileOrFolder.parent === null) {
			_Files.appendFileOrFolder(newFileOrFolder);
		} else if ((_Files.currentWorkingDir !== undefined && _Files.currentWorkingDir !== null) && newFileOrFolder.parent && _Files.currentWorkingDir.id === newFileOrFolder.parent.id) {
			_Files.appendFileOrFolder(newFileOrFolder);
		}
	},
	fileOrFolderDeletionNotificationTimeout: undefined,
	fileOrFolderDeletionNotification: (deletedFileOrFolder) => {

		// this can have been called after multi-deleting files/folders, schedule it to probably only run once.
		window.clearTimeout(_Files.fileOrFolderDeletionNotificationTimeout);

		_Files.fileOrFolderDeletionNotificationTimeout = window.setTimeout(() => {

			if (deletedFileOrFolder.isFile) {

				// optimistically we should get away with only reloading the tree if a folder was deleted...
				// ...but that would be a bit more bookkeeping because we are trying to only run once
				_Files.refreshTree();

			} else if (deletedFileOrFolder.isFolder) {

				_Files.refreshTree();
			}

		}, 200);
	},
	getFormattedDate: (date) => {
		return _Files.dateFormat.format(new Date(date));
	},
	appendFileOrFolder: (d, container = _Files.getFolderContentsElement()) => {

		if (!d.isFile && !d.isFolder) return;

		StructrModel.createOrUpdateFromData(d, null, false);

		let size                  = d.isFolder ? (d.foldersCount + d.filesCount) : d.size;
		let createdDate           = _Files.getFormattedDate(d.createdDate);
		let modifiedDate          = _Files.getFormattedDate(d.lastModifiedDate);
		let progressIndicatorHTML = _Files.templates.progressIndicator({ size });
		let name                  = d.name || '[unnamed]';
		let listModeActive        = _Files.isViewModeActive('list');
		let tilesModeActive       = _Files.isViewModeActive('tiles');
		let imageModeActive       = _Files.isViewModeActive('img');
		let filePath              = d.path;
		let iconSize              = (tilesModeActive || imageModeActive) ? 40 : 16;
		let fileIcon              = (d.isFolder ? _Icons.getFolderIconSVG(d) : _Icons.getFileIconSVG(d));
		let fileIconHTML          = _Icons.getSvgIcon(fileIcon, iconSize, iconSize);
		let parentIdString        = d.parentId ? `data-parent-id="${d.parentId}"` : '';
		let ownerString           = (d.owner ? (d.owner.name ? d.owner.name : '[unnamed]') : '');

		if (listModeActive) {

			let getIconColumnHTML = () => {

				if (d.isFolder) {
					return `<td class="is-folder file-icon" data-target-id="${d.id}" ${parentIdString}>${fileIconHTML}</td>`;
				} else {
					return `<td class="file-icon"><a href="${filePath}" target="_blank">${fileIconHTML}</a></td>`;
				}
			};

			let rowId   = `row${d.id}`;
			let rowHTML = `
				<tr id="${rowId}" class="row${(d.isThumbnail ? ' thumbnail' : '')}">
					${getIconColumnHTML()}
					<td>
						<div id="id_${d.id}" class="node ${d.isFolder ? 'folder' : 'file'} flex items-center justify-between relative" draggable="true">
							<b class="name_ leading-8 truncate" data-key="name"></b>
							<div class="icons-container flex items-end"></div>
							${d.isFolder ? '' : progressIndicatorHTML}
						</div>
					</td>
					<td class="truncate id_ leading-8">${d.id}</td>
					<td class="truncate date">${createdDate}</td>
					<td class="truncate date" data-key="lastModifiedDate">${modifiedDate}</td>
					<td class="size whitespace-nowrap" data-key="size">${d.isFolder ? size : _Helpers.formatBytes(size, 0)}</td>
					<td class="truncate">${d.type}${(d.isThumbnail ? ' thumbnail' : '')}${(d.isFile && d.contentType ? ` (<span  data-key="contentType">${d.contentType}</span>)` : '')}</td>
					<td data-key="owner">${ownerString}</td>
				</tr>
			`;

			let row       = _Helpers.createSingleDOMElementFromHTML(rowHTML);
			let tableBody = container.querySelector('#files-table-body');

			_Helpers.fastRemoveElement(tableBody.querySelector(`#${rowId}`));

			// folders are appended after last folder, files are appended at the end
			if (d.isFile || !tableBody.hasChildNodes()) {
				tableBody.appendChild(row);
			} else {

				let lastFolder = [...tableBody.querySelectorAll('.is-folder')].pop()?.closest('tr');

				if (lastFolder) {

					lastFolder.insertAdjacentElement("afterend", row);

				} else {

					tableBody.insertBefore(row, tableBody.firstElementChild);
				}
			}

			_Elements.contextMenu.enableContextMenuOnElement(row, d);

		} else if (tilesModeActive || imageModeActive) {

			let getFileIcon = () => {

				if (d.isFolder) {

					return `<div class="is-folder file-icon" data-target-id="${d.id}" ${parentIdString}>${fileIconHTML}</div>`;

				} else {

					let thumbnailProperty = (tilesModeActive ? 'tnSmall' : 'tnMid');
					let displayImagePath  = (d.isThumbnail) ? filePath : (d[thumbnailProperty]?.path ?? filePath);
					let iconOrThumbnail   = d.isImage ? `<img class="tn" src="${displayImagePath}" draggable="false">` : fileIconHTML;

					return `
						<div class="file-icon">
							<a href="${filePath}" target="_blank" draggable="false">${iconOrThumbnail}</a>
						</div>
					`;
				}
			};

			let tileId   = `tile${d.id}`;
			let tileHTML = `
				<div id="${tileId}" class="tile${d.isThumbnail ? ' thumbnail' : ''}${imageModeActive ? ' img-tile' : ''}">
					<div id="id_${d.id}" class="node ${d.isFolder ? 'folder' : 'file'} relative flex flex-col" draggable="true">
					${getFileIcon()}
					<b class="name_ abbr-ellipsis mx-2 mb-2 text-center" data-key="name"></b>
					${d.isFolder ? '' : progressIndicatorHTML}
					<div class="icons-container flex items-center"></div>
				</div>
			`;

			let tile           = _Helpers.createSingleDOMElementFromHTML(tileHTML);
			let tilesContainer = container.querySelector('#tiles-container');

			_Helpers.fastRemoveElement(tilesContainer.querySelector(`#${tileId}`));

			// folders are appended after last folder, files are appended at the end
			if (d.isFile || !tilesContainer.hasChildNodes()) {
				tilesContainer.appendChild(tile);
			} else {

				let lastFolder = [...tilesContainer.querySelectorAll('.folder')].pop()?.closest('.tile');

				if (lastFolder) {

					lastFolder.insertAdjacentElement("afterend", tile);

				} else {

					tilesContainer.insertBefore(tile, tilesContainer.firstElementChild);
				}
			}

			_Elements.contextMenu.enableContextMenuOnElement(tile, d);
		}

		let div = Structr.node(d.id, '#id_', container);

		if (!div || !div.length) {
			return;
		}

		let nameElement = div[0].querySelector('b.name_');
		nameElement.textContent = name;
		nameElement.title       = name;
		nameElement.addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.makeNameEditable(div);
		});

		_Dragndrop.enableDraggable(d, div[0], _Dragndrop.dropActions.file, false, (e) => {

			let draggedElementIsSelected = div.hasClass('selected');

			if (!draggedElementIsSelected) {
				$('.node.selected').removeClass('selected');
			}

			_Dragndrop.files.draggedEntityIds = draggedElementIsSelected ? [...document.querySelectorAll('.node.selected')].map(x => Structr.getId(x)) : [d.id];

			let dragIcon       = (draggedElementIsSelected && _Dragndrop.files.draggedEntityIds.length > 1) ? _Icons.iconFilesStack : fileIcon;
			let dragCnt        = (draggedElementIsSelected && _Dragndrop.files.draggedEntityIds.length > 1) ? _Dragndrop.files.draggedEntityIds.length : _Dragndrop.dragEntity.name;
			let dragIconHTML   = _Icons.getSvgIcon(dragIcon, 20, 20, 'mr-1');
			let dragImgElement = _Helpers.createSingleDOMElementFromHTML(`<span class="bg-white inline-flex items-center p-1">${dragIconHTML}<div>${dragCnt}</div></span>`);

			// element must be in document to be displayed...
			Structr.mainContainer.appendChild(dragImgElement);

			let rect = dragImgElement.getBoundingClientRect();

			e.dataTransfer.setDragImage(dragImgElement, rect.width, rect.height);

			// ...remove it in the next animation frame
			requestAnimationFrame(() => {
				dragImgElement.remove();
			});

		}, (e) => {

			_Dragndrop.files.draggedEntityIds = [];
		});

		// even though it can be a file, enable droppable so we can react
		_Dragndrop.files.enableDroppable(d, div[0]);

		let iconsContainer = $('.icons-container', div);
		_Entities.appendContextMenuIcon(iconsContainer[0], d);
		_Entities.appendNewAccessControlIcon(iconsContainer, d, false);

		if (d.isFile) {

			div[0].closest('.node')?.addEventListener('dblclick', () => {

				if ($('b.name_', div).length > 0) {

					let contentType = d.contentType || '';

					if (d.isImage && contentType !== 'text/svg' && !contentType.startsWith('image/svg') && d.isTemplate !== true) {

						_Files.editImage(d);

					} else {

						_Files.editFile(d);
					}
				}
			});
		}

		_Entities.makeSelectable(div);
	},
	handleMoveObjectsAction: (targetFolderId, draggedObjectIds) => {

		/**
		 * handles a drop action to a folder located in the folder contents area...
		 * and a drop action to a folder located in the folder tree
		 *
		 * must take into account the selected elements in the folder contents area
		 */
		_Files.moveObjectsToTargetFolder(targetFolderId, draggedObjectIds).then((info) => {

			if (info.skipped.length > 0) {
				let text = `The following folders were not moved to prevent inconsistencies:<br><br>${info.skipped.map(id => StructrModel.obj(id)?.name ?? 'unknown').join('<br>')}`;
				new InfoMessage().title('Skipped some objects').text(text).show();
			}

			if (info.movedFolder === true) {
				// reload whole tree
				_Files.refreshTree();
			} else {
				// reload current directory only
				if (_Files.currentWorkingDir === null) {
					$('#root_anchor').click();
				} else {
					$(`#${_Files.currentWorkingDir.id}_anchor`).click();
				}
			}
		});
	},
	moveObjectsToTargetFolder: async (targetFolderId, objectIds) => {

		/**
		 * initial promise.all returns a list of moved object ids
		 * skipped objects have the "skipped_" prefix before the uuid
		 * result of the promise is an object telling us the skipped objects, the moved objects, and if there was a folder which has been moved
		 */
		let skippedPrefix = 'skipped_';
		let movePromises = [];

		for (let objectId of objectIds) {

			movePromises.push(new Promise((resolve, reject) => {

				// prevent moving element to self
				if (objectId !== targetFolderId) {

					Command.setProperty(objectId, 'parentId', targetFolderId, false, () => {
						resolve(objectId);
					});

				} else {
					resolve(skippedPrefix + objectId);
				}
			}));
		}

		return Promise.all(movePromises).then(values => {

			let movedObjectIds = values.filter(v => !v.startsWith(skippedPrefix));

			return {
				movedFolder: movedObjectIds.map(id => StructrModel.obj(id)).some(obj => obj?.type === 'Folder'),
				moved: movedObjectIds,
				skipped: values.filter(v => v.startsWith(skippedPrefix)).map(text => text.slice(skippedPrefix.length))
			};
		});
	},
	unpackArchive: (d) => {

		let message = `
			<div class="flex items-center justify-center">
				${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')}
				<div>Unpacking Archive - please stand by...</div>
			</div>
			<p>
				Extraction will run in the background.<br>
				You can safely close this popup and work during this operation.<br>
				You will be notified when the extraction has finished.
			</p>
		`;

		let { headingEl, messageEl, closeButton } = _Dialogs.tempInfoBox.show(message);

		closeButton.style.display = 'none';

		window.setTimeout(() => {
			closeButton.style.display = null;
		}, 500);

		Command.unarchive(d.id, _Files.currentWorkingDir ? _Files.currentWorkingDir.id : undefined, (data) => {

			if (data.success === true) {

				_Files.refreshTree();

				let closed = (messageEl.offsetParent === null);

				if (closed) {

					new SuccessMessage().text(message).requiresConfirmation().show();

				} else {

					_Helpers.fastRemoveAllChildren(messageEl);
					messageEl.insertAdjacentHTML('beforeend', `<div class="flex justify-center">${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, ['mr-2', 'icon-green'])} Extraction of '${data.filename}' finished successfully.</div>`);
				}

			} else {

				_Helpers.fastRemoveAllChildren(messageEl);
				messageEl.insertAdjacentHTML('beforeend', `<div class="flex justify-center">${_Icons.getSvgIcon(_Icons.iconErrorRedFilled, 16, 16, ['mr-2'])} Extraction failed.</div>`);
			}

			_Dialogs.basic.centerAll();
		});
	},
	editImage: (image) => {

		let { dialogText } = _Dialogs.custom.openDialog(image.name);

		let imagePath = image.path;

		dialogText.insertAdjacentHTML('beforeend', `
			<div class="image-editor-menubar">
				<div class="crop-action">
					${_Icons.getSvgIcon(_Icons.iconCropImage)}
					<br>Crop
				</div>
			</div>
			<div><img id="image-editor" class="orientation-${image.orientation}" src="${imagePath}"></div>
		`);

		let x,y,w,h;

		let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton();
		let saveAndClose     = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();

		dialogSaveButton.addEventListener('click', (e) => {
			e.preventDefault();
			e.stopPropagation();
			Command.createConvertedImage(image.id, Math.round(w), Math.round(h), null, Math.round(x), Math.round(y), () => {
				_Helpers.disableElements(true, dialogSaveButton, saveAndClose);
			});
		});

		saveAndClose.addEventListener('click', (e) => {
			e.stopPropagation();
			dialogSaveButton.click();

			window.setTimeout(() => {
				_Dialogs.custom.clickDialogCancelButton();
			}, 500);
		});

		dialogText.querySelector('.crop-action').addEventListener('click', () => {

			$('#image-editor').cropper({
				crop: (e) => {

					x = e.x, y = e.y, w = e.width, h = e.height;

					_Helpers.disableElements(false, dialogSaveButton, saveAndClose);
				}
			});
		});
	},
	editFile: (file, hasBeenWarned = false) => {

		let parent = Structr.node(file.id);

		_Files.fileContents = {};

		let selectedElements = $('.node.selected');
		if (selectedElements.length > 1 && parent.hasClass('selected')) {
			// selectedElements.removeClass('selected');
		} else {
			selectedElements = parent;
		}

		let hasFileAboveThreshold = false;

		let filteredFileModels = [];
		if (selectedElements && selectedElements.length > 1 && parent.hasClass('selected')) {

			for (let el of selectedElements) {
				let modelObj = StructrModel.obj(Structr.getId(el));
				if (modelObj && modelObj.isFolder !== true) {
					filteredFileModels.push(modelObj);
				}
			}

		} else {

			let modelObj = StructrModel.obj(file.id);
			if (!modelObj) {
				modelObj = StructrModel.create(file);
			}
			if (modelObj && modelObj.isFolder !== true) {
				filteredFileModels.push(file);
			}
		}

		let fileThresholdKB = 500;

		for (let fileModel of filteredFileModels) {
			if (fileModel.size > fileThresholdKB * 1024) {
				hasFileAboveThreshold = true;
			}
		}

		if (hasBeenWarned === false && hasFileAboveThreshold === true) {

			_Dialogs.confirmation.showPromise(`At least one file size is greater than ${fileThresholdKB} KB, do you really want to open that in an editor?`).then(confirm => {
				if (confirm === true) {
					_Files.editFile(file, true);
				}
			});

		} else {

			_Files.editFiles(filteredFileModels.map(m => m.id), file.id);
		}
	},
	editFiles:(ids, activeFileId, onTabSelect, onDialogClose) => {

		let { dialogText } = _Dialogs.custom.openDialog('Edit files', null, ['popup-dialog-with-editor']);
		_Dialogs.custom.showMeta();

		dialogText.insertAdjacentHTML('beforeend', '<div id="files-tabs" class="files-tabs flex flex-col h-full"><ul></ul></div>');

		let filesTabs     = document.getElementById('files-tabs');
		let filesTabsUl   = filesTabs.querySelector('ul');
		let loadedEditors = 0;

		for (let fileId of ids) {

			Command.get(fileId, 'id,type,name,contentType,isTemplate', (entity) => {

				loadedEditors++;

				let tab             = _Helpers.createSingleDOMElementFromHTML(`<li id="tab-${entity.id}" class="file-tab">${entity.name}</li>`);
				let editorContainer = _Helpers.createSingleDOMElementFromHTML(`<div id="content-tab-${entity.id}" class="content-tab-editor flex-grow flex"></div>`);

				filesTabsUl.appendChild(tab);
				filesTabs.appendChild(editorContainer);

				_Files.markFileEditorTabAsChanged(entity.id, _Files.fileHasUnsavedChanges[entity.id] ?? false);

				tab.addEventListener('click', (e) => {
					e.stopPropagation();

					// prevent activating the current tab
					if (!tab.classList.contains('active')) {

						// set all other tabs inactive and this one active
						for (let tab of filesTabsUl.querySelectorAll('li')) {
							tab.classList.remove('active');
						}
						tab.classList.add('active');

						// hide all editors and show this one
						for (let otherEditorContainer of filesTabs.querySelectorAll('div.content-tab-editor')) {
							otherEditorContainer.style.display = 'none';
						}
						editorContainer.style.display = 'block';

						// clear all other tabs before editing this one to ensure correct height
						for (let editor of filesTabs.querySelectorAll('.content-tab-editor')) {
							_Helpers.fastRemoveAllChildren(editor);
						}

						onTabSelect?.(entity.id);

						_Files.editFileWithMonaco(entity, editorContainer, onDialogClose);
					}

					return false;
				});

				if (activeFileId === entity.id) {
					tab.click();
				}
			});
		}
	},
	markFileEditorTabAsChanged: (id, hasChanges) => {
		let tab = document.querySelector(`#tab-${id}`);
		_Schema.markElementAsChanged(tab, hasChanges);
	},
	editFileWithMonaco: async (file, editorContainer, onCloseCallback) => {

		let saveButton         = _Dialogs.custom.updateOrCreateDialogSaveButton();
		let saveAndCloseButton = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();

		editorContainer.insertAdjacentHTML('beforeend', '<div class="editor h-full overflow-hidden"></div><div id="template-preview"><textarea readonly></textarea></div>');

		let urlForFileAndPreview = `${Structr.viewRootUrl}${file.id}?${Structr.getRequestParameterName('edit')}=1`;
		let fileResponse         = await fetch(urlForFileAndPreview);
		let data                 = await fileResponse.text();
		let initialText          = _Files.fileContents[file.id] || data;

		let monacoChangeFn = (editor, entity) => {

			let currentText = editor.getValue();

			// Store current editor text
			_Files.fileContents[file.id] = currentText;

			_Files.fileHasUnsavedChanges[file.id] = (data !== currentText);

			_Files.markFileEditorTabAsChanged(file.id, _Files.fileHasUnsavedChanges[file.id]);

			_Helpers.disableElements((data === currentText), saveButton, saveAndCloseButton);
		};

		let fileMonacoConfig = {
			value: initialText,
			language: _Files.getLanguageForFile(file),
			lint: true,
			autocomplete: true,
			changeFn: monacoChangeFn
		};

		let dialogMeta = _Dialogs.custom.getDialogMetaElement();

		_Helpers.fastRemoveAllChildren(dialogMeta);
		dialogMeta.insertAdjacentHTML('beforeend', '<span class="editor-info"></span>');

		let monacoEditor = _Editors.getMonacoEditor(file, 'content', editorContainer.querySelector('.editor'), fileMonacoConfig);

		_Editors.addEscapeKeyHandlersToPreventPopupClose(file.id, 'content', monacoEditor);

		let editorInfo = dialogMeta.querySelector('.editor-info');
		_Editors.appendEditorOptionsElement(editorInfo);
		let { isTemplateCheckbox, showPreviewCheckbox } = _Files.appendTemplateConfig(editorInfo, monacoEditor, file, editorContainer, urlForFileAndPreview);

		_Editors.resizeVisibleEditors();

		fileMonacoConfig.changeFn(monacoEditor);
		monacoEditor.focus();

		saveButton.addEventListener('click', (e) => {

			e.preventDefault();
			e.stopPropagation();

			let newText = monacoEditor.getValue();
			if (data === newText) {
				return;
			}

			// update current value so we can check against it
			data = newText;
			fileMonacoConfig.changeFn(monacoEditor);

			let saveFileAction = (callback) => {
				_Files.updateTextFile(file, newText, callback);
				initialText = newText;
			};

			if (isTemplateCheckbox.checked) {

				_Entities.setProperty(file.id, 'isTemplate', false, false, () => {
					saveFileAction(() => {
						_Entities.setProperty(file.id, 'isTemplate', true, false, () => {
							let active = showPreviewCheckbox.checked;
							if (active) {
								_Files.updateTemplatePreview(editorContainer, urlForFileAndPreview);
							}
						});
					});
				});

			} else {

				saveFileAction();
			}
		});

		let hasUnsavedChanges = () => {
			return (document.querySelectorAll('.file-tab.has-changes').length > 0);
		}

		let newCancelButton = _Dialogs.custom.updateOrCreateDialogCloseButton();
		_Dialogs.custom.setHasCustomCloseHandler();

		newCancelButton.addEventListener('click', async (e) => {

			e.stopPropagation();

			let closeWithoutAsking = (hasUnsavedChanges() === false) || (true === await _Dialogs.confirmation.showPromise('You have unsaved changes, really close without saving?'));

			if (closeWithoutAsking) {

				_Dialogs.custom.dialogCancelBaseAction();

				if (onCloseCallback) {
					window.setTimeout(onCloseCallback, 100);
				}
			}
		});

		saveAndCloseButton.addEventListener('click', (e) => {
			e.stopPropagation();
			saveButton.click();

			if (!hasUnsavedChanges()) {
				window.setTimeout(() => {
					newCancelButton.click();
				}, 250);
			}
		});

		Structr.resize();
	},
	appendTemplateConfig: (element, editor, file, outerElement, urlForFileAndPreview) => {

		element.insertAdjacentHTML('beforeend', `
			<label for="isTemplate">Replace template expressions: <input id="isTemplate" type="checkbox" ${file.isTemplate ? 'checked' : ''}></label>
			<label for="showTemplatePreview">Show preview: <input id="showTemplatePreview" type="checkbox" ${file.isTemplate ? '' : 'disabled=disabled'}></label>
		`);

		let isTemplateCheckbox   = element.querySelector('#isTemplate');
		let showPreviewCheckbox  = element.querySelector('#showTemplatePreview');

		_Helpers.appendInfoTextToElement({
			text: "Expressions like <pre>Hello ${print(me.name)} !</pre> will be evaluated. To see a preview, tick the adjacent checkbox.",
			element: $(isTemplateCheckbox),
			insertAfter: true,
			css: {
				"margin-right": "4px"
			}
		});

		isTemplateCheckbox.addEventListener('change', () => {
			let active = isTemplateCheckbox.checked;
			_Entities.setProperty(file.id, 'isTemplate', active, false, () => {
				file.isTemplate = active;
				showPreviewCheckbox.disabled = !active;

				let language = _Files.getLanguageForFile(file);
				_Editors.updateMonacoEditorLanguage(editor, language, file);
			});
		});

		showPreviewCheckbox.addEventListener('change', () => {

			if (showPreviewCheckbox.checked) {

				_Files.updateTemplatePreview(outerElement, urlForFileAndPreview);

			} else {

				let previewArea = document.querySelector('#template-preview');
				previewArea.style.display = 'none';
				previewArea.querySelector('textarea').value = '';
				outerElement.querySelector('.editor').style.width = 'inherit';
			}

			_Editors.resizeVisibleEditors();
		});

		return { isTemplateCheckbox, showPreviewCheckbox };
	},
	getLanguageForFile: (file) => {

		let language    = 'text';
		let contentType = file.contentType ?? '';
		let fileName    = file.name;

		if (file.isTemplate || (contentType.startsWith('application/javascript') || contentType.startsWith('text/javascript'))) {

			language = 'javascript';

		} else {

			if (fileName.endsWith('.css')) {
				language = 'css';
			} else if (fileName.endsWith('.js') || fileName.endsWith('.mjs')) {
				language = 'javascript';
			} else if (fileName.endsWith('.svg')) {
				language = 'xml';
			} else if (fileName.endsWith('.sql')) {
				language = 'sql';
			}
		}

		return language;
	},
	dialogSizeChanged: () => {
		_Editors.resizeVisibleEditors();
	},
	updateTemplatePreview: async (element, url) => {

		let contentBox = element.querySelector('.editor');
		contentBox.style.width = '50%';

		let previewArea = document.querySelector('#template-preview');
		previewArea.style.display = 'block';

		let response = await fetch(url.substr(0, url.indexOf('?')));
		let text     = await response.text();

		previewArea.querySelector('textarea').value = text;
	},
	isArchive: (file) => {

		let contentType = file.contentType;
		let extension = file.name.substring(file.name.lastIndexOf('.') + 1);

		let archiveTypes = ['application/zip', 'application/x-tar', 'application/x-cpio', 'application/x-dump', 'application/x-java-archive', 'application/x-7z-compressed', 'application/x-ar', 'application/x-arj'];
		let archiveExtensions = ['zip', 'tar', 'cpio', 'dump', 'jar', '7z', 'ar', 'arj'];

		return archiveTypes.includes(contentType) || archiveExtensions.includes(extension);
	},
	openMountDialog: () => {

		_Schema.getTypeInfo('Folder', (typeInfo) => {

			let { dialogText } = _Dialogs.custom.openDialog('Mount Folder', null, ['show-dialog-title']);

			dialogText.insertAdjacentHTML('beforeend', _Files.templates.mountDialog({typeInfo: typeInfo}));
			_Helpers.activateCommentsInElement(dialogText);

			let mountButton = _Dialogs.custom.prependCustomDialogButton('<button id="mount-folder" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Mount</button>');

			let getDataFromElements = (elements) => {

				let data = {};

				for (let element of elements) {

					let attrName = element.dataset['attributeName'];
					if (element.type === 'number') {
						if (element.value != '') {
							data[attrName] = parseInt(element.value);
						}
					} else if (element.type === 'checkbox') {
						data[attrName] = element.checked;
					} else {
						if (element.value != '') {
							data[attrName] = element.value;
						}
					}
				}

				return data;
			};

			mountButton.addEventListener('click', async () => {

				let mountConfig = Object.assign({
					type: 'Folder',
					parentId: (_Files.currentWorkingDir ? _Files.currentWorkingDir.id : null)
				}, getDataFromElements(dialogText.querySelectorAll('.mount-folder-option[data-attribute-name]')));

				let storageConfigurationData      = getDataFromElements(dialogText.querySelectorAll('.mount-storageconfiguration-option[data-attribute-name]'));
				let storageConfigurationEntryData = getDataFromElements(dialogText.querySelectorAll('.mount-storageconfigurationentry-option[data-attribute-name]'))

				if (!mountConfig.name) {

					_Dialogs.custom.showAndHideInfoBoxMessage('Must supply name', 'warning', 2000);

				} else if (!storageConfigurationEntryData.mountTarget) {

					_Dialogs.custom.showAndHideInfoBoxMessage('Must supply mount target', 'warning', 2000);

				} else {

					mountConfig.storageConfiguration         = storageConfigurationData;
					mountConfig.storageConfiguration.name    = 'Configuration for ' + mountConfig.name;
					mountConfig.storageConfiguration.entries = [ Object.fromEntries(Object.entries(storageConfigurationEntryData).map(entry => [ ['name', entry[0]], ['value', entry[1]] ]).flat()) ];

					let response = await fetch ('/structr/rest/Folder', {
						method: 'POST',
						body: JSON.stringify(mountConfig)
					});

					if (response.ok) {

						_Dialogs.custom.clickDialogCancelButton();

						_Files.refreshTree();

					} else {

						let json = await response.json();

						if (json.code === 422 && json.message === 'Unable to encrypt data, no secret key set.') {

							let warningMessage = `
								The mount configuration may contain sensitive information (credentials etc) and thus is an encrypted field.</br><br>
								For this, the configuration key <code>application.encryption.secret</code> in structr.conf has to be set.<br><br>
								Please update your configuration settings via the config editor (to have the key permanently set) or set it via the builtin function <code>set_encryption_key(key)</code>.<br><br>
								Please keep in mind that a changed encryption key will have the effect that previously encrypted fields will not be readable anymore with the changed key.
							`;

							new WarningMessage().title("Encryption Secret Configuration Required").text(warningMessage).requiresConfirmation().show();

						} else {
							Structr.errorFromResponse(json);
						}
					}
				}
			});
		});
	},
	search: {
		searchField: undefined,
		searchFieldClearIcon: undefined,

		initSearch: () => {

			_Files.search.searchField = Structr.functionBar.querySelector('#files-search-box');

			_Files.search.searchFieldClearIcon = document.querySelector('.clearSearchIcon');
			_Files.search.searchFieldClearIcon.addEventListener('click', (e) => {
				_Files.search.clearSearch();
			});

			_Files.search.searchField.focus();

			_Files.search.searchField.addEventListener('keyup', async (e) => {

				let searchString = _Files.search.searchField.value;

				if (searchString && searchString.length) {
					_Files.search.searchFieldClearIcon.style.display = 'block';
				}

				if (searchString && searchString.length && e.keyCode === 13) {

					await _Files.search.fulltextSearch(searchString);

				} else if (e.keyCode === 27 || searchString === '') {
					_Files.search.clearSearch();
				}
			});

		},
		getCurrentSearchTerm: () => {
			return _Files.getFolderContentsElement().querySelector('#search-results').dataset['for'];
		},
		fulltextSearch: async (searchString) => {

			for (let el of _Files.getFolderContentsElement().children) {
				el.style.display = 'none';
			}

			_Files.search.removeSearchResults();

			let container = _Helpers.createSingleDOMElementFromHTML('<div id="search-results"></div>');
			_Files.getFolderContentsElement().appendChild(container);

			container.dataset['for'] = searchString;

			let response = await fetch(`${Structr.rootUrl}File/ui?${Structr.getRequestParameterName('inexact')}=1${searchString.split(' ').map(str => '&extractedContent=' + str).join('')}`);

			if (response.ok) {

				let data = await response.json();

				if (!data.result || data.result.length === 0) {

					container.insertAdjacentHTML('beforeend', `
						<h1>No results for "${searchString}"</h1>
						<h2>Press ESC or click <a href="javascript:_Files.search.clearSearch();" class="clear-results">here to clear</a> empty result list.</h2>
					`);

				} else {

					container.insertAdjacentHTML('beforeend', `
						<div class="font-bold mb-4 text-3xl">${data.result.length} result${data.result.length > 1 ? 's' : ''}:</div>
						${_Files.templates.folderContentsTableSkeleton({})}
					`);

					for (let fileHit of data.result) {

						_Files.appendFileOrFolder(fileHit, container);
					}
				}
			}
		},
		getAndAppendSearchContext: async (fileId, searchString) => {

			let searchResultContextRowId = `searchContext_${fileId}`;
			let contextAlreadyShown      = !!_Files.getFolderContentsElement().querySelector(`#${searchResultContextRowId}`);
			let searchResultRow          = _Files.getFolderContentsElement().querySelector(`#search-results #row${fileId}`);

			if (!searchResultRow || contextAlreadyShown) {
				return;
			}

			let contextResponse = await fetch(`${Structr.rootUrl}File/${fileId}/getSearchContext`, {
				method: 'POST',
				body: JSON.stringify({
					searchString: searchString,
					contextLength: 30
				})
			});

			if (contextResponse.ok) {

				let data = await contextResponse.json();

				if (data.result) {

					let maxHClassname = 'max-h-24';
					let numCols        = searchResultRow.querySelectorAll('td').length;
					let newContextRow  = _Helpers.createSingleDOMElementFromHTML(`<div id="${searchResultContextRowId}" style="display: table-row;"></div>`);
					let newContextCell = _Helpers.createSingleDOMElementFromHTML(`
						<td colspan="${numCols}">
							<div class="flex items-start gap-2 p-4 mb-2 bg-gray ${maxHClassname} overflow-y-auto">
								<div class="flex-shrink-0">
									<i class="toggle-height fa fa-expand cursor-pointer"></i>
								</div>
								<div context-container class="grid grid-cols-3 gap-8 items-start"></div>
							</div>
						</td>
					`);
					searchResultRow.insertAdjacentElement('afterend', newContextRow);
					newContextRow.appendChild(newContextCell);

					let contextsContainer = newContextCell.querySelector('[context-container]');

					newContextCell.querySelector('.toggle-height').addEventListener('click', e => {
						newContextCell.firstElementChild.classList.toggle(maxHClassname);
						e.target.classList.toggle('fa-expand');
						e.target.classList.toggle('fa-compress');
					});

					let splitSearchStringRegex    = new RegExp('(' + searchString.split(/[\s,;]/).join('|') + ')', 'gi');
					let highlightedContextStrings = data.result.context.map(contextString => `<div class="bg-white px-2 py-1">${contextString.replace(splitSearchStringRegex, '<span class="highlight">$1</span>')}</div>`);

					contextsContainer.insertAdjacentHTML('beforeend', highlightedContextStrings.join(''));
				}
			}
		},
		clearSearch: () => {

			_Files.search.searchField.value = '';
			_Files.search.searchFieldClearIcon.style.display = 'none';

			_Files.search.removeSearchResults();
			for (let el of _Files.getFolderContentsElement().children) {
				el.style.display = null;
			}
		},
		removeSearchResults: () => {

			_Files.getFolderContentsElement().querySelector('#search-results')?.remove();
		},
	},
	helpers: {
		isDisplayingFavorites: () => {
			return _Files.getFolderContentsElement().dataset.currentFolder === 'favorites';
		},
		isDisplayingSearchResults: () => {
			return !!_Files.getFolderContentsElement().querySelector('#search-results');
		}
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/files.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/lib/cropper.min.css">

			<div class="tree-main" id="files-main">

				<div class="column-resizer"></div>

				<div class="tree-container" id="file-tree-container">
					<div class="tree" id="file-tree">
					</div>
				</div>

				<div class="tree-contents-container" id="folder-contents-container">
					<div class="tree-contents tree-contents-with-top-buttons" id="folder-contents">
					</div>
				</div>

			</div>
		`,
		functions: config => `
			<div id="files-action-buttons" class="flex-grow">

				<div class="inline-flex">

					<select class="select-create-type combined-select-create" id="folder-type">
						<option value="Folder">Folder</option>
						${config.folderTypes.map(type => '<option value="' + type + '">' + type + '</option>').join('')}
					</select>

					<button class="action button inline-flex items-center combined-select-create" id="add-folder-button">
						${_Icons.getSvgIcon(_Icons.iconCreateFolder, 16, 16, ['mr-2'])}
						<span>Create</span>
					</button>

					<select class="select-create-type combined-select-create" id="file-type">
						<option value="File">File</option>
						${config.fileTypes.map(type => '<option value="' + type + '">' + type + '</option>').join('')}
					</select>

					<button class="action button inline-flex items-center combined-select-create" id="add-file-button">
						${_Icons.getSvgIcon(_Icons.iconCreateFile, 16, 16, ['mr-2'])}
						<span>Create</span>
					</button>

					<button class="mount_folder button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green" id="mount-folder-dialog-button">
						${_Icons.getSvgIcon(_Icons.iconMountedFolderOpen, 16, 16, ['mr-2'])}
						Mount Folder
					</button>
				</div>
			</div>

			<div class="searchBox module-dependent" data-structr-module="text-search">
				<input id="files-search-box" class="search" name="search" placeholder="Search...">
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
			</div>
		`,
		folderContentsTableSkeleton: config => `
			<table id="files-table" class="stripe">
				<thead>
					<tr>
						<th class="icon">&nbsp;</th>
						<th>Name</th>
						<th>ID</th>
						<th>Created</th>
						<th>Modified</th>
						<th>Size</th>
						<th>Type</th>
						<th>Owner</th>
					</tr>
				</thead>
				<tbody id="files-table-body">
					${config?.tableContent ?? ''}
				</tbody>
			</table>
		`,
		folderContentsTileContainerSkeleton: config => `
			<div id="tiles-container" class="flex flex-wrap">
				${config?.tilesContent ?? ''}
			</div>
		`,
		progressIndicator: config => `
			<div class="progress">
				<div class="bar">
					<div class="indicator">
						<span class="part"></span>/<span class="size">${config.size}</span>
					</div>
				</div>
			</div>
		`,
		mountDialog: config => `
			<table id="mount-dialog" class="props">
				<tr>
					<td data-comment="Different storage providers have different capabilities. Currently the only existing storage provider is for mounting directories from a filesystem local to the server. In future versions storage providers for cloud storage are planned.">Storage Provider</td>
					<td>
						<select class="mount-storageconfiguration-option" data-attribute-name="provider" disabled>
							<option value="org.structr.storage.providers.local.LocalFSStorageProvider">Local Filesystem</option>
						</select>
					</td>
				</tr>
				<tr>
					<td data-comment="The name of the folder that will be created in the current folder.">Name</td>
					<td><input type="text" class="mount-folder-option" data-attribute-name="name"></td>
				</tr>
				<tr>
					<td data-comment="The absolute path of the local directory to mount">Mount Target</td>
					<td><input type="text" class="mount-storageconfigurationentry-option" data-attribute-name="mountTarget"></td>
				</tr>
				<tr>
					<td>Do Fulltext Indexing</td>
					<td><input type="checkbox" class="mount-folder-option" data-attribute-name="mountDoFulltextIndexing"></td>
				</tr>
				<tr>
					<td data-comment="The scan interval for repeated scans of this mount target">Scan Interval (s)</td>
					<td><input type="number" class="mount-folder-option" data-attribute-name="mountScanInterval"></td>
				</tr>
				<tr>
					<td data-comment="Folders encountered underneath this mounted folder are created with this type">Mount Target Folder Type</td>
					<td><input type="text" class="mount-folder-option" data-attribute-name="mountTargetFolderType"></td>
				</tr>
				<tr>
					<td data-comment="Files encountered underneath this mounted folder are created with this type">Mount Target File Type</td>
					<td><input type="text" class="mount-folder-option" data-attribute-name="mountTargetFileType"></td>
				</tr>
				<tr>
					<td data-comment="List of checksum types which are being automatically calculated on file creation.<br>Supported values are: crc32, md5, sha1, sha512">Enabled Checksums</td>
					<td><input type="text" class="mount-folder-option" data-attribute-name="enabledChecksums"></td>
				</tr>
				<tr>
					<td data-comment="Registers this path with a watch service (if supported by operating/file system)">Watch Folder Contents</td>
					<td><input type="checkbox" class="mount-folder-option" data-attribute-name="mountWatchContents"></td>
				</tr>
			</table>
		`,
	}
};