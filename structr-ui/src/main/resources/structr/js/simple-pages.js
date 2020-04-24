/*
 * Copyright (C) 2010-2020 Structr GmbH
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
document.addEventListener('DOMContentLoaded', function() {
	Structr.registerModule(_SimplePages);
	Structr.classes.push('');
});

var _SimplePages = {

	_moduleName: 'simple-pages',
	
	resizerLeftKey:       'structrSimplePagesResizerLeftKey_' + port,
	resizerRightKey:      'structrSimplePagesResizerRightKey_' + port,
	currentPageIdKey:     'structrSimplePagesCurrentPageIdKey_' + port,
	currentTemplateIdKey: 'structrSimplePagesCurrentTemplateIdKey_' + port,
	currentPreviewURLKey: 'structrSimplePagesCurrentPreviewURLKey_' + port,
	previewURLListKey:    'structrSimplePagesPreviewURLListKeyKey_' + port,

	components: {},
	objectCache: {},
	activeElement: {},
	activeTemplate: null,
	styles: [],
	previewURLList: [],

	onload: function() {

		Structr.fetchHtmlTemplate('simple-pages/main', {}, function(html) {

			main.append(html);

			_SimplePages.components.main            = document.querySelector('#simple-pages-main');
			
			_SimplePages.components.tree            = document.querySelector('#simple-pages-tree');
			_SimplePages.components.contents        = document.querySelector('#simple-pages-contents');
			_SimplePages.components.previewUrlInput = document.querySelector('#preview-url-input');
			_SimplePages.components.previewIframe   = document.querySelector('iframe#preview');

			_SimplePages.components.context         = document.querySelector('#simple-pages-context');
			
			_SimplePages.moveLeftResizer();
			Structr.initVerticalSlider(document.querySelector('.column-resizer-left',  _SimplePages.components.main), _SimplePages.resizerLeftKey,  204, _SimplePages.moveLeftResizer);
			Structr.initVerticalSlider(document.querySelector('.column-resizer-right', _SimplePages.components.main), _SimplePages.resizerRightKey, 204, _SimplePages.moveRightResizer);

			_SimplePages.resize();

			window.removeEventListener('resize', null, false);
			window.addEventListener('resize', function() {
				_SimplePages.resize();
			});

			Structr.unblockMenu(500);

			_SimplePages.init();

		});

	},
	init: function() {

		_SimplePages.previewURLList = LSWrapper.getItem(_SimplePages.previewURLListKey) || [];
		_SimplePages.activeTemplate = LSWrapper.getItem(_SimplePages.currentTemplateIdKey);
		
		//LSWrapper.removeItem(_SimplePages.previewURLListKey);

		_SimplePages.refreshTreeArea();

		// clear object cache
		_SimplePages.objectCache = {};
		_SimplePages.templateCodeEditor;
				
		fastRemoveAllChildren(_SimplePages.components.context);

		Structr.fetchHtmlTemplate('simple-pages/template-code-preview', {}, function(html) {

			_SimplePages.components.context.insertAdjacentHTML('afterbegin', html);
			_SimplePages.components.codeArea = _SimplePages.components.context.querySelector('.code-preview');
			
			let contentType = 'text/html';
			CodeMirror.defineMIME('text/html', 'htmlmixed-structr');
			_SimplePages.templateCodeEditor = CodeMirror(_SimplePages.components.codeArea, Structr.getCodeMirrorSettings({
				//value: unescapeTags(el.innerText),
				mode: contentType,
				lineNumbers: true,
				lineWrapping: false,
				indentUnit: 4,
				tabSize: 4,
				indentWithTabs: true,
				styleSelectedText: true
			}));
			
		});

		live('.delete_icon', 'click', function(e) {
			let el = e.target;
			let parentEl = el.closest('div.node');
			if (parentEl) {
				let id = parentEl.getAttribute('id').substring(3);
				_Entities.deleteNode(el, { id: id,	name: parentEl.querySelector('b').innerText,	type: 'Page' }, true, function() {
					_SimplePages.refreshTreeArea();
				});
			}
		});

		live('.edit_props_icon', 'click', function(e) {
			let el = e.target;
			let parentEl = el.closest('div.node');
			if (parentEl) {
				let id = parentEl.getAttribute('id').substring(3);
				_Entities.showProperties({ id: id,	name: parentEl.querySelector('b').innerText,	type: 'Page' }, false);
			}
		});

		live('.key_icon', 'click', function(e) {
			let el = e.target;
			let parentEl = el.closest('div.node');
			if (parentEl) {
				let id = parentEl.getAttribute('id').substring(3);
				_Entities.showAccessControlDialog({ id: id,	name: parentEl.querySelector('b').innerText,	type: 'Page' });
			}
		});
		
		on('#preview-url-input', 'keyup', function(e) {
			if (e.keyCode === 13) {
				let el = e.target;
				let url = el.value;
				_SimplePages.showPreview(url);
				LSWrapper.setItem(_SimplePages.currentPreviewURLKey, url);
			}
		});
		
		live('.save-template', 'click', function(e) {
			
			let name        = document.querySelector('.save-template-form input[name="name"]').value;
			
			if (!name) {
				alert('You must enter a template name.');
				return;
			}
			
			/* TODO:
			1. Get currently active template
			2. Replace HTML code with ${include('Template Name')}
			3. DONE - Make active template parent of the new template
			4. DONE - Make active template parent of new template
			*/

			Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
				let shadowPage = entities[0];
				let name        = document.querySelector('.save-template-form input[name="name"]').value;
				let selector    = document.querySelector('.save-template-form input[name="selector"]').value;
				let pageContent = document.querySelector('.save-template-form input[name="page-content"]').value;
				let content = _SimplePages.templateCodeEditor.getValue();
				
				Command.search(name, 'Template', true, function(result) {

					if (result && result.length) {
						
						let existingTemplate = result[0];
						
						Command.setProperties(existingTemplate.id, {
							selector: selector,
							content: content,
							pageContent: pageContent
						}, function() {
							if (_SimplePages.activeTemplate) {
								_SimplePages.loadTemplate(_SimplePages.activeTemplate.id);
							}
						});
						
					} else {
					
						//console.log('Create Template', name, content, selector, shadowPage.id, _SimplePages.activeTemplate.id);
					
						Command.create({
							type: 'Template',
							contentType: 'text/html',
							name: name || null,
							content: content,
							selector: selector,
							pageContent: pageContent,
							pageId: shadowPage.id,
							parentId: _SimplePages.activeTemplate ? _SimplePages.activeTemplate.id : null
						}, function(result) {
							
							_SimplePages.replaceCodeByIncludeStatement(_SimplePages.activeTemplate, name, selector, content);
							
							_SimplePages.refreshTreeArea();
							if (_SimplePages.activeTemplate) {
								_SimplePages.loadTemplate(_SimplePages.activeTemplate.id);
							}
						});
					}
				});
				
			});
			
		});

		live('.node.content', 'click', function(e) {
			e.preventDefault();
			e.stopPropagation();

			let el = e.target.closest('.node');
			
			let id = el.id.substring(3);

			LSWrapper.setItem(_SimplePages.currentTemplateIdKey, id);
			
			_SimplePages.components.tree.querySelectorAll('.node').forEach(function(childNode) {
				childNode.classList.remove('selected');
			});
			el.classList.add('selected');
			_SimplePages.loadTemplate(id);
			return false;
		});

		
		//let url = LSWrapper.getItem(_SimplePages.currentPreviewURLKey);
		//if (url) {
		//	_SimplePages.showPreview(url);
		//}

	},
	resize: function() {

		let windowHeight = window.innerHeight;
		let headerOffsetHeight = document.querySelector('#header').offsetHeight + 40;

		if (_SimplePages.components.tree) {
			_SimplePages.components.tree.style.height = windowHeight - headerOffsetHeight - 27 + 'px';
		}

		if (_SimplePages.components.contents) {
			_SimplePages.components.contents.style.height = windowHeight - headerOffsetHeight - 43 + 'px';
		}

		if (_SimplePages.components.context) {
			_SimplePages.components.context.style.height = windowHeight - headerOffsetHeight - 11 + 'px';
		}

		if (_SimplePages.components.previewIframe) {
			_SimplePages.components.previewIframe.style.height = windowHeight - headerOffsetHeight - 40 + 'px';
		}

		if (_SimplePages.components.codeArea) {
			_SimplePages.components.codeArea.style.height = windowHeight - headerOffsetHeight - 48 + 'px';
		}

		if (document.querySelector('.CodeMirror')) {
			document.querySelector('.CodeMirror').style.height = _SimplePages.components.context.offsetHeight - headerOffsetHeight + 'px';
		}

		_SimplePages.moveLeftResizer();
		_SimplePages.moveRightResizer();

		Structr.resize();

	},
	moveLeftResizer: function(left) {
		left = left || LSWrapper.getItem(_SimplePages.resizerLeftKey) || 300;
		_SimplePages.components.main.querySelector('.column-resizer-left').style.left = left + 'px';

		var contextWidth  = _SimplePages.components.context.offsetWidth;

		_SimplePages.components.tree.style.width = left - 14 + 'px';
		_SimplePages.components.contents.style.left = left + 8 + 'px';
		_SimplePages.components.contents.style.width = window.innerWidth - left - contextWidth - 56 + 'px';
		_SimplePages.components.context.style.left = left + window.innerWidth - left - contextWidth - 14 + 'px';
		_SimplePages.components.previewUrlInput.style.width = _SimplePages.components.contents.offsetWidth - 100 + 'px';
	},
	moveRightResizer: function(left) {
		left = left || LSWrapper.getItem(_SimplePages.resizerRightKey) || window.innerWidth - 300;
		_SimplePages.components.main.querySelector('.column-resizer-right').style.left = left + 'px';

		var treeWidth     = _SimplePages.components.tree.offsetWidth;

		_SimplePages.components.contents.style.width = left - treeWidth - 46 + 'px';
		_SimplePages.components.context.style.left = left + 8 + 'px';
		_SimplePages.components.context.style.width = window.innerWidth - left - 48 + 'px';
		_SimplePages.components.previewUrlInput.style.width = _SimplePages.components.contents.offsetWidth - 100 + 'px';
	},	
	replaceCodeByIncludeStatement: function(activeTemplate, name, selector, content) {
	
		console.log('replaceCodeByIncludeStatement:', activeTemplate, name, selector, content);
	
		// TODO: Decide what to do if no active template is given (auto-create one?)
		if (!activeTemplate) return;
	
		let parser = new DOMParser();
		let doc = parser.parseFromString(activeTemplate.pageContent, 'text/html');

		if (selector) {
			doc.querySelector(selector).outerHTML = '${include(\'' + name + '\')}';
		}
	
		let html;
		if (doc.doctype) {
			html = '<!DOCTYPE ' + doc.doctype.name + '>\n' + doc.documentElement.outerHTML;
			activeTemplate.pageContent = html;
		} else {
			html = doc.querySelector(activeTemplate.selector).outerHTML;
		}
		
		console.log(selector, html, activeTemplate);
		
		Command.setProperties(activeTemplate.id, {
			selector: selector,
			content: html
			//pageContent: activeTemplate.pageContent
		}, function() {
			if (_SimplePages.activeTemplate) {
				_SimplePages.loadTemplate(_SimplePages.activeTemplate.id);
			}
		});
	
	},
