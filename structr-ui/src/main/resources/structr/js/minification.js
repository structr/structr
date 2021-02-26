/*
 * Copyright (C) 2010-2021 Structr GmbH
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
var _Minification = {

	showMinificationDialog: function (file) {
		Structr.dialog('Minification', function() { }, function() { });

		dialogText.append('<table id="minification-files" class="props"><thead><tr><th>Position</th><th>Filename</th><th>Size</th><th>Actions</th></tr></thead><tbody></tbody></table>');
		dialogText.append('<div id="minification-source-search"></div>');

		var $minificationSourceSearch = $('#minification-source-search', dialogText);
		_Minification.displaySearch(file.type, file.id, "minificationSources", "AbstractFile", $minificationSourceSearch);

		_Minification.reloadHeadAndFiles(file.id);
	},

	updateHead: function (file) {

		dialogHead.empty();

		dialogHead.append('<table id="minify-options" class="props">'
				+ '<tr><td class="head">File</td><td><a href="' + file.path + '" target="_blank">' + file.name + '</a></td></tr>'
				+ '<tr><td class="head">Size</td><td>' + file.size + '</td></tr>'
				+ _Minification.getTypeSpecificRows(file)
				+ '<tr><td class="action" colspan=2><button title="Manually trigger minification"><i class="' + _Icons.getFullSpriteClass(_Icons.minification_trigger_icon) + '" /> Manually trigger minification</button></td></tr>'
				+ '</table>');

		if (file.type === 'MinifiedCssFile') {
			$('.lineBreak', dialogHead).on('blur', function () {
				var $el = $(this);
				var oldVal = parseInt($el.data('before'));
				var newVal = parseInt($el.val());

				if (oldVal !== newVal) {
					Command.setProperties(file.id, { lineBreak: newVal }, function (f) {
						$el.val(newVal).data('before', newVal);

						_Minification.reloadHeadAndFiles(file.id);
					});
				}
			});
		} else {
			$('.optimizationLevel', dialogHead).val(file.optimizationLevel).on('change', function () {
				var $el = $(this);
				var oldVal = $el.data('before');
				var newVal = $el.val();

				if (oldVal !== newVal) {
					Command.setProperties(file.id, { optimizationLevel: newVal }, function (f) {
						$el.val(newVal).data('before', newVal);

						_Minification.reloadHeadAndFiles(file.id);
					});
				}
			});
		}

		$('td.action button', dialogHead).on('click', function() {
			_Minification.minifyFile(file.id, function () {
				_Minification.reloadHeadAndFiles(file.id);
			});
		});

	},

	minifyFile: function (fileId, callback) {
		$.ajax({
			url: '/structr/rest/AbstractMinifiedFile/' + fileId + '/minify',
			type: 'POST',
			success: function () {
				if (typeof callback === 'function') {
					callback();
				}
			}
		});
	},

	reloadHeadAndFiles: function (fileId) {
		Command.get(fileId, 'id,type,path,size,name,minificationSources,lineBreak,optimizationLevel,errors,warnings', function (f) {
			_Minification.updateHead(f);

			$('#minification-files tbody', dialogText).empty();

			_Minification.printMinificationSourcesTable(f);

			_Files.resize();
		});
	},
	getTypeSpecificRows: function (file) {
		return (file.type === 'MinifiedCssFile') ? _Minification.getCssSpecificRows(file) : _Minification.getJavaScriptSpecificRows(file);
	},
	getCssSpecificRows: function (file) {
		return '<tr><td class="head">LineBreak</td><td><input class="lineBreak" data-target="lineBreak" data-before="' + file.lineBreak + '" value="' + file.lineBreak + '"></td></tr>';
	},
	getJavaScriptSpecificRows: function (file) {
		return '<tr><td class="head">Optimization Level</td><td><select class="optimizationLevel" data-target="optimizationLevel" data-before="' + file.optimizationLevel + '"><option>WHITESPACE_ONLY</option><option>SIMPLE_OPTIMIZATIONS</option><option>ADVANCED_OPTIMIZATIONS</option></select></td></tr>'
			+ '<tr><td class="head">Errors</td><td class="minification-scrollable-cell"><div class="scrollable-cell-content">' + (file.errors ? file.errors.replaceAll('\n', '<br>') : '') + '</div></td></tr>'
			+ '<tr><td class="head">Warnings</td><td class="minification-scrollable-cell"><div class="scrollable-cell-content">' + (file.warnings ? file.warnings.replaceAll('\n', '<br>') : '') + '</div></td></tr>';
	},
	printMinificationSourcesTable: function (file) {

		var $minificationTable = $('#minification-files tbody', dialogText);
		var maxPos = -1;

		$.ajax({
			url: '/structr/rest/AbstractMinifiedFile/' + file.id + '/out/all?relType=MINIFICATION&sort=position&order=asc',
			success: function (data) {
				var files = {};
				file.minificationSources.forEach(function (f) {
					files[f.id] = f;
				});

				data.result.sort(function (a, b) {
					return (a.position === b.position) ? 0 : (a.position < b.position) ? -1 : 1;
				});

				data.result.forEach(function (rel) {
					var f = files[rel.targetId];
					maxPos = Math.max(maxPos, rel.position);
					$minificationTable.append('<tr data-position=' + rel.position + '><td>' + rel.position + '</td><td>' + f.name + '</td><td>' + f.size + '</td><td><i title="Remove" data-rel-id="' + rel.id + '" class="remove-minification-source ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" /></td></tr>');
				});

				$('.remove-minification-source', $minificationTable).on('click', function () {
					var relId = $(this).data('relId');
					$.ajax({
						url: '/structr/rest/' + relId,
						type: 'DELETE',
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						success: function() {
							_Minification.minifyFile(file.id, function () {
								_Minification.reloadHeadAndFiles(file.id);
							});
						}
					});
				});

				$minificationTable.sortable({
					revert: true,
					helper: function(e, ui) {
						ui.children().each(function() {
							$(this).width($(this).width());
						});
						return ui;
					},
					update: function (e, ui) {
						var oldPos = ui.item.data('position');
						var newPos = ui.item.next().data('position');
						if (undefined === newPos) {
							newPos = maxPos;
						}

						_Minification.moveMinificationSource(file.id, oldPos, newPos, function () {
							_Minification.minifyFile(file.id, function () {
								_Minification.reloadHeadAndFiles(file.id);
							});
						});
					}
				});
			}
		});

	},
	moveMinificationSource: function (fileId, from, to, callback) {
		$.ajax({
			url: '/structr/rest/AbstractMinifiedFile/' + fileId + '/moveMinificationSource',
			type: 'POST',
			data: JSON.stringify({from: from, to: to}),
			success: function () {
				if (typeof callback === 'function') {
					callback();
				}
			}
		});
	},

	displaySearch: function(parentType, id, key, type, el) {
		el.append('<div class="searchBox searchBoxDialog"><input class="search" name="search" size="20" placeholder="Search"><i class="clearSearchIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
		var searchBox = $('.searchBoxDialog', el);
		var search = $('.search', searchBox);
		window.setTimeout(function() {
			search.focus();
		}, 250);

		search.keyup(function(e) {
			e.preventDefault();

			var searchString = $(this).val();
			if (searchString && searchString.length && e.keyCode === 13) {

				$('.clearSearchIcon', searchBox).show().on('click', function() {
					if (_Crud.clearSearchResults(el)) {
						$('.clearSearchIcon').hide().off('click');
						search.val('');
						search.focus();
					}
				});

				_Crud.search(searchString, el, type, function(e, node) {
					e.preventDefault();
					_Minification.addRelatedObject(parentType, id, key, node, function() {
						_Minification.minifyFile(id, function () {
							_Minification.reloadHeadAndFiles(id);
						});
					});
					return false;
				}, 1000, [id]);

				_Files.resize();

			} else if (e.keyCode === 27) {

				if (_Crud.clearSearchResults(el)) {
					$('.clearSearchIcon').hide().off('click');
					search.val('');
					search.focus();
				} else {
					search.val('');
				}
			}

			return false;
		});
	},

	addRelatedObject: function(type, id, key, relatedObj, callback) {
		var url = '/structr/rest/' + type + '/' + id + '/' + key + '/ui';
		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {
				var objects = _Crud.extractIds(data.result);
				if (!isIn(relatedObj.id, objects)) {
					objects.push({'id': relatedObj.id});
				}
				let body = {};
				body[key] = objects;
				_Crud.crudUpdateObj(id, JSON.stringify(body), function() {
					if (callback) {
						callback();
					}
				});
			}
		});
	}
};