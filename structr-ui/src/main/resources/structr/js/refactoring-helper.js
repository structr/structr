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
	
class RefactoringHelper {

	constructor(container) {
		this.container = container;
	}

	show() {
		
		this.container.append('<p>This helper allows you to find and edit HTML elements in Structr pages and Shared Components based on a type and some common attributes.</p>');
		this.container.append('<div id="select-container"></div>');

		var selectContainer = $('#select-container');
		
		selectContainer.append('<input class="refactoring-helper" id="selector-input" placeholder="Enter selector" /><input class="refactoring-helper" id="property-input" placeholder="Enter property keys to display" />');
		selectContainer.append('<div id="result-container"></div>');
		selectContainer.append('<div><pre id="error-container"></pre></div>');

		var typeSelector    = $('#selector-input');
		var keysSelector    = $('#property-input');
		var resultContainer = $('#result-container');
		var errorContainer  = $('#error-container');

		window.setTimeout(() => { $(typeSelector).focus(); }, 100);

		keysSelector.on('blur', () => {

			var typeQuery  = typeSelector.val().trim();
			var properties = keysSelector.val().trim();

			if (typeQuery && properties) {

				typeQuery = typeQuery[0].toUpperCase() + typeQuery.slice(1);

				resultContainer.empty();
				errorContainer.empty();
	
				$.ajax({
					url: '/structr/rest/' + typeQuery + '/all',
					method: 'get',
					statusCode: {
						200: (response) => {

							if (response && response.result && response.result.length) {
								
								resultContainer.append('<table class="refactoring-helper" id="result-table"><thead><tr id="header-row"></tr></thead></table>');
								var keyCandidates = Object.keys(response.result[0]);
								var table         = $('#result-table');
								var header        = $('#header-row');
								var list          = properties.split(',');
								var keys          = [];
								var i             = 0;

								list.forEach(p => {
									
									var name = p.trim();
									
									if (keyCandidates.includes(name) && name !== 'id' && name !== 'type') {
										keys.push({ name: name, title: name });
									}

									if (keyCandidates.includes('_html_' + name)) {
										keys.push({ name: '_html_' + name, title: 'HTML ' + name });
									}

									if (keyCandidates.includes('_custom_html_' + name)) {
										keys.push({ name: '_custom_html_' + name, title: 'HTML ' + name });
									}

									if (keyCandidates.includes('_custom_html_data-' + name)) {
										keys.push({ name: '_custom_html_data-' + name, title: 'data-' + name });
									}
								});
									
								header.append('<th>Page</th>');

								keys.forEach(k => {
									header.append('<th>' + k.title + '</th>');
								});

								response.result.forEach(v => {
									table.append('<tr id="row' + i + '"></tr>');
									var row = $('#row' + i);
									row.append('<td>' + v.ownerDocument.name + '</td>');
									keys.forEach(k => {
										var value = '';
										var id    = 'edit-' + v.id + '-' + k.name;
										if (v[k.name]) {
											value = v[k.name];
										}
										row.append('<td><input type="text" id="' + id + '" value="' + value + '"/></td>');
										var input = $('#' + id);
										input.on('blur', e => {
											var val = $('#' + id).val();
											Command.setProperty(v.id, k.name, val, false, function() {
												blinkGreen(input);
											});
										});
									});

									i++;
								});
							}
						},
						422:(response) =>{
							errorContainer.append(response.responseText);
						}
					}
				});
			}

		});
	
		


	}
}