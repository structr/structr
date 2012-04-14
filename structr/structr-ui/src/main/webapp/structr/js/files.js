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

var images, files, folders, drop;
var fileList;
var chunkSize = 1024*64;
var sizeLimit = 1024*1024*42;

$(document).ready(function() {
	Structr.registerModule('files', _Files);
	Structr.classes.push('file');
	Structr.classes.push('folder');
	Structr.classes.push('image');
});

var _Files = {

	icon : 'icon/page_white.png',
	add_file_icon : 'icon/page_white_add.png',
	delete_file_icon : 'icon/page_white_delete.png',
	add_folder_icon : 'icon/folder_add.png',
	folder_icon : 'icon/folder.png',
	delete_folder_icon : 'icon/folder_delete.png',
	download_icon : 'icon/basket_put.png',
	
	init : function() {
	//Structr.classes.push('file');
	//Structr.classes.push('folder');
	//Structr.classes.push('image');
	},

	onload : function() {
		if (debug) console.log('onload');
		if (palette) palette.remove();

		main.append('<table><tr><td id="folders"></td><td id="files"></td><td id="images"></td></tr></table>');
		folders = $('#folders');
		files = $('#files');
		images = $('#images');
        
		_Files.refreshFiles();
		_Files.refreshImages();
		_Files.refreshFolders();
	},

	refreshFiles : function() {
		files.empty();
		
		if (window.File && window.FileReader && window.FileList && window.Blob) {

			files.append('<div id="filesDropArea">Drop files here</div>');
            
			drop = $('#filesDropArea');

			drop.on('dragover', function(event) {
				event.originalEvent.dataTransfer.dropEffect = 'copy';
				return false;
			});
            
			drop.on('drop', function(event) {
				event.stopPropagation()
				event.preventDefault();
				//                console.log(event);
				fileList = event.originalEvent.dataTransfer.files;
				console.log(fileList);
				var filesToUpload = [];
				var tooLargeFiles = [];

				$(fileList).each(function(i, file) {
					if (file.size <= sizeLimit) {
						filesToUpload.push(file);
					} else {
						tooLargeFiles.push(file);
					}
				});

				if (filesToUpload.length < fileList.length) {

					var errorText = 'The following files are too large (limit ' + sizeLimit/(1024*1024) + ' Mbytes):<br>\n';

					$(tooLargeFiles).each(function(i, tooLargeFile) {
						errorText += tooLargeFile.name + ': ' + Math.round(tooLargeFile.size/(1024*1024)) + ' Mbytes<br>\n';
					});

					Structr.error(errorText, function() {
						$.unblockUI({ fadeOut: 25 });
						$(filesToUpload).each(function(i, file) {
							if (debug) console.log(file);
							if (file) _Files.createFile(file);
						});
					})
				} else {
					$(fileList).each(function(i, file) {
						if (debug) console.log(file);
						if (file) _Files.createFile(file);
					});

				}

				return false;
			});
		}
		_Entities.list('File');
	},
	
	refreshImages : function() {
		images.empty();
		_Entities.list('Image');
	},
	
	refreshFolders : function() {
		folders.empty();
		if (_Entities.list('Folder')) {
			folders.append('<button class="add_folder_icon button"><img title="Add Folder" alt="Add Folder" src="' + _Files.add_folder_icon + '"> Add Folder</button>');
			$('.add_folder_icon', main).on('click', function() {
				var entity = {};
				entity.type = 'Folder';
				_Entities.create(this, entity);
			});
		}
	},

	appendFileElement : function(file, parentId) {

		if (debug) console.log('Files.appendFileElement: parentId: ' + parentId + ', file: ', file);

		var icon = _Files.icon; // default
		if (file.contentType) {

			if (file.contentType.indexOf('pdf') > -1) {
				icon = 'icon/page_white_acrobat.png';
			} else if (file.contentType.indexOf('text') > -1) {
				icon = 'icon/page_white_text.png';
			} else if (file.contentType.indexOf('xml') > -1) {
				icon = 'icon/page_white_code.png';
			}

		}

		var parent = Structr.findParent(parentId, null, files);
        
		parent.append('<div class="file ' + file.id + '_">'
			+ '<img class="typeIcon" src="'+ icon + '">'
			+ '<b class="name_">' + file.name + '</b> <span class="id">' + file.id + '</span>'
			+ '</div>');
		
		var div = $('.' + file.id + '_', parent);

		$('.typeIcon', div).on('click', function() {
			window.open(viewRootUrl + file.name, 'Download ' + file.name);
		});
		
		if (parentId) {

			div.append('<img title="Remove file \'' + file.name + '\' from folder ' + parentId + '" alt="Remove file \'' + file.name + '\' from folder" class="delete_icon button" src="' + _Files.delete_file_icon + '">');
			$('.delete_icon', div).on('click', function() {
				_Files.removeFileFromFolder(file.id, parentId);
			});
			
		} else {
		
			div.append('<img title="Delete file \'' + file.name + '\'" alt="Delete file \'' + file.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
			$('.delete_icon', div).on('click', function() {
				_Files.deleteFile(this, file);
			});
		
		}
		//        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
		//        $('.add_icon', div).on('click', function() {
		//            Resources.addElement(this, resource);
		//        });
		$('b', div).on('click', function() {
			//_Entities.showProperties(this, file, 'all', $('.' + file.id + '_', files));
			_Entities.showProperties(this, file, $('#dialogBox .dialogText'));
		});
		
		div.draggable({
			revert: 'invalid',
			containment: '#main',
			zIndex: 1
		});

		_Entities.appendAccessControlIcon(div, file);
        
		return div;
	},
	
	appendImageElement : function(file, parentId) {
		if (debug) console.log('Files.appendImageElement: parentId: ' + parentId + ', file: ', file);
		var	icon = viewRootUrl + file.name;
		var parent = Structr.findParent(parentId, null, images);

		parent.append('<div class="image ' + file.id + '_">'
			+ '<img class="typeIcon" src="'+ icon + '">'
			+ '<b class="name_">' + file.name + '</b> <span class="id">' + file.id + '</span>'
			+ '</div>');
		
		var div = $('.' + file.id + '_', parent);

		$('.typeIcon', div).on('click', function() {
			window.open(viewRootUrl + file.name, 'Download ' + file.name);
		});
		
		if (parentId) {

			div.append('<img title="Remove file \'' + file.name + '\' from folder ' + parentId + '" alt="Remove file \'' + file.name + '\' from folder" class="delete_icon button" src="' + _Files.delete_file_icon + '">');
			$('.delete_icon', div).on('click', function() {
				_Files.removeImageFromFolder(file.id, parentId);
			});
			
		} else {
		
			div.append('<img title="Delete file \'' + file.name + '\'" alt="Delete file \'' + file.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
			$('.delete_icon', div).on('click', function() {
				_Files.deleteFile(this, file);
			});
		
		}
		//        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
		//        $('.add_icon', div).on('click', function() {
		//            Resources.addElement(this, resource);
		//        });
		$('b', div).on('click', function() {
			//_Entities.showProperties(this, file, 'all', $('.' + file.id + '_', images));
			_Entities.showProperties(this, file, $('#dialogBox .dialogText'));
		});
		
		div.draggable({
			revert: 'invalid',
			containment: '#main',
			zIndex: 1
		});
        
		_Entities.appendAccessControlIcon(div, file);

		return div;
	},
		
	appendFolderElement : function(folder, parentId) {
		//        var parent;
		//        if (debug) console.log(parentId);
		//        if (parentId) {
		//            parent = $('.' + parentId + '_');
		//            if (debug) console.log(parent);
		//        } else {
		//            parent = folders;
		//        }
		
		if (debug) console.log('Folder: ', folder);
		var parent = Structr.findParent(parentId, null, folders);
		
		parent.append('<div structr_type="folder" class="folder ' + folder.id + '_">'
			+ '<img class="typeIcon" src="'+ _Files.folder_icon + '">'
			+ '<b class="name_">' + folder.name + '</b> <span class="id">' + folder.id + '</span>'
			+ '</div>');
		var div = $('.' + folder.id + '_', parent);
		div.append('<img title="Delete content \'' + folder.name + '\'" alt="Delete content \'' + folder.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
		$('.delete_icon', div).on('click', function() {
			_Files.deleteFolder(this, folder);
		});
		//        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
		//        $('.add_icon', div).on('click', function() {
		//            Resources.addElement(this, resource);
		//        });
		$('b', div).on('click', function() {
			_Entities.showProperties(this, folder, $('#dialogBox .dialogText'));
		});
		
		div.droppable({
			accept: '.file, .image',
			greedy: true,
			hoverClass: 'folderHover',
			drop: function(event, ui) {
				var fileId = getIdFromClassString(ui.draggable.attr('class'));
				var folderId = getIdFromClassString($(this).attr('class'));
				var nodeData = {};
				nodeData.id = fileId;
				_Entities.createAndAdd(folderId, nodeData);
			}
		});

		_Entities.appendAccessControlIcon(div, folder);
		
		return div;
	},
	
	addFileToFolder : function(fileId, folderId) {

		var folder = $('.' + folderId + '_');
		var file = $('.' + fileId + '_');

		folder.append(file);

		$('.delete_icon', file).remove();
		file.append('<img title="Remove file ' + fileId + ' from folder ' + folderId + '" '
			+ 'alt="Remove file ' + fileId + ' from folder ' + folderId + '" class="delete_icon button" src="' + _Files.delete_file_icon + '">');
		$('.delete_icon', file).on('click', function() {
			_Files.removeFileFromFolder(fileId, folderId)
		});
		file.draggable('destroy');

		var numberOfFiles = $('.file', folder).size();
		if (debug) console.log(numberOfFiles);
		if (numberOfFiles > 0) {
			disable($('.delete_icon', folder)[0]);
		}

	},

	addImageToFolder : function(imageId, folderId) {

		var folder = $('.' + folderId + '_');
		var image = $('.' + imageId + '_');

		folder.append(image);

		$('.delete_icon', image).remove();
		image.append('<img title="Remove image ' + imageId + ' from folder ' + folderId + '" '
			+ 'alt="Remove image ' + imageId + ' from folder ' + folderId + '" class="delete_icon button" src="' + _Files.delete_file_icon + '">');
		$('.delete_icon', image).on('click', function() {
			_Files.removeImageFromFolder(imageId, folderId)
		});
		image.draggable('destroy');

		var numberOfImages = $('.image', folder).size();
		if (debug) console.log(numberOfImages);
		if (numberOfImages > 0) {
			disable($('.delete_icon', folder)[0]);
		}

	},

	removeFileFromFolder : function(fileId, folderId) {

		var folder = $('.' + folderId + '_');
		var file = $('.' + fileId + '_', folder);
		files.append(file);//.animate();
		$('.delete_icon', file).remove();
		file.append('<img title="Delete file ' + fileId + '" '
			+ 'alt="Delete file ' + fileId + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
		$('.delete_icon', file).on('click', function() {
			_Files.deleteFile(this, Structr.entity(fileId));
		});
        
		file.draggable({
			revert: 'invalid',
			containment: '#main',
			zIndex: 1
		});

		var numberOfFiles = $('.file', folder).size();
		if (debug) console.log(numberOfFiles);
		if (numberOfFiles == 0) {
			enable($('.delete_icon', folder)[0]);
		}

		if (debug) console.log('removeFileFromFolder: fileId=' + fileId + ', folderId=' + folderId);
		_Entities.removeSourceFromTarget(fileId, folderId);
	},
    
	removeImageFromFolder : function(imageId, folderId) {

		var folder = $('.' + folderId + '_');
		var image = $('.' + imageId + '_', folder);
		images.append(image);//.animate();
		$('.delete_icon', image).remove();
		image.append('<img title="Delete image ' + imageId + '" '
			+ 'alt="Delete image ' + imageId + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
		$('.delete_icon', image).on('click', function() {
			_Files.deleteFile(this, Structr.entity(imageId));
		});

		image.draggable({
			revert: 'invalid',
			containment: '#main',
			zIndex: 1
		});

		var numberOfImages = $('.file', folder).size();
		if (debug) console.log(numberOfImages);
		if (numberOfImages == 0) {
			enable($('.delete_icon', folder)[0]);
		}

		if (debug) console.log('removeImageFromFolder: imageId=' + imageId + ', folderId=' + folderId);
		_Entities.removeSourceFromTarget(imageId, folderId);
	},
    
	deleteFolder : function(button, folder) {
		if (debug) console.log('delete folder ' + folder);
		deleteNode(button, folder);
	},

	deleteFile : function(button, file) {
		if (debug) console.log('delete file ' + file);
		deleteNode(button, file);
	},

	createFile : function(fileObj) {
		var entity = {};
		entity.contentType = fileObj.type;
		console.log(fileObj);
		entity.name = fileObj.name;
		entity.size = fileObj.size;
		entity.type = isImage(entity.contentType) ? 'Image' : 'File';
		_Entities.create(null, entity);

	},

	uploadFile : function(file) {

		if (debug) console.log(fileList);

		$(fileList).each(function(i, fileObj) {

			if (debug) console.log(file);

			if (fileObj.name == file.name) {
     
				if (debug) console.log(fileObj);
				if (debug) console.log('Uploading chunks for file ' + file.id);
                
				var reader = new FileReader();
				reader.readAsBinaryString(fileObj);
				//reader.readAsText(fileObj);

				var chunks = Math.ceil(fileObj.size / chunkSize);
				if (debug) console.log('file size: ' + fileObj.size + ', chunk size: ' + chunkSize + ', chunks: ' + chunks);

				// slicing is still unstable/browser dependent yet, see f.e. http://georgik.sinusgear.com/2011/05/06/html5-api-file-slice-changed/

				//                var blob;
				//                for (var c=0; c<chunks; c++) {
				//
				//                    var start = c*chunkSize;
				//                    var end = (c+1)*chunkSize-1;
				//
				//                    console.log('start: ' + start + ', end: ' + end);
				//
				//                    if (fileObj.webkitSlice) {
				//                        blob = fileObj.webkitSlice(start, end);
				//                    } else if (fileObj.mozSlice) {
				//                        blob = fileObj.mozSlice(start, end);
				//                    }
				//                    setTimeout(function() { reader.readAsText(blob)}, 1000);
				//                }

				reader.onload = function(f) {
                    
					if (debug) console.log('File was read into memory.');
					var binaryContent = f.target.result;
					//console.log(binaryContent);

					for (var c=0; c<chunks; c++) {
                        
						var start = c*chunkSize;
						var end = (c+1)*chunkSize;
                        
						var chunk = utf8_to_b64(binaryContent.substring(start,end));
						// TODO: check if we can send binary data directly

						var obj = {};
						obj.command = 'CHUNK';
						obj.id = file.id;
						var data = {};
						data.chunkId = c;
						data.chunkSize = chunkSize;
						data.chunk = chunk;
						obj.data = data;
						//var data = '{ "command" : "CHUNK" , "id" : "' + file.id + '" , "data" : { "chunkId" : ' + c + ' , "chunkSize" : ' + chunkSize + ' , "chunk" : "' + chunk + '" } }';
						sendObj(obj);

					}

					var iconSrc = $('.' + file.id + '_').find('.typeIcon').attr('src');
					console.log('Icon src: ', iconSrc);
					$('.' + file.id + '_').find('.typeIcon').attr('src', iconSrc + '?' + new Date().getTime());

				}
			}

		});

	}

};