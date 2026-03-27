'use strict';

let debug = false;

import {Rest} from './Rest.js';

export class StructrRest {

    constructor(basePath = "") {
        this.restEndPoint = "/structr/rest/";
        this._rest = new Rest(basePath);
    }

    get(path, view) {
        let queryPath = this.restEndPoint + path;
        if (view !== undefined && view !== null && view.length > 0) {
            queryPath += ("/" + view);
        }
        return this._rest.get(queryPath);
    }

    getById(type, id, view) {
        let queryPath = this.restEndPoint;
        if (type !== undefined && type !== null && type.length > 0) {
            queryPath += (type + "/");
        }

        queryPath += id;

        if (view !== undefined && view !== null &&view.length > 0) {
            queryPath += ("/" + view);
        }

        return this._rest.get(queryPath);
    }

    getByName(type, name, view) {
        let queryPath = this.restEndPoint;
        if (type !== undefined && type !== null && type.length > 0) {
            queryPath += (type + "/");
        }

        if (view !== undefined && view !== null && view.length > 0) {
            queryPath += view;
        }

        queryPath += ("?name=" + name);

        return this._rest.get(queryPath);
    }

    put(type, id, data) {
        return this._rest.put(this.restEndPoint + type + "/" + id, data);
    }

    post(path, data) {
        return this._rest.post(this.restEndPoint + path, data);
    }

    delete(type, id) {
        return this._rest.delete(this.restEndPoint + type + "/" + id);
    }
}

if (debug === true) {
    window._structrRest = new StructrRest();
}

