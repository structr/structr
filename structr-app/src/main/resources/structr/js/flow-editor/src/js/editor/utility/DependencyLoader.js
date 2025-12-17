export class DependencyLoader {

	constructor(basePath) {
		this.basePath = basePath;
	}

	_getBasePath() {
		return this.basePath + "/structr/js/flow-editor/src/";
	}

	injectDependencies(dependencyObject) {
		const self = this;
		return new Promise(resolve => {
			const scripts = dependencyObject.scripts;
			const stylesheets = dependencyObject.stylesheets;

			let promises = [];

			for (const script of scripts) {
				promises.push(self.injectScript(script));
			}

			for (const stylesheet of stylesheets) {
				promises.push(self.injectStyle(stylesheet));
			}

			Promise.all(promises).then( () => resolve(true));
		});
	}

	injectScript(src) {
		return new Promise((resolve, reject) => {

			let scripts = document.querySelectorAll("script");

			if (Array.prototype.slice.call(scripts).filter(s => s.hasAttribute("src") && s.getAttribute("src").indexOf(src) !== -1).length <= 0) {
                const script = document.createElement('script');
                script.src = this._getBasePath() + src;
                script.onload = () => resolve();
                script.onerror = () => reject('Error while loading script.');
                document.head.appendChild(script);
			} else {
				resolve();
			}
		});
	}

	injectStyle(href) {
		return new Promise((resolve, reject) => {
			let stylesheets = document.querySelectorAll("link");

			if (Array.prototype.slice.call(stylesheets).filter(s => s.hasAttribute("href") && s.getAttribute("href").indexOf(href) !== -1).length <= 0) {
				const link = document.createElement('link');
				link.rel = "stylesheet";
				link.type = "text/css";
				link.href = this._getBasePath() + href;
				link.onload = () => resolve();
				link.onerror = () => reject('Error while loading css.');
				document.head.appendChild(link);
			} else {
				resolve();
			}
		});
	}
}