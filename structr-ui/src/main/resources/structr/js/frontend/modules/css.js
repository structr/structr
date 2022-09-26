'use strict';

export class Handler {

	constructor(frontendModule) {
		this.frontendModule = frontendModule;
	}

	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		// remove prefix "css:"
		let css = reloadTarget.substring(4);

		element.classList.add(css);

		window.setTimeout(() => {
			element.classList.remove(css);
		}, 2000);

	}
}
