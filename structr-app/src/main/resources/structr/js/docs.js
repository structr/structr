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
	Structr.registerModule(_Documentation);
});

let _Documentation = {
	_content: {},
	_moduleName: 'docs',
	//urlHashKey: 'structrUrlHashKey_' + location.port,
	currentDoc: undefined,

	unload: () => {
		_Documentation.search.dispose();
		_Documentation.currentDoc = undefined;

		window.removeEventListener('hashchange', _Documentation.hashChangeHandler);
	},
	onload: () => {

		Structr.setMainContainerHTML(_Documentation.templates.main());
		_Documentation.init();

		const lastIndexOfHash = Structr.subModule?.lastIndexOf('#');
		let inPageHash;
		if (lastIndexOfHash > 0) {
			inPageHash = Structr.subModule?.substring(lastIndexOfHash + 1);
			Structr.subModule = Structr.subModule?.substring(0, lastIndexOfHash);
		}

		_Documentation.preloadContent().then(() => {

			// install handler after preload
			window.addEventListener('hashchange', _Documentation.hashChangeHandler);

			_Documentation.updateHashWithoutAddingHistory('#docs:' + (Structr.subModule || '1-Introduction/1-Getting%20Started.md') + '#' + (inPageHash ?? ''))
			_Documentation.loadDoc(Structr.subModule || '1-Introduction/1-Getting%20Started.md', inPageHash);
		});

		Structr.setFunctionBarHTML(_Documentation.templates.searchField());

		Structr.mainMenu.unblock(100);

		_Documentation.search.init();

		const checkIndexLinks = (clickedEl) => {
			document.querySelectorAll('.index a').forEach(el => {
				el.classList.remove('active');
			});
			clickedEl.classList.add('active');
			window.setTimeout(() => { window.scrollBy(0,-108); }, 10);
		};

		document.querySelectorAll('.index a').forEach(el => {
			el.addEventListener('click', e => {
				checkIndexLinks(e.target);
			});
		});
	},
	hashChangeHandler: e => {

		let hash = new URL(e.newURL).hash;

		if (hash.startsWith('#docs:')) {

			let rawPath = hash.substring(hash.indexOf(':') + 1);
			let deepHash = undefined;

			if (rawPath.indexOf('#') > -1) {
				deepHash = rawPath.substring(rawPath.indexOf('#') + 1);
				rawPath = rawPath.substring(0, rawPath.indexOf('#'));
			}

			_Documentation.loadDoc(rawPath, deepHash);

		} else {

			// browser should have navigated by itself - update hash so it survives page reload
			// _Documentation.updateHashWithoutAddingHistory('#docs:' + _Documentation.currentDoc + hash);
			_Documentation.loadDoc(_Documentation.currentDoc, hash.substring(1));
		}
	},
	updateHashWithoutAddingHistory: (newHash) => {
		history.replaceState(history.state, "", newHash);
	},
	keyDownHandler: e => {

		if (e.key === 'Escape') {
			_Documentation.search.hideSearch();
			_Documentation.search.clearSearchResults();
		}

        if ((e.code === 'KeyK' || e.keyCode === 75) && ((!_Helpers.isMac() && e.ctrlKey) || (_Helpers.isMac() && e.metaKey))) {
            e.preventDefault();
			_Documentation.search.showSearch();
        }
	},
	removeAllMarkdownChars: (text) => {
		return text.replace(/[*`![\]()_~#>\-+=|\\{}<>^]/g, ' ').replace(/ +/g, ' ');
	},
	init: () => {

		const docsElement = document.getElementById('docs-main-navigation');

		fetch('/structr/docs/index.txt')
			.then(response => response.text())
			.then(text => {
				text.split('\n').forEach(line => {

					if (line) {
						const [index, label] = line.split('-');
						docsElement.insertAdjacentHTML('beforeend', _Documentation.templates.mainNavigationItem({
							index: index,
							label: label
						}));

						const parentIndex = index;
						const parentLabel = label;

						docsElement.querySelector(`li:nth-child(${index})`).insertAdjacentHTML('beforeend', '<ul>');

						const articleListElement = docsElement.querySelector(`li:nth-child(${index}) ul`);

						fetch(`/structr/docs/${index}-${label}/index.txt`)
							.then(response => {
								if (response.ok) {
									return response.text();
								}
							})
							.then(text => {
								text.split('\n').forEach(line => {

									if (line) {

										const [index, name] = line.split('-');
										articleListElement.insertAdjacentHTML('beforeend', _Documentation.templates.mainNavigationSubItem({
											parentIndex: parentIndex,
											parentLabel: parentLabel,
											index: index,
											name: name,
											label: name.substring(0, name.lastIndexOf('.'))
										}));

									}
								});
							});
					}
				});
			});

	},
	preloadContent: () => {
		return new Promise(async resolve => {

			await fetch('/structr/docs/index.txt')
				.then(response => response.text())
				.then(async text => {

					for (const line of text.split('\n')) {

						if (line) {
							const [index, label] = line.split('-');

							const parentIndex = index;
							const parentLabel = label;

							await fetch(`/structr/docs/${index}-${label}/index.txt`)
								.then(async response => {
									if (response.ok) {
										return response.text();
									}
								})
								.then(async text => {

									for (const line of text.split('\n')) {

										if (line) {

											const path = `${parentIndex}-${parentLabel}/${line}`;

											await fetch(`/structr/docs/${path}`)
												.then(async response => {
													if (response.ok) {
														return response.text();
													}
												})
												.then(async text => {
													_Documentation._content[path] = text;
												});

										}
									}

								});
						}
					}
					resolve();

				});
		});
	},
	loadDoc: (rawPath, hash, searchText) => {

		// console.log('Loading documentation page ', rawPath, hash);
		const path = decodeURI(rawPath);
		let articleElement       = document.querySelector('#docs-area article');

		if (_Documentation.currentDoc !== rawPath) {

			_Documentation.currentDoc = rawPath;

			const text = _Documentation._content[path];
			const converter = new showdown.Converter({ tables: true, simplifiedAutoLink: true, prefixHeaderId: `${path}` });

			// Transform image URLs
			// Input: Local URLs like just "login.png": ![Login Screen](login.png)
			// Output: /docs/5-Admin%20User%20Interface/login.png

			const markdownImageRegex = /!\[([^\]]*)\]\(((?!https?:\/\/)[^)\s]+(?<!\.md))\)/g;
			const markdownLinkRegex  = /[^!]\[([^\]]*)\]\(((?!https?:\/\/)[^)\s]+)\)/g;

			const prefixMarkdownImages = (text, prefix) => {
				return text?.replace(markdownImageRegex, (match, alt, url) => {
					return `![${alt}](${prefix}${url})`;
				});
			}

			// Transform image URLs
			// Input: Local URLs like just "login.png": ![Login Screen](login.png)
			// Output: /docs/5-Admin%20User%20Interface/login.png

			const prefixArticleLinks = (text, prefix) => {
				return text?.replace(markdownLinkRegex, (match, alt, url) => {
					return ` [${alt}](${prefix}${url})`;
				});
			}

			const parent       = path.substring(0, path.lastIndexOf('/'));
			let updatedContent = prefixMarkdownImages(text, '/structr/docs/' + parent.replace(/ /g, '%20') + '/');
			updatedContent     = prefixArticleLinks(updatedContent, '/structr/#docs:' + parent.replace(/ /g, '%20') + '/');

			const html               = converter.makeHtml(updatedContent);
			articleElement.innerHTML = `<p class="main-category subtitle">${parent.substring(parent.lastIndexOf('-')+1)}</p>` + html;

			const indexHtml = converter.makeHtml(html);

			// Page index
			document.querySelector('#docs-area aside').innerHTML = '<div class="index-heading">On this page</div>' + indexHtml;

			// Activate in-page index links (aside element on the right-hand side)
			document.querySelectorAll('#docs-area aside h1, #docs-area aside h2, #docs-area aside h3, #docs-area aside h4').forEach(el => {
				const newAElement = document.createElement('A');
				newAElement.href = '#' + el.id;
				el.removeAttribute('id');
				el.parentNode.insertBefore(newAElement, el);
				newAElement.appendChild(el);
			});
		}

		articleElement.scrollTo(0,0);

		if (hash) {

			const el = document.getElementById(hash) || document.querySelector(`[name="${CSS.escape(hash)}"]`);
			if (el) el.scrollIntoView({ block: "start" });

			_Documentation.updateHashWithoutAddingHistory('#docs:' + _Documentation.currentDoc + '#' + hash);

		} else if (searchText) {

			let navigateToClosestAnchor = (el) => {
				let id;
				while (!id) {
					if (el.id) {
						id = el.id;
						break;
					} else {
						el = el.previousElementSibling;
						if (el === articleElement) {
							return;
						}
					}
				}

				// attempt to find link for this id
				let linkToEl = document.querySelector(`a[href="#${id}"]`);
				if (linkToEl) {
					linkToEl.click();
				} else {
					// this breaks on reload...
					location.hash = '#' + id;
				}
			};

			for (let child of articleElement.children) {

				if (child.innerText.toLowerCase().contains(searchText)) {
					navigateToClosestAnchor(child);
					break;
				}
			}
		}

		document.querySelectorAll('#docs-area nav a').forEach(aElement => {
			aElement.classList.remove('active');
		});

		// Make navigation link active and scroll into view
		waitForElement(`#docs-area nav a[href='#docs:${decodeURI(path)}']`).then(el => {
			el.classList.add('active');
			el.scrollIntoView({ block: "start" });
		});

		const isElementInViewport = el => {

			const rect = el.getBoundingClientRect();

			return (
				rect.top >= 0 &&
				rect.left >= 0 &&
				rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && /* or $(window).height() */
				rect.right <= (window.innerWidth || document.documentElement.clientWidth) /* or $(window).width() */
			);
		};

		articleElement.addEventListener('scroll', e => {
			for (const el of document.querySelectorAll('.article h1, .article h2')) {
				const isVisible = isElementInViewport(el);
				if (isVisible) {
					document.querySelectorAll('.index a').forEach(el => {
						el.classList.remove('active');
					});
					const id = el.id; //querySelector('a').id;
					const indexEl = document.querySelector('.index a[href="#' + id + '"]');
					indexEl.classList.add('active');
					break;
				} else {
					/*
					const id = el.querySelector('a').id;
					const indexEl = document.querySelector('.index a[href="#' + id + '"]');
					indexEl.classList.remove('active');
					*/
				}
			}
		});
	},
	search: {
		searchOverlay: undefined,
		overlaySearchField: undefined,
		searchAnimationFrameKey: undefined,
		init: () => {

			_Documentation.search.searchOverlay      = _Helpers.createSingleDOMElementFromHTML(_Documentation.templates.searchOverlay());
			_Documentation.search.overlaySearchField = _Documentation.search.searchOverlay.querySelector('#overlay-search-field');
			_Documentation.search.searchResultsList  = _Documentation.search.searchOverlay.querySelector('#search-results ul');

			document.querySelector('body').insertAdjacentElement('beforeend', _Documentation.search.searchOverlay);

			document.querySelector('#search-field').addEventListener('click', e => {
				_Documentation.search.showSearch();
			});

			_Documentation.search.searchOverlay.addEventListener('click', e => {
				if (e.target?.id === 'search-overlay') {
					_Documentation.search.hideSearch();
					_Documentation.search.clearSearchResults();
				}
			});

			document.addEventListener('keydown', _Documentation.keyDownHandler);

			_Documentation.search.overlaySearchField.addEventListener('keyup', e => {
				_Helpers.requestAnimationFrameWrapper(_Documentation.search.searchAnimationFrameKey, _Documentation.search.doSearch);
			});

			_Documentation.search.overlaySearchField.addEventListener('search', e => {
				let query = e.target.value;
				if (query.trim() === '') {
					_Documentation.search.clearSearchResults();
				}
			});
		},
		dispose: () => {

			document.removeEventListener('keydown', _Documentation.keyDownHandler);

			_Helpers.fastRemoveElement(_Documentation.search.searchOverlay);

			delete _Documentation.search.searchOverlay;
			delete _Documentation.search.overlaySearchField;
			delete _Documentation.search.searchResultsList;
		},
		clearSearchResults: () => {
			_Documentation.search.searchResultsList.replaceChildren();
		},
		doSearch: () => {

			_Documentation.search.clearSearchResults();

			let searchText = _Documentation.search.overlaySearchField.value;

			if (searchText) {

				for (const file of Object.keys(_Documentation._content)) {

					const textLowerCase       = _Documentation.removeAllMarkdownChars(_Documentation._content[file]).toLowerCase();
					const searchTextLowerCase = searchText.toLowerCase();
					const numContextWords     = 5;
					let index                 = textLowerCase.indexOf(searchTextLowerCase.toLowerCase());

					if (index >= 0) {

						const substringBefore = textLowerCase.slice(0, index);
						const wordsBefore = substringBefore.split(' ');
						const substringAfter = textLowerCase.slice(index);
						const wordsAfter = substringAfter.split(' ');

						let result = _Helpers.createSingleDOMElementFromHTML(`
							<li class="search-result cursor-pointer">
								<div class="group-aria-selected:text-sky-600">
									<span>${wordsBefore.slice(Math.max(wordsBefore.length - numContextWords, 1)).join(' ')}${wordsAfter.slice(0, numContextWords).join(' ')}</span>
								</div>
								<div aria-hidden="true" class="search-result-path">${file}<span class="sr-only">/</span></div>
							</li>
						`);

						result.addEventListener('click', e => {
							_Documentation.search.hideSearch();
							_Documentation.loadDoc(encodeURI(file), null, searchTextLowerCase);
						});

						_Documentation.search.searchResultsList.appendChild(result);
					}
				}
			}
		},
		showSearch: () => {
			_Documentation.search.searchOverlay.classList.remove('hidden');
			_Documentation.search.overlaySearchField.focus();
		},
		hideSearch: () => {
			_Documentation.search.searchOverlay.classList.add('hidden');
		}
	},
	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/docs.css">
			<div id="docs-area">
				<nav>
					<ul id="docs-main-navigation">
					</ul>
				</nav>
				<article class="article"></article>
				<aside class="index"></aside>
			</div>`,
		searchField: config => `
			<div id="search-field">
				<svg aria-hidden="true" viewBox="0 0 20 20">
					<path fill="#555" d="M16.293 17.707a1 1 0 0 0 1.414-1.414l-1.414 1.414ZM9 14a5 5 0 0 1-5-5H2a7 7 0 0 0 7 7v-2ZM4 9a5 5 0 0 1 5-5V2a7 7 0 0 0-7 7h2Zm5-5a5 5 0 0 1 5 5h2a7 7 0 0 0-7-7v2Zm8.707 12.293-3.757-3.757-1.414 1.414 3.757 3.757 1.414-1.414ZM14 9a4.98 4.98 0 0 1-1.464 3.536l1.414 1.414A6.98 6.98 0 0 0 16 9h-2Zm-1.464 3.536A4.98 4.98 0 0 1 9 14v2a6.98 6.98 0 0 0 4.95-2.05l-1.414-1.414Z"></path>
				</svg>
				<input type="text" placeholder="Search docs">
			</div>
		`,
		mainNavigationItem: config => `<li class="docs-main-nav-item">${config.label}</li>`,
		mainNavigationSubItem: config => `<li><a href="#docs:${config.parentIndex}-${config.parentLabel}/${config.index}-${config.name}">${config.label}</a></li>`,
		searchOverlay: config => `
			<div id="search-overlay" class="hidden">
				<div id="search-overlay-dialog">
					<svg aria-hidden="true" viewbox="0 0 20 20">
						<path d="M16.293 17.707a1 1 0 0 0 1.414-1.414l-1.414 1.414ZM9 14a5 5 0 0 1-5-5H2a7 7 0 0 0 7 7v-2ZM4 9a5 5 0 0 1 5-5V2a7 7 0 0 0-7 7h2Zm5-5a5 5 0 0 1 5 5h2a7 7 0 0 0-7-7v2Zm8.707 12.293-3.757-3.757-1.414 1.414 3.757 3.757 1.414-1.414ZM14 9a4.98 4.98 0 0 1-1.464 3.536l1.414 1.414A6.98 6.98 0 0 0 16 9h-2Zm-1.464 3.536A4.98 4.98 0 0 1 9 14v2a6.98 6.98 0 0 0 4.95-2.05l-1.414-1.414Z"></path>
					</svg>
					<input spellcheck="false" id="overlay-search-field" autocomplete="off" type="search" maxlength="512" placeholder="Search docs" autofocus="autofocus" aria-autocomplete="both" aria-labelledby=":R2dja:-label" data-autofocus="true">
					<div id="search-results">
						<ul></ul>
					</div>
				</div>
			</div>
		`
	}
}