//	refreshPageList: function() {
//		fastRemoveAllChildren(_SimplePages.components.tree);
//		_SimplePages.listPages();
//	},
	refreshTreeArea: function() {
		_SimplePages.listPreviewURLs();
		_SimplePages.listTemplates();
	},
	showAddPageTemplateButton: function(callback) {
		Structr.fetchHtmlTemplate('simple-pages/add-page-template-button', null, function(html) {
			_SimplePages.components.pageRoot.insertAdjacentHTML('beforeEnd', html);
			_SimplePages.components.addPageTemplateButton = document.querySelector('.add-page-template');
			if (callback) callback();
		});
	},
	hideAddPageTemplateButton: function() {
		let el = querySelector('.add-page-template');
		el.parentNode.removeChild(el);
	},
	listPages: function() {

		fastRemoveAllChildren(_SimplePages.components.tree);
		
		Structr.fetchHtmlTemplate('simple-pages/create-page-button', {}, function(html) {

			_SimplePages.components.tree.insertAdjacentHTML('beforeEnd', html);
			_SimplePages.components.tree.querySelector('.create-page-button').addEventListener('click', function() {
				Command.create({type: 'Page'}, function() {
					_SimplePages.refreshPageList();
				});
			});
		});
		
		Command.query('Page', 1000, 1, 'name', 'desc', { hidden: false }, function(pages) {

			pages.forEach(function(page) {

				Structr.fetchHtmlTemplate('simple-pages/page-list-node', { page: page }, function(html) {

					_SimplePages.components.tree.insertAdjacentHTML('beforeEnd', html);
					
					on('#id_' + page.id, 'mouseenter', function(e) {
						let el = e.target;
						el.querySelectorAll('.button').forEach(function(childNode) { childNode.style.display = 'inline-block'; });
					});

					on('#id_' + page.id, 'mouseleave', function(e) {
						let el = e.target;
						el.querySelectorAll('.button').forEach(function(childNode) { childNode.style.display = 'none'; });
					});

					on('#id_' + page.id, 'click', function(e) {
						let el = e.target;
						e.preventDefault();
						e.stopPropagation();
						LSWrapper.setItem(_SimplePages.currentPageIdKey, page.id);
						_SimplePages.components.tree.querySelectorAll('.page').forEach(function(childNode) {
							childNode.classList.remove('selected');
						});
						el.classList.add('selected');
						_SimplePages.loadPage(page.id);
						return false;
					});

					let currentPageId = LSWrapper.getItem(_SimplePages.currentPageIdKey);
					let previewUrl   = LSWrapper.getItem(_SimplePages.currentPreviewURLKey);
					if (currentPageId === page.id && !previewUrl) {
						document.querySelector('#id_' + currentPageId).click();
					}
				});
			});

		});
		
	},
	loadPage: function(pageId) {
		
		Command.get(pageId, null, function(page) {
			_SimplePages.showPreview('/structr/html/' + page.id);
		});				
	},
	storePreviewURL: function(url) {
		if (_SimplePages.previewURLList.indexOf(url) === -1) {
			_SimplePages.previewURLList.push(url);
			LSWrapper.setItem(_SimplePages.previewURLListKey, _SimplePages.previewURLList);
			_SimplePages.listPreviewURLs();
		}
	},
	removeFromPreviewURLList: function(url) {
		const index = _SimplePages.previewURLList.indexOf(url);
		if (index > -1) {
			_SimplePages.previewURLList.splice(index, 1);
			LSWrapper.setItem(_SimplePages.previewURLListKey, _SimplePages.previewURLList);
		}
	},
	showPreview: function(url, templateName, content, selector) {
		
		let previewUrlInput = document.querySelector('#preview-url-input');
		if (url && url.startsWith('http')) {
			previewUrlInput.value = url;
			_SimplePages.storePreviewURL(url);
			url = '/structr/proxy?url=' + encodeURIComponent(url);
			_SimplePages.activateElement(null, url);
			_SimplePages.activeTemplate = null;
		} else if (templateName) {
			previewUrlInput.value = templateName;
			document.querySelector('.save-template-form input[name="name"]').value = templateName;
		}
		
		if (content) {
			_SimplePages.templateCodeEditor.setValue(content);
		}
		
		Structr.fetchHtmlTemplate('simple-pages/page-root', { url: url }, function(html) {

			fastRemoveAllChildren(_SimplePages.components.contents);
			_SimplePages.components.contents.insertAdjacentHTML('beforeEnd', html);
			_SimplePages.components.pageRoot = document.querySelector('.page-root');
			
			_SimplePages.components.previewIframe = document.querySelector('iframe#preview');

			if (page && page.children && page.children.length === 0) {

				// page has no child element => show add icon
				_SimplePages.showAddPageTemplateButton(function() {

					_SimplePages.components.addPageTemplateButton.addEventListener('click', function() {

						_SimplePages.addPageTemplate(page, function() {
							dialogCancelButton.click();
							_SimplePages.hideAddPageTemplateButton();
							window.setTimeout(function() {
								_SimplePages.refreshCurrentPage();
							}, 250);
						});
					});
				});
			}
			
			_SimplePages.resize();
			_SimplePages.activatePreviewIframe(url, selector, content);
			
		});		
	},
	activatePreviewIframe: function(url, selector, content) {
		
		_SimplePages.components.previewIframe.addEventListener('load', function(e) {

			try {
				let doc = e.target.contentDocument; // this.contentDocument;
				let head = doc.querySelector('head');
				if (head) {
					//head.insertAdjacentHTML('beforeEnd', '<link rel="stylesheet" type="text/css" media="screen" href="/structr/css/sprites.css">');
					//head.insertAdjacentHTML('beforeEnd', '<link rel="stylesheet" type="text/css" media="screen" href="/structr/css/simple-pages.css">');
				}

				live('.delete_icon', 'click', function(e) {
					let el = e.target;
					let id = el.closest('[data-structr-id]').getAttribute('data-structr-id');
					Command.removeChild(id, function() {
						_SimplePages.refreshCurrentPage();
					});

				}, doc);

				live('.edit_props_icon', 'click', function(e) {
					let el = e.target;
					let id = el.closest('[data-structr-id]').getAttribute('data-structr-id');
					//console.log('edit_props_icon clicked', id);
					Command.get(id, null, function(t) {
						_Entities.showProperties(t);
					});
				}, doc);

				live('.replace_icon', 'click', function(e) {
					let el = e.target;
					let id = el.closest('[data-structr-id]').getAttribute('data-structr-id');
					Command.get(id, null, function(t) {
						_SimplePages.replaceTemplate(t);
					});
				}, doc);

				live('.add_icon', 'click', function(e) {
					let el = e.target;
					let id = el.closest('[data-structr-id]').getAttribute('data-structr-id');
					Command.get(id, null, function(t) {
						_SimplePages.addTemplate(t, function() {
							dialogCancelButton.click();
							_SimplePages.refreshCurrentPage();
						});
					});
				}, doc);

				let x,y;

				_SimplePages.resize();

				addEvent(doc, 'click', function(e) {
					e.preventDefault();
					e.stopPropagation();
					_SimplePages.activateElement(e.target, url);
				});

				addEvent(doc, 'mouseover', function(e) {
					let el = e.target;

					let tag = el.tagName.toLowerCase();
					if (tag === 'html') return;

					let style = window.getComputedStyle(el);

					_SimplePages.saveStyle(el);

					el.style.marginTop    = parseFloat(style.marginTop || 0)    + parseFloat(style.borderTopWidth || 0)    - 1 + 'px';
					el.style.marginRight  = parseFloat(style.marginRight || 0)  + parseFloat(style.borderRightWidth || 0)  - 1 + 'px';
					el.style.marginBottom = parseFloat(style.marginBottom || 0) + parseFloat(style.borderBottomWidth || 0) - 1 + 'px';
					el.style.marginLeft   = parseFloat(style.marginLeft || 0)   + parseFloat(style.borderLeftWidth || 0)   - 1 + 'px';

					el.style.border   = '1px dashed orange';
					el.style.boxShadow = '0 0 0 99999px rgba(0, 0, 0, .4)';
					el.style.position  = 'relative';
					el.style.zIndex    = 99999;

					let id      = el.getAttribute('id');
					let classes = el.classList;
					document.querySelector('body').insertAdjacentHTML('beforeend', '<div id="element-preview-overlay">&lt;' + tag + (id ? ' id="' + id + '"' : '') + (classes.length ? ' class="' + classes + '"' : '') + '&gt;</div>');
					let elementPreviewOverlay = document.querySelector('body #element-preview-overlay');

					elementPreviewOverlay.style.left = offset(el).left + _SimplePages.components.tree.offsetWidth + 32 + 'px';
					elementPreviewOverlay.style.top  = offset(el).top  + document.querySelector('#header').offsetHeight - 24 + 'px';

				});

				addEvent(doc, 'mouseout', function(e) {

					let overlay = document.querySelector('body #element-preview-overlay');
					if (overlay) {
						overlay.parentNode.removeChild(overlay);
					}

					let el = e.target;

					if (el === _SimplePages.activeElement) {
						el.style.border = '1px dashed green';
						el.style.boxShadow = '0 0 0 99999px rgba(0, 0, 0, .8)';
						el.style.position  = 'relative';
						el.style.zIndex    = 99999;
					} else {
						_SimplePages.restoreSavedStyle(el);
					}

				});

				let templates = doc.querySelectorAll('[data-structr-id]');
				templates.forEach(function(templateElement) {

					let id = templateElement.getAttribute('data-structr-id');

					live('[data-structr-id="' + id + '"]', 'mouseenter', function(e) {
						//e.preventDefault();
						//e.stopPropagation();
						let templateElement = this;

						let children = doc.querySelectorAll('.edit-header');
						children.forEach(function(childNode) {
							childNode.style.zIndex = '9999999';
							//childNode.style.position = 'absolute';
						});
						templateElement.setAttribute('x-enter', x);
						templateElement.setAttribute('y-enter', y);
						templateElement.classList.add('hover');
						x = e.screenX, y = e.screenY;
					}, doc);

					live('[data-structr-id="' + id + '"]', 'mouseleave', function(e) {
						e.preventDefault();
						e.stopPropagation();

						let templateElement = this;
						let xEnter = parseInt(templateElement.getAttribute('x-enter'));
						let yEnter = parseInt(templateElement.getAttribute('y-enter'));
						let distance = Math.sqrt(Math.pow(e.screenX - xEnter, 2) + Math.pow(e.screenY - yEnter, 2));
						if (distance > 15) {

							templateElement.removeAttribute('x-enter');
							templateElement.removeAttribute('y-enter');

							let children = doc.querySelectorAll('.edit-header');
							children.forEach(function(childNode) {
								childNode.style.zIndex = '';
								//childNode.style.position = '';
							});

							templateElement.classList.remove('hover');
						}
					}, doc);

					if (!_SimplePages.objectCache.hasOwnProperty(id)) {

						Command.get(id, null, function(t) {

							_SimplePages.objectCache[id] = t;

							Structr.fetchHtmlTemplate('simple-pages/template-edit-elements', { template: t }, function(html) {

								templateElement.insertAdjacentHTML('afterBegin', html);

								//alert(html);
								//$(templateElement).append(html);

								// bind actions 
								// make draggable (HTML5 drag'n drop)
								templateElement.setAttribute('draggable', 'true');
								templateElement.setAttribute('ondragover', 'return false');
								templateElement.addEventListener('dragstart', _SimplePages.dragStart, false);
								templateElement.addEventListener('dragenter', _SimplePages.dragEnter, false);
								templateElement.addEventListener('dragleave', _SimplePages.dragLeave, false);
								templateElement.addEventListener('dragover', _SimplePages.dragOver, false);
								//el.addEventListener('dragend', _SimplePages.dragEnd, false);
								templateElement.addEventListener('drop', _SimplePages.drop, false);

							});
						});

					}
				});

			} catch (e) { console.debug(e); };


			if (selector && !content.startsWith('<!')) {
				_SimplePages.highlightSelectionInPreview(selector, content);
			}

		});		
	},
	loadTemplate: function(id) {
		Command.get(id, null, function(obj) {
			_SimplePages.activeTemplate = obj;
			
			//console.log('loadTemplate:', obj);
			
			
			_SimplePages.showPreview('/' + obj.id + '?edit=7', obj.name, obj.content, obj.selector);
			//_SimplePages.showPreview('/' + obj.id + '', obj.name, obj.content, obj.selector);
			_SimplePages.components.context.querySelector('input[name="page-content"]').value = obj.pageContent;
			_SimplePages.components.context.querySelector('input[name="name"]').value = obj.name;
			
			LSWrapper.setItem(_SimplePages.currentTemplateIdKey, obj);
		}, 'editor');
	},
	highlightSelectionInPreview: function(selector, content) {
		let doc = _SimplePages.components.previewIframe.contentDocument;
		if (doc) {
			let el = doc.querySelector(selector);
			if (el) {
				
				el.outerHTML = content;
				el = doc.querySelector(selector);
				
				let w = el.offsetWidth;
				let h = el.offsetHeight;
				
				_SimplePages.activateElement(el);
				
				el.style.boxShadow = '0 0 0 99999px rgba(0, 0, 0, .8)';
				el.style.position  = 'relative';
				el.style.zIndex    = 99999;
			}
		}
	},
	activateElement: function(el, url) {
		let selector;
		
		if (el) {

			_SimplePages.restoreSavedStyle(_SimplePages.activeElement);
			_SimplePages.activeElement = el;

			el.style.border = '1px dashed green';
			
			selector = OptimalSelect.getSingleSelector(el);
			_SimplePages.components.context.querySelector('input[name="selector"]').value = selector;
			
		}
		
		if (url) {
			
			let xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject('Microsoft.XMLHTTP');
			xhr.open('GET', url);
			xhr.onreadystatechange = function() {

				if (xhr.readyState > 3 && xhr.status === 200) {

					let pageContent    = unescapeTags(html_beautify(xhr.responseText));

					if (pageContent) {
						_SimplePages.components.context.querySelector('input[name="page-content"]').value = pageContent;
						_SimplePages.components.context.querySelector('input[name="name"]').value = '';
					}

					let partialContent = _SimplePages.getPartialHTML(xhr.responseText, selector);
					_SimplePages.templateCodeEditor.setValue(partialContent);

				}
			};
			xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
			xhr.send();
		}
		
		return false;
	},
	getPartialHTML: function(html, selector) {
		
		let parser = new DOMParser();
		let doc = parser.parseFromString(html, 'text/html');

		if (selector) {
			html = doc.querySelector(selector).outerHTML;
		} else {
			html = '<!DOCTYPE ' + doc.doctype.name + '>\n'+ doc.documentElement.outerHTML;
		}

		return unescapeTags(html_beautify(html));
	},
	refreshCurrentPage: function() {
		_SimplePages.refreshPage(LSWrapper.getItem(_SimplePages.currentPageIdKey));
	},
	refreshPage: function(id) {
		document.querySelector('#id_' + id).click();
	},
	appendTemplate: function(template, parent, callback) {

		Structr.fetchHtmlTemplate('simple-pages/template-selection-node', { template: template }, function(html) {
			dialogText.append(html);
			
			//console.log(parent, template);

			let cloneComponent = function() {
				Command.cloneComponent(template.id, parent.id, function() {
					if (callback) callback();
				});
			}

			on('.template-' + template.id, 'click', cloneComponent);

			//let iframe = $('.template-' + template.id).find('iframe');
			//iframe.contents().on('click', cloneComponent);
			
			on(_SimplePages.components.previewIframe.contentDocument, 'click', cloneComponent);

		});
		
	},
	addPageTemplate: function(parent, callback) {

		//console.log(parent);

		Structr.dialog('Add page template to ' + parent.name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});

		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {

			let shadowPage = entities[0];

			Command.query('TemplateCategory', 10, 1, 'name', 'desc', { name: 'Page Templates' }, function(categories) {

				Command.query('Template', 1000, 1, 'name', 'desc', { category: { id: categories[0].id }, hidden: false, pageId: shadowPage.id }, function(templates) {

					templates.forEach(function(template) {
						_SimplePages.appendTemplate(template, parent, callback);
					});
				});
			});

		});

	},
	addTemplate: function(parent, callback) {
		Structr.dialog('Add template to ' + parent.name, function() {
			_SimplePages.refreshCurrentPage();
			dialogCancelButton = $('.closeButton', dialogBox);
		});

		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {

			let shadowPage = entities[0];
			
			Command.query('Template', 1000, 1, 'name', 'desc', { hidden: false, pageId: shadowPage.id }, function(templates) {

				templates.forEach(function(template) {
					_SimplePages.appendTemplate(template, parent, callback);
				});
			});
		});
		
	},
	replaceTemplate: function(t) {
		Structr.dialog('Replace template ' + t.name, function() {
			_SimplePages.refreshCurrentPage();
		});

		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {

			let shadowPage = entities[0];

			Command.query('Template', 1000, 1, 'name', 'desc', { hidden: false, pageId: shadowPage.id }, function(templates) {

				templates.forEach(function(template) {

					Structr.fetchHtmlTemplate('simple-pages/template-selection-node', { template: template }, function(html) {
						dialogText.append(html);

						on('.template-' + template.id, 'click', function(e) {
							dialogCancelButton.click();
							Command.replaceTemplate(t.id, template.id, function() {
								_SimplePages.refreshCurrentPage();
							}, dialogText);
						});
					});

				});


			});
		});

	},
	removeTemplateFromParent: function(id) {
		if (id) {
			//doc.contents().find('[data-structr-id="' + id + '"]').remove();
			Command.removeChild(id, function() {
				_SimplePages.refreshCurrentPage();
				//console.log(doc, doc.contents(), doc.contents().find('[data-structr-id="' + id + '"]'));
				//doc.contents().find('[data-structr-id="' + id + '"]').remove();
			});
		}
	},

	dragStart: function(e) {
		e.stopPropagation();
		//e.currentTarget.style.border = 'dashed';
		e.dataTransfer.setData('text/plain', e.target.dataset.structrId);
		//e.dataTransfer.dropEffect = 'move';
		//e.dataTransfer.effectAllowed = 'move';
	},
	dragEnter: function(e) {
		e.stopPropagation();
		//e.preventDefault();
		let el = e.target.dataset.structrId ? e.target : e.target.closest('[data-structr-id');
		el.style.border = '1px dashed rgba(0,0,0,.25)';
	},
	dragLeave: function(e) {
		e.stopPropagation();
		let el = e.target;
		e.currentTarget.style.border = '1px solid rgba(0,0,0,.25)';
		e.target.closest('[data-structr-id').style.border = '1px dashed rgba(0,0,0,.25)';
	},
	dragEnd: function(e) {
		//e.stopPropagation();
		//e.preventDefault();
		//e.currentTarget.style.border = '1px solid rgba(0,0,0,.25)';
	},
	drop: function(e) {
		//e.preventDefault();
		e.stopPropagation();
		e.target.style.border = '1px solid rgba(0,0,0,.25)';
		let sourceId = e.dataTransfer.getData('text/plain');
		let targetId = e.target.dataset.structrId || e.target.closest('[data-structr-id').dataset.structrId;
		//console.log('drop', sourceId, targetId);
		if (sourceId && targetId && sourceId !== targetId) {
			Command.appendChild(sourceId, targetId, null, function() {
				_SimplePages.refreshCurrentPage();
			});
		}
	},
	listPreviewURLs: function() {
		let pos = 0;
		let container = _SimplePages.components.tree.querySelector('.preview-urls-container');
		fastRemoveAllChildren(container);
		container.insertAdjacentHTML('beforeEnd', '<h3>Preview URLs</h3>');
		_SimplePages.previewURLList.forEach(function(url) {
			Structr.fetchHtmlTemplate('simple-pages/preview-url-list-node', { pos: pos, url: url }, function(html) {
				container.insertAdjacentHTML('beforeEnd', html);
				
				let pos = _SimplePages.previewURLList.indexOf(url);
				
				on('#preview-url_' + pos, 'mouseenter', function(e) {
					let el = e.target;
					el.querySelectorAll('.button').forEach(function(childNode) { childNode.style.display = 'inline-block'; });
				});

				on('#preview-url_' + pos, 'mouseleave', function(e) {
					let el = e.target;
					el.querySelectorAll('.button').forEach(function(childNode) { childNode.style.display = 'none'; });
				});

				on('#preview-url_' + pos + ' .delete_icon', 'click', function(e) {
					e.stopPropagation();
					e.preventDefault();
					_SimplePages.removeFromPreviewURLList(url);
					let el = e.target.closest('#preview-url_' + pos);
					el.parentNode.removeChild(el);
					return false;
				});
			});			
			pos++;
		});

		live('.preview-url', 'click', function(e) {
			let el = e.target.closest('.preview-url');
			let url = el.dataset.url;
			
			
			_SimplePages.components.tree.querySelectorAll('.node').forEach(function(childNode) {
				childNode.classList.remove('selected');
			});
			el.classList.add('selected');
			
			
			_SimplePages.components.context.querySelector('input[name="selector"]').value = '';
			_SimplePages.components.context.querySelector('input[name="name"]').value = '';
			_SimplePages.showPreview(url);
		});
	},
	listTemplates: function() {
		let container = _SimplePages.components.tree.querySelector('.templates-container');
		fastRemoveAllChildren(container);
		container.insertAdjacentHTML('beforeEnd', '<h3>Templates</h3>');
		Command.query('Template', 1000, 1, 'name', 'desc', { hidden: false, sharedComponentId: null }, function(templates) {
			templates.forEach(function(template) {
				Structr.fetchHtmlTemplate('simple-pages/template-list-node', { template: template }, function(html) {
					container.insertAdjacentHTML('beforeEnd', html);
					
					on('#id_' + template.id, 'mouseenter', function(e) {
						let el = e.target;
						el.querySelectorAll('.button').forEach(function(childNode) { childNode.style.display = 'inline-block'; });
					});

					on('#id_' + template.id, 'mouseleave', function(e) {
						let el = e.target;
						el.querySelectorAll('.button').forEach(function(childNode) { childNode.style.display = 'none'; });
					});
					
					if (_SimplePages.activeTemplate && _SimplePages.activeTemplate.id === template.id) {
						_SimplePages.components.tree.querySelector('#id_' + _SimplePages.activeTemplate.id).click();
					}
					
				});
			});
		
					
		});
	},
	listWidgets: function() {
		Command.query('Widget', 1000, 1, 'name', 'desc', { hidden: false }, function(widgets) {
			widgets.forEach(function(widget) {
				Structr.fetchHtmlTemplate('simple-pages/widget-list-node', { widget: widget }, function(html) {
					_SimplePages.components.context.insertAdjacentHTML('beforeEnd', html);
				});
			});
		});
	},
	saveStyle: function(el) {
		let style = window.getComputedStyle(el);
		_SimplePages.styles.push({
			element      : el,
			border       : style.border,
			boxShadow    : style.boxShadow,
			position     : style.position,
			zIndex       : style.zIndex,
			marginTop    : style.marginTop,
			marginRight  : style.marginRight,
			marginBottom : style.marginBottom,
			marginLeft   : style.marginLeft
		});
	},
	restoreSavedStyle: function(el) {
		let matches = _SimplePages.styles.filter(function(style) {
			return style.element === el;
		});

		if (matches && matches.length) {
			let style = matches[0];
			el.style.border       = style.border;
			el.style.boxShadow    = style.boxShadow;
			el.style.position     = style.position;
			el.style.zIndex       = style.zIndex;
			el.style.marginTop    = style.marginTop;
			el.style.marginRight  = style.marginRight;
			el.style.marginBottom = style.marginBottom;
			el.style.marginLeft   = style.marginLeft;
		}
	}
};


