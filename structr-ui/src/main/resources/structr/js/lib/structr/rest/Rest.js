'use strict';

export class Rest {

    constructor() {
        this.config = {};
    }

    async get(url) {

        let response = (await fetch(url, {
            headers: this.headers(),
            method: 'GET',
            credentials: 'same-origin'
        }));


        this.handleStatusCode(response.status);

        return response.json();
    }

    async put(url, data) {

        let response = (await fetch(url, {
            headers: this.headers(),
            method: 'PUT',
            body: JSON.stringify(data),
            credentials: 'same-origin',
        }));

        this.handleStatusCode(response.status);

        return response.json();
    }

    async post(url, data) {

        let response = (await fetch(url, {
            headers: this.headers(),
            method: 'POST',
            body: JSON.stringify(data),
            credentials: 'same-origin'
        }));

        this.handleStatusCode(status);

        return response.json();
    }

    async delete(url, data) {

        let response = (await fetch(url, {
            headers: this.headers(),
            method: 'DELETE',
            body: JSON.stringify(data),
            credentials: 'same-origin'
        }));

        this.handleStatusCode(status);

        return response.json();
    }

    headers() {
        let result = new Headers();
        //result.set("X-User", this.config['user']);
        //result.set("X-Password", this.config['password']);
        return result;
    }

    handleStatusCode(status) {
        switch (status) {
            case 401:
                alert('Unauthorized. Make sure your session has not expired.');
                throw 'Unauthorized. Make sure your session has not expired.';
        }
    }
}

