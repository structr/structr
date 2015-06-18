self.addEventListener('message', function (e) {
	try {

		var file = e.data;
		var reader = new FileReaderSync();

		postMessage(reader.readAsArrayBuffer(file));

	} catch (e) {
		postMessage({
			result: 'error'
		});
	}
}, false);
