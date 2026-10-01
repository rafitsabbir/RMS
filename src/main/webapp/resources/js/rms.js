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
	document.addEventListener('submit', function (event) {
		var message = event.target.getAttribute('data-rms-confirm');
		if (message && !window.confirm(message)) {
			event.preventDefault();
		}
	});
})();
