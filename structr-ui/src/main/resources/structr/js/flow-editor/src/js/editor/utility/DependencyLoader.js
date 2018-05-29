export class DependencyLoader {

    _getBasePath() {
        return "/structr/js/flow-editor/src/";
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
            let scripts = document.head.querySelectorAll("script");

            let didInject = false;

            for (let script of scripts) {
                if (script.hasAttribute("src") && script.getAttribute("src").indexOf(src) == -1) {
                    const script = document.createElement('script');
                    script.async = true;
                    script.src = this._getBasePath() + src;
                    script.addEventListener('load', resolve);
                    script.addEventListener('error', () => reject('Error loading script.'));
                    script.addEventListener('abort', () => reject('Script loading aborted.'));
                    document.head.appendChild(script);
                    didInject = true;
                    break;
                }
            }

            if (!didInject) {
                resolve(true);
            }

        });
    }

    injectStyle(href) {
        return new Promise((resolve, reject) => {
            let stylesheets = document.head.querySelectorAll("link");

            let didInject = false;

            for (let style of stylesheets) {
                if (style.hasAttribute("href") && style.getAttribute("href").indexOf(href) == -1) {
                    const link = document.createElement('link');
                    link.rel = "stylesheet";
                    link.href = this._getBasePath() + href;
                    link.addEventListener('load', resolve);
                    link.addEventListener('error', () => reject('Error loading script.'));
                    link.addEventListener('abort', () => reject('Script loading aborted.'));
                    document.head.appendChild(link);
                    didInject = true;
                    break;
                }
            }

            if (!didInject) {
                resolve(true);
            }

        });
    }
}