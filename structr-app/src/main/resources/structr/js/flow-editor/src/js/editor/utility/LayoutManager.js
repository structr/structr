'use strict';

import {Persistence} from "../../../../../lib/structr/persistence/Persistence.js";
import {Rest} from "../../../../../lib/structr/rest/Rest.js";
import {StructrRest} from "../../../../../lib/structr/rest/StructrRest.js";

export class LayoutManager {

    constructor(editor) {
        this._persistence = new Persistence();
        this._rest = new Rest();
        this._structrRest = new StructrRest();
        this._flowEditor = editor;

    }

    async getActiveSavedLayout() {
    	let layout = null;

        if (this._flowEditor._flowContainer.activeConfiguration !== null && this._flowEditor._flowContainer.activeConfiguration !== undefined) {
            let restResult = await this._structrRest.getById('FlowContainerConfiguration', this._flowEditor._flowContainer.activeConfiguration.id, 'all');
			layout = await JSON.parse(restResult.result.configJson);
		}

        return layout;
    }

    async getSavedLayoutById(id) {
        let layout = null;

        let r = await this._rest.get('/structr/rest/FlowContainerConfiguration?id=' + id + '&flow=' + this._flowEditor._flowContainer.id + '&validForEditor=' + this._flowEditor._editorId);

        if (r !== null && r !== undefined && r.result_count > 0) {
            layout = JSON.parse(r.result[0].configJson);
        }

        return layout;
    }

    async getSavedLayouts(raw) {

        let layouts = null;

        let r = await this._rest.get('/structr/rest/FlowContainerConfiguration?&flow=' + this._flowEditor._flowContainer.id + '&validForEditor=' + this._flowEditor._editorId + '&' + '_sort' + '=lastModifiedDate&' + '_order' + '=asc');

        if (r !== null && r !== undefined && r.result_count > 0 && !raw) {
            layouts = r.result.map( res => JSON.parse(res.configJson));
        } else if (r !== null && r !== undefined && r.result_count > 0 && raw) {
            layouts = r.result;
        }

        return layouts;

    }

    async saveLayout(visibleForAll, saveAsNewLayout) {
        let layout = {};
        let editorConfig = this._flowEditor.getEditorJson();

        for (let [key,node] of Object.entries(editorConfig.nodes)) {

            if (node.data.dbNode !== null && node.data.dbNode !== undefined) {
                layout[node.data.dbNode.id] = node.position;
            }

        }

		let activeLayout = null;

		if (this._flowEditor._flowContainer.activeConfiguration !== null && this._flowEditor._flowContainer.activeConfiguration !== undefined && saveAsNewLayout === false) {
			activeLayout = await this._persistence.getNodesById(this._flowEditor._flowContainer.activeConfiguration.id, {type: "FlowContainerConfiguration"}, 'all');
			if (activeLayout !== null && activeLayout !== undefined && activeLayout.length > 0) {
				activeLayout = activeLayout[0];
			}
		}

        if (activeLayout !== null && activeLayout !== undefined && saveAsNewLayout === false) {
			activeLayout.configJson = JSON.stringify(layout);
			activeLayout.visibleToAuthenticatedUsers =  visibleForAll !== undefined ? visibleForAll : false;
			activeLayout.visibleToPublicUsers =  visibleForAll !== undefined ? visibleForAll : false;
            return activeLayout;
        } else {
            return await this._persistence._persistObject({
                type: "FlowContainerConfiguration",
                flow: this._flowEditor._flowContainer.id,
				activeForFlow: this._flowEditor._flowContainer.id,
                validForEditor: this._flowEditor._editorId,
                configJson: JSON.stringify(layout),
                visibleToPublicUsers: visibleForAll !== undefined ? visibleForAll : false,
                visibleToAuthenticatedUsers: visibleForAll !== undefined ? visibleForAll : false
            });
        }

    }

    applySavedLayout(layout) {

        let editor = this._flowEditor._editor;

        if (layout !== null && layout !== undefined) {

            for (let [key, node] of Object.entries(editor.nodes)) {
                if (node.data.dbNode !== undefined && layout[node.data.dbNode.id] !== undefined) {
                    node.position = layout[node.data.dbNode.id];
                }

            }

            this._flowEditor._editor.view.update();
            this._flowEditor.resetView();
        }

    }

}