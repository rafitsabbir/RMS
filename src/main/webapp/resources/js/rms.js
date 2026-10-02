/* RMS page behaviour, loaded by WEB-INF/tags/layout.tag. The JSPs have no inline scripts.
   Keep this file ASCII: the pages are ISO-8859-1 (G34). */
(function () {
	'use strict';

	// Lists and results: every table[data-rms-table] becomes a DataTable (only pages with tables="true" load it).
	// The paging labels are set as escapes because DataTables' own are raw UTF-8.
	if (window.DataTable) {
		document.querySelectorAll('table[data-rms-table]').forEach(function (table) {
			new DataTable(table, {
				// Column widths come from the browser and rms.css, not from DataTables' own measuring
				autoWidth: false,
				language: {
					paginate: { first: '\u00AB', previous: '\u2039', next: '\u203A', last: '\u00BB' }
				}
			});
		});
	}

	// Delete buttons: forms with data-rms-confirm ask before they post.
	// Uploads: a file over input[data-rms-maxbytes] isn't sent, because the server would refuse it anyway and a large
	// body can end in a reset connection. The server still checks the size.
	document.addEventListener('submit', function (event) {
		var form = event.target;
		var inputs = form.querySelectorAll('input[type=file][data-rms-maxbytes]');
		for (var i = 0; i < inputs.length; i++) {
			var limit = parseInt(inputs[i].getAttribute('data-rms-maxbytes'), 10);
			var file = inputs[i].files && inputs[i].files[0];
			if (file && file.size > limit) {
				window.alert(inputs[i].getAttribute('data-rms-toolarge'));
				event.preventDefault();
				return;
			}
		}
		var message = form.getAttribute('data-rms-confirm');
		if (message && !window.confirm(message)) {
			event.preventDefault();
		}
	});
})();
