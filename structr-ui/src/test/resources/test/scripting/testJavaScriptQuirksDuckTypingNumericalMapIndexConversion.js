function test() {

	let x = {};

	x['24'] = 'jack bauer';

	$.log($.toJson(x));
}

let _structrMainResult = test();