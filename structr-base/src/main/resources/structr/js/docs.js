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

	unload: () => {
	},
	onload: () => {

		Structr.setMainContainerHTML(_Documentation.templates.main());
		_Documentation.init();

		const lastIndexOfHash = Structr.subModule?.lastIndexOf('#');
		let inPageHash;
		if (lastIndexOfHash > 0) {
			inPageHash = Structr.subModule?.substring(lastIndexOfHash);
			Structr.subModule = Structr.subModule?.substring(0, lastIndexOfHash);
		}

		_Documentation.preloadContent().then(() => {
			_Documentation.loadDoc(Structr.subModule || '1-Introduction/1-Getting%20Started.md', inPageHash);
		});

		Structr.setFunctionBarHTML(_Documentation.templates.searchField());
		document.querySelector('body').insertAdjacentHTML('beforeend', _Documentation.templates.searchOverlay());

		Structr.mainMenu.unblock(100);

		waitForElement('#search-field').then(searchField => {
			searchField.addEventListener('click', e => {
				const searchOverlay = document.getElementById('search-overlay');
				searchOverlay.classList.remove('hidden');
				//overlaySearchField.focus();
				document.querySelector('[data-autofocus=true]').focus();
			});
		});

		const overlaySearchField = document.getElementById('overlay-search-field');
		let   searchOverlay      = document.getElementById('search-overlay');
		const searchResultsBox   = document.querySelector('#search-results');
		let   searchResultsList  = document.querySelector('#search-results ul');

		let searchText;

		window.addEventListener('keydown', e => {
			if (e.key === 'Escape') {
				searchOverlay = document.getElementById('search-overlay');
				searchOverlay.classList.add('hidden');
			}
		});

		const removeAllMarkdownChars = (text) => {
			return text.replace(/[*`![\]()_~#>\-+=|\\{}<>^]/g, ' ');
		}

		window.addEventListener('keyup', e => {
			searchText = overlaySearchField.value;

			if (!searchResultsList) {
				searchResultsBox.insertAdjacentHTML('afterbegin', '<ul></ul>');
				searchResultsList = document.querySelector('#search-results ul');
			} else {
				searchResultsList.replaceChildren();
			}

			if (searchText) {

				let files = [];
				for (const key of Object.keys(_Documentation._content)) {
					const content = _Documentation._content[key].toLowerCase();
					if (content.indexOf(searchText.toLowerCase()) > -1) {
						files.push(key);
					}
				}

				for (const file of files) {

					const text = removeAllMarkdownChars(_Documentation._content[file]);

					//const index = file.content.indexOf(searchText);
					const path  = encodeURI('#docs:' + file);
					const index = text.toLowerCase().indexOf(searchText.toLowerCase());
					const substringBefore = text.slice(0, index);
					const wordsBefore = substringBefore.split(' ');
					const substringAfter = text.slice(index);// + searchText.length);
					const wordsAfter = substringAfter.split(' ');

					const n = 5;

					searchResultsList.insertAdjacentHTML('beforeend', '<li class="search-result"><div class="group-aria-selected:text-sky-600"><span>' +

						wordsBefore.slice(Math.max(wordsBefore.length - n, 1)).join(' ')
						+ wordsAfter.slice(0, n).join(' ')

						+ `</span></div><div aria-hidden="true" class="search-result-path">` +
						file
						+ '<span class="sr-only">/</span></div></li>');
					//});

					document.querySelectorAll('.search-result').forEach(el => {
						el.addEventListener('click', searchResultElement => {
							window.location.href = el.href;
							window.location.reload();
						});
					});

				}
			}
		});



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

								articleListElement.querySelectorAll(`li a`).forEach(el => {
									el.addEventListener('click', e => {
										const el = e.target;
										const href = el.href;
										_Documentation.loadDoc(href.substring(href.lastIndexOf(':') + 1));
									});
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
	loadDoc: (rawPath, hash) => { console.log('Loading documentation page ', rawPath, hash);
		const path = decodeURI(rawPath);
		//const path = rawPath;
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

		const parent = path.substring(0, path.lastIndexOf('/'));
		let updatedContent = prefixMarkdownImages(text, '/structr/docs/' + parent.replace(/ /g, '%20') + '/');
		updatedContent = prefixArticleLinks(updatedContent, '/structr/#docs:' + parent.replace(/ /g, '%20') + '/');

		const html      = converter.makeHtml(updatedContent);
		document.querySelector('#docs-area article').innerHTML = `<p class="main-category subtitle">${parent.substring(parent.lastIndexOf('-')+1)}</p>` + html;

		const indexHtml = converter.makeHtml(html);

		// Page index
		document.querySelector('#docs-area aside').innerHTML = '<div class="index-heading">On this page</div>' + indexHtml;

		// Activate in-page index links (aside element on the right-hand side)
		document.querySelectorAll('#docs-area aside h1, #docs-area aside h2, #docs-area aside h3, #docs-area aside h4').forEach(el => {
			const newAElement = document.createElement('A');
			const hrefAfterClick = `#docs:${path}#${el.id}`;
			newAElement.href = '#' + el.id;
			newAElement.dataset.hrefAfterClick = hrefAfterClick;
			el.removeAttribute('id');
			el.parentNode.insertBefore(newAElement, el);
			newAElement.appendChild(el);
			newAElement.addEventListener('click', e => {
				//e.preventDefault();
				const aEl = e.target.closest('a');
				document.querySelectorAll('#docs-area aside a').forEach(el => {
					el.classList.remove('active');
				});
				aEl.classList.add('active');
				window.setTimeout(() => {
					window.location.hash = aEl.dataset.hrefAfterClick;
				}, 100);
			});
		});

		document.querySelector('#docs-area article').scrollTo(0,0);

		if (hash) {
			document.querySelector(`#docs-area aside a[href="${hash}"]`).click();
		}

		document.querySelectorAll('#docs-area nav a').forEach(aElement => {
			aElement.classList.remove('active');
		});

		// Make navigation link active
		waitForElement(`#docs-area nav a[href='#docs:${decodeURI(path)}']`).then(el => {
			el.classList.add('active');
		});

		// Allow navigation from main document
		document.querySelectorAll('#docs-area .article a').forEach(aElementInArticle => {
			aElementInArticle.addEventListener('click', e => {
				const el = e.target;
				const href = el.href;
				_Documentation.loadDoc(href.substring(href.lastIndexOf(':') + 1));
			});
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

		document.querySelector('#docs-area article').addEventListener('scroll', e => {
			for (const el of document.querySelectorAll('.article h1, .article h2, .article h3')) {
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
		searchField: config => `<div id="search-field"><svg aria-hidden="true" viewBox="0 0 20 20"><path fill="#555" d="M16.293 17.707a1 1 0 0 0 1.414-1.414l-1.414 1.414ZM9 14a5 5 0 0 1-5-5H2a7 7 0 0 0 7 7v-2ZM4 9a5 5 0 0 1 5-5V2a7 7 0 0 0-7 7h2Zm5-5a5 5 0 0 1 5 5h2a7 7 0 0 0-7-7v2Zm8.707 12.293-3.757-3.757-1.414 1.414 3.757 3.757 1.414-1.414ZM14 9a4.98 4.98 0 0 1-1.464 3.536l1.414 1.414A6.98 6.98 0 0 0 16 9h-2Zm-1.464 3.536A4.98 4.98 0 0 1 9 14v2a6.98 6.98 0 0 0 4.95-2.05l-1.414-1.414Z"></path></svg>
		<input type="text" placeholder="Search docs"></div>`,
		mainNavigationItem: config => `<li class="docs-main-nav-item">${config.label}</li>`,
		mainNavigationSubItem: config => `<li><a href="#docs:${config.parentIndex}-${config.parentLabel}/${config.index}-${config.name}">${config.label}</a></li>`,
		searchOverlay: config => `
<div id="search-overlay" class="hidden">
	<div id="search-overlay-dialog">
		<svg aria-hidden="true" viewbox="0 0 20 20">
			<path d="M16.293 17.707a1 1 0 0 0 1.414-1.414l-1.414 1.414ZM9 14a5 5 0 0 1-5-5H2a7 7 0 0 0 7 7v-2ZM4 9a5 5 0 0 1 5-5V2a7 7 0 0 0-7 7h2Zm5-5a5 5 0 0 1 5 5h2a7 7 0 0 0-7-7v2Zm8.707 12.293-3.757-3.757-1.414 1.414 3.757 3.757 1.414-1.414ZM14 9a4.98 4.98 0 0 1-1.464 3.536l1.414 1.414A6.98 6.98 0 0 0 16 9h-2Zm-1.464 3.536A4.98 4.98 0 0 1 9 14v2a6.98 6.98 0 0 0 4.95-2.05l-1.414-1.414Z"></path>
		</svg>
		<input spellcheck="false" id="overlay-search-field" autocomplete="off" type="search" maxlength="512" placeholder="Search docs" autofocus="autofocus" aria-autocomplete="both" aria-labelledby=":R2dja:-label" data-autofocus="true">
		<div id="search-results"><ul></ul></div>
	</div>
</div>`
	}
}