// matches polyfill
this.Element && function(ElementPrototype) {
	ElementPrototype.matches = ElementPrototype.matches ||
	ElementPrototype.matchesSelector ||
	ElementPrototype.webkitMatchesSelector ||
	ElementPrototype.msMatchesSelector ||
	function(selector) {
		var node = this, nodes = (node.parentNode || node.document).querySelectorAll(selector), i = -1;
		while (nodes[++i] && nodes[i] != node);
		return !!nodes[i];
	}
} (Element.prototype);

	
// closest polyfill
this.Element && function(ElementPrototype) {
	ElementPrototype.closest = ElementPrototype.closest ||
	function(selector) {
		var el = this;
		while (el.matches && !el.matches(selector)) el = el.parentNode;
		return el.matches ? el : null;
	}
} (Element.prototype);

function addEvent(el, type, handler) {
	if (el.attachEvent) {
		el.attachEvent('on'+type, handler);
	} else {
		el.addEventListener(type, handler);
	}
}

/**
 * Direct binding to element only.
 * Can only be used on existing elements.
 */
function on(selector, event, callback) {
	addEvent(document.querySelector(selector), event, function(e) {
		let el = e.target || e.srcElement;
		if (el && el.matches && el.matches(selector)) {
			callback.call(el, e);
		}
	});
}

/**
 * Live binding with optional context.
 * 
 * For mouseenter and mouseleave, it's similar to on().
 */
function live(selector, event, callback, context) {
	let el;
	if (event === 'mouseenter' || event === 'mouseleave') {
		el = (context || document).querySelector(selector);
	}
	addEvent(el || context || document, event, function(e) {
		let found, el = e.target || e.srcElement;
		while (el && el.matches && el !== context && !(found = el.matches(selector))) {
			el = el.parentElement;
		}
		if (found) callback.call(el, e);
	});
}

function offset(el) {
	let rect       = el.getBoundingClientRect();
	let scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;
	let scrollTop  = window.pageYOffset || document.documentElement.scrollTop;
	return { top: rect.top + scrollTop, left: rect.left + scrollLeft };
}