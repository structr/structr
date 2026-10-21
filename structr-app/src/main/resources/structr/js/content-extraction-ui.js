/*
 * Copyright (C) 2010-2026 Structr GmbH
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
'use strict';

/**
 * UI of the content extraction module: a dialog that edits the ContentExtractionConfiguration of a folder
 * or file. It opens from the Files function bar (for the current folder) and from the context menu of a
 * folder or file.
 *
 * It also provides the content search of the Files search box (_Files.search.contentSearch), which finds
 * files by their extracted content and shows the matches in context.
 *
 * Only loaded while the content-extraction module is active (ContentExtractionModule.getUIExtensionScripts),
 * so files.js knows nothing about it: the function bar template, its status update, the context menu and
 * the content search are wrapped or replaced here.
 */
let _ContentExtraction = {

	type: 'ContentExtractionConfiguration',

	/** The options of a configuration, in dialog order. */
	options: [
		{ name: 'splitter',           label: 'Splitter',             kind: 'select',  comment: 'How extracted content is divided into chunks. structure follows headings, pages and tables; recursive splits at paragraphs, lines, sentences and words.' },
		{ name: 'chunkSize',          label: 'Chunk Size',           kind: 'number',  comment: 'Maximum chunk size, in the size unit.' },
		{ name: 'chunkOverlap',       label: 'Chunk Overlap',        kind: 'number',  comment: 'How much consecutive chunks overlap, in the size unit. Must be smaller than the chunk size.' },
		{ name: 'sizeUnit',           label: 'Size Unit',            kind: 'select',  comment: 'Unit of chunk size and overlap.' },
		{ name: 'tokenEncoding',      label: 'Token Encoding',       kind: 'select',  comment: 'Tokenizer used to count tokens. Exact for OpenAI models, an approximation for others.' },
		{ name: 'includeHeadingPath', label: 'Include Heading Path', kind: 'boolean', comment: 'Whether each chunk starts with its section path, e.g. "Installation > Requirements".' },
		{ name: 'separators',         label: 'Separators',           kind: 'lines',   comment: 'Separators of the recursive splitter, one per line, tried in order. Use \\n for a line break and "" to split anywhere.' },
		{ name: 'ocr',                label: 'OCR Mode',             kind: 'select',  comment: 'auto: images, and PDF pages without text. always: every image and page. off: no OCR.' },
		{ name: 'ocrLanguages',       label: 'OCR Languages',        kind: 'text',    comment: 'Tesseract languages joined with +, e.g. eng+deu.' },
		{ name: 'maxCharacters',      label: 'Max. Characters',      kind: 'number',  comment: 'Maximum number of characters extracted per document.' },
		{ name: 'includeEmbedded',    label: 'Include Embedded',     kind: 'boolean', comment: 'Whether attachments, archive entries and embedded documents are extracted as well.' },
		{ name: 'includeImages',      label: 'Include Images',       kind: 'boolean', comment: 'Whether images inside documents (inline images of PDFs, pictures in office documents and e-mails) are read with OCR.' }
	],

	init: () => {

		if (typeof _Files === 'undefined') {
			return;
		}

		let originalFunctions = _Files.templates.functions;
		_Files.templates.functions = (config) => _ContentExtraction.addFunctionBarButton(originalFunctions(config));

		let originalUpdateFunctionBarStatus = _Files.updateFunctionBarStatus;
		_Files.updateFunctionBarStatus = (...args) => {
			originalUpdateFunctionBarStatus(...args);
			_ContentExtraction.updateFunctionBarButton();
		};

		let originalContextMenuElements = _Files.getContextMenuElements;
		_Files.getContextMenuElements = (div, entity) => _ContentExtraction.addContextMenuElement(originalContextMenuElements(div, entity), entity);

		_Files.search.contentSearch = _ContentExtraction.searchFiles;

		// the function bar is rendered again whenever the Files area opens, so the click is delegated
		document.addEventListener('click', (e) => {
			if (e.target.closest('#content-extraction-dialog-button') && _Files.currentWorkingDir?.id) {
				_ContentExtraction.showDialog(_Files.currentWorkingDir.id);
			}
		});
	},
	addFunctionBarButton: (html) => {

		if (!StructrWS.isAdmin) {
			return html;
		}

		let container = document.createElement('div');
		container.innerHTML = html;

		let mountButton = container.querySelector('#mount-folder-dialog-button');
		if (mountButton) {
			mountButton.insertAdjacentHTML('afterend', _ContentExtraction.templates.functionBarButton());
		}

		return container.innerHTML;
	},
	updateFunctionBarButton: () => {

		let button = Structr.functionBar?.querySelector('#content-extraction-dialog-button');
		if (button) {

			// there is no folder node for the root folder, so it cannot be configured
			let disabled = _Files.helpers.isDisplayingFavorites() === true || !_Files.currentWorkingDir?.id;
			_Helpers.disableElements(disabled, button);
		}
	},
	addContextMenuElement: (elements, entity) => {

		if (!StructrWS.isAdmin || !(entity.isFolder || entity.isFile) || _Files.selection.getAllSelected().length > 1) {
			return elements;
		}

		let element = {
			icon: _Icons.getMenuSvgIcon(_Icons.iconSettingsCog),
			name: 'Content Extraction...',
			clickHandler: () => {
				_ContentExtraction.showDialog(entity.id);
			}
		};

		// right before the property dialogs, which follow the type-specific entries
		let index = elements.findIndex(el => el?.name === 'General');
		if (index >= 0) {
			elements.splice(index, 0, element, '|');
		} else {
			elements.push(element);
		}

		return elements;
	},
	searchFiles: async (searchString) => {

		let response = await fetch(`${Structr.rootUrl}ExtractedContentNode/searchFiles`, {
			method: 'POST',
			body: JSON.stringify({ query: searchString })
		});

		if (!response.ok) {
			return [];
		}

		let hits = (await response.json()).result ?? [];

		// the search returns id, name and path of each file; the result table needs the file in the ui view
		let files = await Promise.all(hits.map(hit => fetch(`${Structr.rootUrl}File/${hit.file.id}/ui`).then(r => r.ok ? r.json() : null).catch(() => null)));

		return hits.map((hit, i) => ({ file: files[i]?.result, snippets: hit.snippets ?? [] })).filter(hit => hit.file);
	},
	call: async (method, body) => {

		let response = await fetch(`${Structr.rootUrl}${_ContentExtraction.type}/${method}`, {
			method: 'POST',
			body: JSON.stringify(body)
		});

		let json = await response.json();

		if (!response.ok) {
			throw json;
		}

		return json.result;
	},
	showDialog: async (targetId) => {

		let data;

		try {
			data = await _ContentExtraction.call('describe', { target: targetId });
		} catch (error) {
			Structr.errorFromResponse(error);
			return;
		}

		let { dialogText } = _Dialogs.custom.openDialog('Content Extraction', null, ['show-dialog-title']);

		_ContentExtraction.renderDialog(dialogText, data);
	},
	renderDialog: (dialogText, data) => {

		_Helpers.fastRemoveAllChildren(dialogText);
		_Dialogs.custom.elements.dialogBtn?.querySelectorAll('[data-content-extraction-button]').forEach(button => button.remove());

		dialogText.insertAdjacentHTML('beforeend', _ContentExtraction.templates.dialog(data));
		_Helpers.activateCommentsInElement(dialogText);

		let form   = dialogText.querySelector('#content-extraction-form');
		let exists = !!data.configuration;

		if (data.canEdit) {

			if (exists) {

				let removeButton = _Dialogs.custom.prependCustomDialogButton(`<button data-content-extraction-button class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Remove Configuration</button>`);

				removeButton.addEventListener('click', async () => {

					let what = data.target.isFolder ? 'this folder and its subfolders' : 'this file';
					if (await _Dialogs.confirmation.showPromise(`Remove the content extraction configuration of ${_Helpers.escapeTags(data.target.path)}?<br><br>${_Helpers.escapeTags(what)} will then use ${data.inherited ? 'the configuration of ' + _Helpers.escapeTags(data.inherited.path) : 'no configuration and are not extracted automatically'}. Existing chunks are kept.`)) {
						await _ContentExtraction.submit(dialogText, 'remove', { target: data.target.id });
					}
				});
			}

			_Dialogs.custom.prependCustomDialogButton(`<button data-content-extraction-button form="content-extraction-form" class="action hover:bg-gray-100 focus:border-gray-666 active:border-green">${exists ? 'Save' : 'Create'}</button>`);

		} else {

			for (let element of form.querySelectorAll('input, select, textarea')) {
				element.disabled = true;
			}
		}

		form.addEventListener('submit', async (e) => {

			e.preventDefault();

			await _ContentExtraction.submit(dialogText, 'save', {
				target:  data.target.id,
				values:  _ContentExtraction.readValues(form),
				process: form.querySelector('[name="process"]').checked
			});
		});
	},
	submit: async (dialogText, method, body) => {

		try {

			let data = await _ContentExtraction.call(method, body);

			_ContentExtraction.renderDialog(dialogText, data);

			if (body.process) {
				new InfoMessage().text('Content extraction started, its progress is shown in the job queue.').show();
			}

			_Dialogs.custom.showAndHideInfoBoxMessage(method === 'remove' ? 'Configuration removed.' : 'Configuration saved.', 'success', 2000, 200);

		} catch (error) {

			Structr.errorFromResponse(error);
		}
	},
	readValues: (form) => {

		let values = {
			enabled: form.querySelector('[name="enabled"]').checked
		};

		for (let option of _ContentExtraction.options) {

			let element = form.querySelector(`[name="${option.name}"]`);
			let value   = element.value.trim();

			switch (option.kind) {

				case 'number':
					values[option.name] = (value === '') ? '' : parseInt(value, 10);
					break;

				case 'boolean':
					values[option.name] = (value === '') ? '' : (value === 'true');
					break;

				case 'lines':
					values[option.name] = _ContentExtraction.parseSeparators(element.value);
					break;

				default:
					values[option.name] = value;
			}
		}

		return values;
	},
	/** One separator per line; escapes for invisible characters, "" for the empty separator. */
	parseSeparators: (text) => {

		return text.split('\n').filter(line => line !== '').map(line => {

			if (line === '""') {
				return '';
			}

			return line.replace(/\\(n|t|r|\\)/g, (match, c) => ({ n: '\n', t: '\t', r: '\r', '\\': '\\' })[c]);
		});
	},
	formatSeparators: (separators) => {

		return (separators ?? []).map(separator => {

			if (separator === '') {
				return '""';
			}

			return separator.replace(/[\\\n\t\r]/g, c => ({ '\\': '\\\\', '\n': '\\n', '\t': '\\t', '\r': '\\r' })[c]);
		}).join('\n');
	},
	formatDefault: (value) => {

		if (value === null || value === undefined) {
			return 'none';
		}

		if (typeof value === 'boolean') {
			return value ? 'yes' : 'no';
		}

		return String(value);
	},
	templates: {
		functionBarButton: () => `
			<button class="button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green" id="content-extraction-dialog-button" title="Configure automatic content extraction for the current folder">
				${_Icons.getSvgIcon(_Icons.iconSettingsCog, 16, 16, ['mr-2'])}
				Content Extraction
			</button>
		`,
		dialog: (data) => {

			let own      = data.configuration;
			let values   = own?.options ?? {};
			let isFolder = data.target.isFolder;
			let scope    = isFolder ? 'files in this folder and all its subfolders' : 'this file';

			return `
				<form id="content-extraction-form">
					<table class="props">
						<tbody>
							<tr>
								<td>${isFolder ? 'Folder' : 'File'}</td>
								<td class="font-mono">${_Helpers.escapeTags(data.target.path)}</td>
							</tr>
							<tr>
								<td>Configuration</td>
								<td>${_ContentExtraction.templates.configurationState(data)}</td>
							</tr>
							<tr>
								<td>Automatic Extraction</td>
								<td>${data.decision.extract ? 'On' : 'Off'} for ${scope}</td>
							</tr>
							${data.state ? _ContentExtraction.templates.fileState(data.state) : ''}
							${(data.warnings ?? []).length ? `<tr><td>Warnings</td><td>${data.warnings.map(w => _Helpers.escapeTags(w)).join('<br>')}</td></tr>` : ''}
						</tbody>
						<thead>
							<tr>
								<th colspan="2">${own ? 'Configuration' : 'New Configuration'}</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td data-comment="Whether ${scope} are extracted automatically when content is uploaded or moved in. A configuration with this switched off excludes ${isFolder ? 'a subtree' : 'a file'} from the configuration of a folder above.">Extract Automatically</td>
								<td><input type="checkbox" name="enabled" ${(own ? own.enabled : true) ? 'checked' : ''}></td>
							</tr>
							${_ContentExtraction.options.map(option => _ContentExtraction.templates.option(option, values[option.name], data)).join('')}
							<tr>
								<td data-comment="Extracts the files this configuration covers after saving. Files whose content and options are unchanged are skipped. Without this, existing files are only extracted when their content changes or when processContent() is called.">Process Existing Files</td>
								<td><input type="checkbox" name="process"></td>
							</tr>
						</tbody>
					</table>
					<p class="mt-4 text-sm">Empty values use the global <span class="font-mono">contentextraction.*</span> settings, shown as default. The nearest configuration on a file's path applies completely: values it leaves empty are not taken from configurations further up.</p>
					${data.canEdit ? '' : '<p class="mt-2 text-sm">Only admins can change content extraction configurations.</p>'}
				</form>
			`;
		},
		configurationState: (data) => {

			if (data.configuration) {
				return `Own configuration`;
			}

			if (data.inherited) {
				return `Inherited from <span class="font-mono">${_Helpers.escapeTags(data.inherited.path)}</span> (extraction ${data.inherited.enabled ? 'on' : 'off'}). A configuration created here replaces it completely.`;
			}

			return 'None. Files are not extracted automatically.';
		},
		fileState: (state) => `
			<tr>
				<td>Extraction Status</td>
				<td>${_Helpers.escapeTags(state.status ?? 'never extracted')}${state.date ? ', ' + _Helpers.escapeTags(new Date(state.date).toLocaleString()) : ''}, ${state.chunkCount} chunk${state.chunkCount === 1 ? '' : 's'}${state.ocrUsed ? ', with OCR' : ''}</td>
			</tr>
			${state.error ? `<tr><td>Last Error</td><td>${_Helpers.escapeTags(state.error)}</td></tr>` : ''}
		`,
		option: (option, value, data) => {

			let defaultValue = _ContentExtraction.formatDefault(data.defaults[option.name]);
			let comment      = _Helpers.escapeForHtmlAttributes(option.comment);
			let input;

			switch (option.kind) {

				case 'select':
					input = `
						<select name="${option.name}" class="box-border w-full">
							<option value="">Default (${_Helpers.escapeTags(defaultValue)})</option>
							${(data.choices[option.name] ?? []).map(choice => `<option value="${choice}" ${choice === value ? 'selected' : ''}>${choice}</option>`).join('')}
						</select>
					`;
					break;

				case 'boolean':
					input = `
						<select name="${option.name}" class="box-border w-full">
							<option value="">Default (${defaultValue})</option>
							<option value="true" ${value === true ? 'selected' : ''}>yes</option>
							<option value="false" ${value === false ? 'selected' : ''}>no</option>
						</select>
					`;
					break;

				case 'number': {
					let limits = data.limits[option.name];
					input = `<input type="number" name="${option.name}" class="box-border w-full" min="${limits?.min ?? (option.name === 'chunkOverlap' ? 0 : 1)}" ${limits?.max ? `max="${limits.max}"` : ''} placeholder="Default: ${_Helpers.escapeForHtmlAttributes(defaultValue)}" value="${value ?? ''}">`;
					break;
				}

				case 'lines':
					input = `<textarea name="${option.name}" class="box-border w-full font-mono" rows="3" placeholder="Default: blank lines, lines, sentences, words">${_Helpers.escapeTags(_ContentExtraction.formatSeparators(value))}</textarea>`;
					break;

				default: {
					let extra = (option.name === 'ocrLanguages') ? `Installed: ${data.ocr.available ? (data.ocr.languages.join(', ') || 'none') : 'Tesseract is not installed'}` : '';
					input = `<input type="text" name="${option.name}" class="box-border w-full" placeholder="Default: ${_Helpers.escapeForHtmlAttributes(defaultValue)}" value="${_Helpers.escapeForHtmlAttributes(value ?? '')}">${extra ? `<div class="text-sm mt-1">${_Helpers.escapeTags(extra)}</div>` : ''}`;
				}
			}

			return `
				<tr>
					<td data-comment="${comment}">${option.label}</td>
					<td>${input}</td>
				</tr>
			`;
		}
	}
};

_ContentExtraction.init();
