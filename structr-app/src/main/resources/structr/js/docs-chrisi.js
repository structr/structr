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

		_Documentation.loadDoc(Structr.subModule || 'Getting Started', inPageHash);

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

        // build the table of contents
		const docsElement = document.getElementById('docs-main-navigation');

		fetch('/structr/docs/ontology?root=Structr&details=name&levels=1&format=toc&startLevel=1')
			.then(response => response.json())
			.then(json => {

                let index = 1;

                for (let entry of json.data) {

                    let menuItem = _Documentation.createElementFromHTML(_Documentation.templates.mainNavigationItem({
                        index: index,
                        label: entry.name
                    }));

                    docsElement.appendChild(menuItem);

                    let sublist = document.createElement('ul');
                    menuItem.appendChild(sublist);

                    const parentIndex = index;
                    const parentLabel = entry.name;
                    let subIndex = 1;

                    index++;

                    for (let child of entry.links[0].targets) {

                        let subItem = _Documentation.createElementFromHTML(_Documentation.templates.mainNavigationSubItem({
                            parentIndex: parentIndex,
                            parentLabel: parentLabel,
                            index: subIndex,
                            type: child.type,
                            name: child.name,
                            label: child.name
                        }));

                        subItem.addEventListener('click', e => {
                            _Documentation.loadContext(child.type, child.name)
                        });

                        sublist.appendChild(subItem);
                    }
				}
			});

	},
	loadDoc: (rawPath, hash) => {

        let iframe = document.querySelector('div#docs-area iframe');
        if (iframe) {

           iframe.src = `/structr/docs/ontology?root=${encodeURIComponent(rawPath)}&details=all&format=markdown`;
        } else {
            console.log('no iframe found')
        }
	},
    loadContext: (type, name) => {

        fetch(`/structr/docs/ontology?root=${type}:${encodeURIComponent(name)}&details=name&levels=1&format=toc&startLevel=1`)
            .then(response => response.json())
            .then(json => {

                let index = 1;
                let aside = document.querySelector('aside.index');

                aside.innerHTML = '';

                for (let entry of json.data) {

                    aside.appendChild(_Documentation.createElementFromHTML(_Documentation.templates.indexItem({
                        parentName: name,
                        type: type,
                        name: entry.name.replaceAll(/[\W]+/g, '-').toLowerCase(),
                        label: entry.name
                    })));
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

				let files = [];
				for (const key of Object.keys(_Documentation._content)) {
					const content = _Documentation._content[key].toLowerCase();
					if (content.indexOf(searchText.toLowerCase()) > -1) {
						files.push(key);
					}
				}

				for (const file of files) {

					const text            = _Documentation.removeAllMarkdownChars(_Documentation._content[file]);
					const index           = text.toLowerCase().indexOf(searchText.toLowerCase());
					const substringBefore = text.slice(0, index);
					const wordsBefore     = substringBefore.split(' ');
					const substringAfter  = text.slice(index);
					const wordsAfter      = substringAfter.split(' ');
					const numContextWords = 5;

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
						_Documentation.loadDoc(encodeURI(file));
					});

					_Documentation.search.searchResultsList.appendChild(result);
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
    createElementFromHTML: (html) => {
        let range = document.createRange();
        return range.createContextualFragment(html).firstElementChild;
    },
	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/docs-chrisi.css">
			<div id="docs-area">
				<nav>
					<ul id="docs-main-navigation">
					</ul>
				</nav>
				<iframe name="main-documentation"></iframe>
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
        mainNavigationSubItem: config => `<li><a target="main-documentation" href="/structr/docs/ontology?root=${config.type}:${encodeURIComponent(config.name)}&details=all&startLevel=0&format=markdown">${config.label}</a></li>`,
		indexItem: config => `<h2><a target="main-documentation" href="/structr/docs/ontology?root=${config.type}:${encodeURIComponent(config.parentName)}&details=all&startLevel=0&format=markdown#${config.name}">${config.label}</a></h2>`,
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
