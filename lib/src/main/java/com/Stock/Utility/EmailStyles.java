package com.Stock.Utility;

/**
 * Shared HTML/CSS building blocks for the report emails. The large inline
 * {@code <style>} block plus the opening {@code <table>}/{@code <thead>} markup
 * used to be copy-pasted verbatim into every publisher class.
 */
public class EmailStyles {

	/**
	 * The full {@code <style>} block followed by the opening
	 * {@code <html><body><table ...><thead>} markup, shared by every report.
	 */
	public static final String TABLE_STYLE = "<style>\r\n"
			+ "* {\r\n"
			+ "  font-family: sans-serif; /* Change your font family */\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table {\r\n"
			+ "  border-collapse: collapse;\r\n"
			+ "  margin: 25px 0;\r\n"
			+ "  font-size: 0.9em;\r\n"
			+ "  min-width: 400px;\r\n"
			+ "  border-radius: 5px 5px 0 0;\r\n"
			+ "  overflow: hidden;\r\n"
			+ "  box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table thead tr {\r\n"
			+ "  background-color: #009879;\r\n"
			+ "  color: #ffffff;\r\n"
			+ "  text-align: left;\r\n"
			+ "  font-weight: bold;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table th,\r\n"
			+ ".content-table td {\r\n"
			+ "  padding: 12px 15px;\r\n"
			+ " border-left: 1px solid #a9a9a9;\r\n"
			+ " border-right: 1px solid #a9a9a9;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr {\r\n"
			+ "  border-bottom: 1px solid #dddddd;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr:nth-of-type(even) {\r\n"
			+ "  background-color: #f3f3f3;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr:last-of-type {\r\n"
			+ "  border-bottom: 2px solid #a9a9a9;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ ".content-table tbody tr.active-row {\r\n"
			+ "  font-weight: bold;\r\n"
			+ "  color: #a9a9a9;\r\n"
			+ "}\r\n"
			+ ""
			+ "td,tr{\r\n"
			+ " height : 2px;\r\n"
			+ " border-collapse:collapse;\r\n"
			+ " border:1px solid #a9a9a9;\r\n"
			+ " border-right : 1px solid #a9a9a9;\r\n"
			+ " border-left: 1px solid #a9a9a9;\r\n"
			+ "\r\n"
			+ "</style>\r\n"
			+ "<html><body><table class='content-table' cellspacing='0'><thead>";

	/**
	 * Builds the header row for the report table: {@link #TABLE_STYLE} followed by
	 * a {@code <tr>} of {@code <th>} cells for each supplied column heading.
	 */
	public static String tableHeader(String... columns) {
		StringBuilder sb = new StringBuilder(TABLE_STYLE).append("<tr>");
		for (String column : columns) {
			sb.append("<th>").append(column).append("</th>");
		}
		return sb.append("</tr></thead>").toString();
	}
}
