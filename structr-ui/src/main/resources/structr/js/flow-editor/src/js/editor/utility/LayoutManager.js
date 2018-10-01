'use strict';

import {Persistence} from "../../persistence/Persistence.js";
import {Rest} from "../../rest/Rest.js";

export class LayoutManager {

    constructor(editor) {
        this._persistence = new Persistence();
        this._rest = new Rest();
        this._flowEditor = editor;

    }

    async getOwnSavedLayout() {
        let layout = null;

        let me = await this._persistence.getNodesByClass({type: "me"});

        let r = await this._rest.get('/structr/rest/FlowContainerConfiguration?principal=' + me[0].id + '&flow=' + this._flowEditor._flowContainer.id + '&validForEditor=' + this._flowEditor._editorId);

        if (r !== null && r !== undefined && r.result_count > 0) {
            layout = JSON.parse(r.result[0].configJson);
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

        let r = await this._rest.get('/structr/rest/FlowContainerConfiguration?&flow=' + this._flowEditor._flowContainer.id + '&validForEditor=' + this._flowEditor._editorId + '&sort=lastModifiedDate&order=asc');

        if (r !== null && r !== undefined && r.result_count > 0 && !raw) {
            layouts = r.result.map( res => JSON.parse(res.configJson));
        } else if (r !== null && r !== undefined && r.result_count > 0 && raw) {
            layouts = r.result;
        }

        return layouts;

    }

    async saveLayout(visibleForAll) {

        var layout = {};
        var editorConfig = this._flowEditor.getEditorJson();

        for (let [key,node] of Object.entries(editorConfig.nodes)) {

            if (node.data.dbNode !== null && node.data.dbNode !== undefined) {
                layout[node.data.dbNode.id] = node.position;
            }

        }

        let persistence = new Persistence();
        let rest = new Rest();

        let me = await persistence.getNodesByClass({type:"me"});

        if (me !== null && me !== undefined && me.length > 0) {
            me = me[0];
        }

        if (me.id !== null && me.id !== undefined) {
            let r = await rest.get('/structr/rest/FlowContainerConfiguration?principal=' + me.id + '&flow=' + this._flowEditor._flowContainer.id + '&validForEditor=' + this._flowEditor._editorId);

            if (r != null && r !== undefined && r.result_count > 0) {
                let config = persistence._wrapObject(r.result[0], new Object());
                config.configJson = JSON.stringify(layout);
                config.visibleToAuthenticatedUsers =  visibleForAll !== undefined ? visibleForAll : false;
                config.visibleToPublicUsers =  visibleForAll !== undefined ? visibleForAll : false;
            } else {
                await persistence._persistObject({
                    type: "FlowContainerConfiguration",
                    flow: this._flowEditor._flowContainer.id,
                    principal: me.id,
                    validForEditor: this._flowEditor._editorId,
                    configJson: JSON.stringify(layout),
                    visibleToPublicUsers: visibleForAll !== undefined ? visibleForAll : false,
                    visibleToAuthenticatedUsers: visibleForAll !== undefined ? visibleForAll : false
                });
            }
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