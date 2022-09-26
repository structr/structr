'use strict';

export class Handler {

	constructor(frontendModule) {
		this.frontendModule = frontendModule;
	}

	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		// remove prefix "toast:"
		let selector = reloadTarget.substring(6);

		bootstrap.Toast.getOrCreateInstance(document.querySelector(selector)).show();
	}
}
