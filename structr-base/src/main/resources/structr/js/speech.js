/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
if ('webkitSpeechRecognition' in window) {

	var recognizing, prefix = '';
	var commands = {
		stop:                 ['speech recognition stop', 'speech recognition stopp', 'spracherkennung stop'],
		save:                 [prefix + 'save', prefix + 'safe', prefix + 'speichern'],
		saveAndClose:         [prefix + 'save and close', prefix + 'save and close', prefix + 'speichern und schließen'],
		close:                [prefix + 'close', prefix + 'schließen'],
		clearAll:             [prefix + 'clear all', prefix + 'delete all', prefix + 'alles löschen'],
		deleteLastParagraph:  [prefix + 'delete last paragraph', prefix + 'letzten\n\nlöschen'],
		deleteLastSentence:   [prefix + 'delete last sentence', prefix + 'letzten satz löschen'],
		deleteLastWord:       [prefix + 'delete last word', prefix + 'letztes wort löschen'],
		deleteLine:           [prefix + 'delete line', prefix + 'zeile löschen'],
		deleteLineLeft:       [prefix + 'delete line before cursor', prefix + 'zeile vor cursor löschen'],
		deleteLineRight:      [prefix + 'delete line after cursor', prefix + 'zeile hinter cursor löschen'],
		lineUp:               [prefix + 'line up', prefix + 'zeile hoch'],
		lineDown:             [prefix + 'line down', prefix + 'zeile runter'],
		wordLeft:             [prefix + 'word left', prefix + 'wort links'],
		wordRight:            [prefix + 'word right', prefix + 'wort rechts'],
		left:                 [prefix + 'left', prefix + 'links'],
		right:                [prefix + 'right', prefix + 'rechts']
	};

	var _Speech = {
		recognition: new webkitSpeechRecognition(),
		reset: function (btn) {
			_Speech.recognition.recognizing = false;
			btn.removeClass('active');
		},
		init: function (btn, callback) {
			_Speech.recognition.continuous = true;
			_Speech.recognition.interim = true;
			_Speech.reset(btn);
			_Speech.recognition.onend = _Speech.reset(btn);
			_Speech.recognition.onresult = function (event) {
				var finalResult = '';
				var interim = '';
				for (var i = 0; i < event.results.length; ++i) {
					if (event.results[i]['final']) {
						finalResult += event.results[i][0].transcript;
					} else {
						interim = event.results[i][0].transcript;
					}
				}
				callback(interim, finalResult);
			};

			btn.on('click', function () {
				_Speech.toggleStartStop(btn);
			});
		},
		toggleStartStop: function (btn, callback) {
			if (_Speech.recognition.recognizing) {
				_Speech.recognition.stop();
				_Speech.reset(btn);

				callback?.();
			} else {
				_Speech.recognition.start();
				_Speech.recognition.recognizing = true;
				btn.addClass('active');

				callback?.();
			}
		},
		isCommand: function (command, text) {
			return commands[command].some(function (command) {
				return (text.toLowerCase().trim() === command);
			});
		}
	};
}