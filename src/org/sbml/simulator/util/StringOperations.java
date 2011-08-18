/*
    SBML2LaTeX converts SBML files (http://sbml.org) into LaTeX files.
    Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sbml.simulator.util;

import java.util.Arrays;
import java.util.Vector;

/**
 * @author Andreas Dr&auml;ger
 * 
 */
public class StringOperations {

	/**
	 * The file separator of the operating system.
	 */
	protected static final String fileSeparator = System
			.getProperty("file.separator");

	/**
	 * New line separator of this operating system
	 */
	protected static final String newLine = System
			.getProperty("line.separator");

	/**
	 * Returns a String who's first letter is now in upper case.
	 * 
	 * @param name
	 * @return
	 */
	public static String firstLetterUpperCase(String name) {
		char c = name.charAt(0);
		if (Character.isLetter(c))
			c = Character.toUpperCase(c);
		if (name.length() > 1)
			name = Character.toString(c) + name.substring(1);
		else
			return Character.toString(c);
		return name;
	}

	/**
	 * Retunrs a String who's first letter is now in lower case.
	 * 
	 * @param name
	 * @return
	 */
	public static String firstLetterLowerCase(String name) {
		char c = name.charAt(0);
		if (Character.isLetter(c))
			c = Character.toLowerCase(c);
		if (name.length() > 1)
			name = Character.toString(c) + name.substring(1);
		else
			return Character.toString(c);
		return name;
	}

	/**
	 * Returns the number as an English word. Zero is converted to "no". Only
	 * positive numbers from 1 to twelve can be converted. All other numbers are
	 * just converted to a String containing the number.
	 * 
	 * @param number
	 * @return
	 */
	public static String getWordForNumber(long number) {
		if ((number < Integer.MIN_VALUE) || (Integer.MAX_VALUE < number))
			return Long.toString(number);
		switch ((int) number) {
		case 0:
			return "no";
		case 1:
			return "one";
		case 2:
			return "two";
		case 3:
			return "three";
		case 4:
			return "four";
		case 5:
			return "five";
		case 6:
			return "six";
		case 7:
			return "seven";
		case 8:
			return "eight";
		case 9:
			return "nine";
		case 10:
			return "ten";
		case 11:
			return "eleven";
		case 12:
			return "twelve";
		default:
			return Long.toString(number);
		}
	}

	/**
	 * Returns the English name of a given month.
	 * 
	 * @param month
	 * @return
	 */
	public static String getMonthName(short month) {
		switch (month) {
		case 1:
			return "January";
		case 2:
			return "February";
		case 3:
			return "March";
		case 4:
			return "April";
		case 5:
			return "May";
		case 6:
			return "June";
		case 7:
			return "July";
		case 8:
			return "August";
		case 9:
			return "September";
		case 10:
			return "October";
		case 11:
			return "November";
		case 12:
			return "December";
		default:
			return "invalid month " + month;
		}
	}

	/**
	 * This method constructs a full length SBO number from a given SBO id.
	 * Whenever a SBO number is used in the model please don't forget to add
	 * this identifier to the Set of SBO numbers (only those numbers in this set
	 * will be displayed in the glossary).
	 * 
	 * @param sbo
	 * @return
	 */
	public static String getSBOnumber(int sbo) {
		String sboString = Integer.toString(sbo);
		while (sboString.length() < 7)
			sboString = '0' + sboString;
		return sboString;
	}

	/**
	 * 
	 * @param c
	 * @return True if the given character is a vocal and false if it is a
	 *         consonant.
	 */
	public static boolean isVocal(char c) {
		c = Character.toLowerCase(c);
		return (c == 'a') || (c == 'e') || (c == 'i') || (c == 'o')
				|| (c == 'u');
	}

	/**
	 * This method introduces left and right quotation marks where we normally
	 * have straight quotation marks.
	 * 
	 * @param text
	 * @param leftQuotationMark
	 * @param rightQuotationMark
	 * @return
	 */
	public static String correctQuotationMarks(String text,
			String leftQuotationMark, String rightQuotationMark) {
		boolean opening = true;
		for (int i = 0; i < text.length(); i++)
			if (text.charAt(i) == '"')
				if (opening) {
					text = text.substring(0, i - 1) + leftQuotationMark
							+ text.substring(i + 1);
					opening = false;
				} else {
					text = text.substring(0, i - 1) + rightQuotationMark
							+ text.substring(i + 1);
					opening = true;
				}
		return text;
	}

	/**
	 * Merges the two given arrays of Strings according to a lexicographic
	 * order.
	 * 
	 * @param n1
	 *            An array of strings
	 * @param n2
	 *            Another array of strings to be merged with the first argument.
	 * @return
	 */
	public static String[] merge(String[] n1, String... n2) {
		Arrays.sort(n1);
		Arrays.sort(n2);
		Vector<String> l = new Vector<String>();
		int i = 0, j = 0, lex;
		while ((i < n1.length) || (j < n2.length)) {
			if ((i < n1.length) && (j < n2.length)) {
				lex = n1[i].compareTo(n2[j]);
				l.add(lex <= 0 ? n1[i++] : n2[j++]);
				if (lex == 0)
					j++;
			} else
				l.add(i == n1.length ? n2[j++] : n1[i++]);
		}
		return l.toArray(new String[] {});
	}

	/**
	 * Creates a new String, in which all special characters within the given
	 * String are replaced with corresponding HTML codes and returns the result.
	 * 
	 * @param s
	 * @return
	 */
	public static String toUnicode(String s) {
		StringBuffer html = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
			case 'ä':
				html.append("\u00e4");
				break;
			case 'ö':
				html.append("\u00f6");
				break;
			case 'ü':
				html.append("\u00fc");
				break;
			case 'ß':
				html.append("\u00df");
				break;
			case 'Ä':
				html.append("\u00c4");
				break;
			case 'Ö':
				html.append("\u00d6");
				break;
			case 'Ü':
				html.append("\u00dc");
				break;
			default:
				html.append(s.charAt(i));
				break;
			}
		}
		return html.toString();
	}

	/**
	 * Returns the most similar String from a list of given Strings
	 * 
	 * @param s
	 * @param synonyms
	 * @return
	 */
	public static String getMostSimilarString(String s, String... synonyms) {
        int dist[] = new int[synonyms.length];
        int min = 0;
        for (int i =0; i< dist.length; i++) {
		  dist[i] = globalAlignment(s.toCharArray(), synonyms[i].toCharArray(), 2, 1);
		  if (dist[i] < dist[min])
			  min = i;
        }
		return synonyms[min];
	}

	/**
	 * 
	 * @param squery
	 * @param ssubject
	 * @param indel
	 * @param gapExt
	 * @return
	 */
	public static int globalAlignment(char squery[], char ssubject[],
			int indel, int gapExt) {
		int insert = indel, delete = indel, match = 0, replace = insert
				+ delete, i, j;
		int costMatrix[][] = new int[squery.length + 1][ssubject.length + 1]; // Matrix
		// CostMatrix

		/*
		 * Variables for the traceback
		 */
		StringBuffer[] align = { new StringBuffer(), new StringBuffer() };
		StringBuffer path = new StringBuffer();

		// construct the matrix:
		costMatrix[0][0] = 0;

		/*
		 * If we want to have affine gap penalties, we have to initialise
		 * additional matrices: If this is not necessary, we won't do that
		 * (because it's expensive).
		 */
		if ((gapExt != delete) || (gapExt != insert)) {

			int[][] E = new int[squery.length + 1][ssubject.length + 1]; // Inserts
			int[][] F = new int[squery.length + 1][ssubject.length + 1]; // Deletes

			E[0][0] = F[0][0] = Integer.MAX_VALUE; // Double.MAX_VALUE;
			for (i = 1; i <= squery.length; i++) {
				// costMatrix[i][0] = costMatrix[i-1][0] + delete;
				E[i][0] = Integer.MAX_VALUE; // Double.POSITIVE_INFINITY;
				costMatrix[i][0] = F[i][0] = delete + i * gapExt;
			}
			for (j = 1; j <= ssubject.length; j++) {
				// costMatrix[0][j] = costMatrix[0][j - 1] + insert;
				F[0][j] = Integer.MAX_VALUE; // Double.POSITIVE_INFINITY;
				costMatrix[0][j] = E[0][j] = insert + j * gapExt;
			}
			for (i = 1; i <= squery.length; i++)
				for (j = 1; j <= ssubject.length; j++) {
					E[i][j] = Math.min(E[i][j - 1], costMatrix[i][j - 1]
							+ insert)
							+ gapExt;
					F[i][j] = Math.min(F[i - 1][j], costMatrix[i - 1][j]
							+ delete)
							+ gapExt;
					costMatrix[i][j] = Functions.min(E[i][j], F[i][j],
							costMatrix[i - 1][j - 1]
									- ((squery[i-1] == ssubject[j-1]) ? -match
											: -replace));
				}
			/*
			 * Traceback for affine gap penalties.
			 */
			boolean[] gap_extend = { false, false };
			j = costMatrix[costMatrix.length - 1].length - 1;

			for (i = costMatrix.length - 1; i > 0;) {
				do {
					// only Insert.
					if (i == 0) {
						align[0].insert(0, '~');
						align[1].insert(0, ssubject[j--]);
						path.insert(0, ' ');

						// only Delete.
					} else if (j == 0) {
						align[0].insert(0, squery[--i]);
						align[1].insert(0, '~');
						path.insert(0, ' ');

						// Match/Replace
					} else if ((costMatrix[i][j] == costMatrix[i - 1][j - 1]
							- ((squery[i-1] == ssubject[j-1]) ? -match : -replace))
							&& !(gap_extend[0] || gap_extend[1])) {
						if (squery[i-1] == ssubject[j-1])
							path.insert(0, '|');
						else
							path.insert(0, ' ');
						align[0].insert(0, squery[--i]);
						align[1].insert(0, ssubject[--j]);

						// Insert || finish gap if extended gap is
						// opened
					} else if (costMatrix[i][j] == E[i][j] || gap_extend[0]) {
						// check if gap has been extended or freshly
						// opened
						gap_extend[0] = (E[i][j] != costMatrix[i][j - 1]
								+ insert + gapExt);

						align[0].insert(0, '-');
						align[1].insert(0, ssubject[--j]);
						path.insert(0, ' ');

						// Delete || finish gap if extended gap is
						// opened
					} else {
						// check if gap has been extended or freshly
						// opened
						gap_extend[1] = (F[i][j] != costMatrix[i - 1][j]
								+ delete + gapExt);

						align[0].insert(0, squery[--i]);
						align[1].insert(0, '-');
						path.insert(0, ' ');
					}
				} while (j > 0);
			}
			/*
			 * No affine gap penalties, constant gap penalties, which is much
			 * faster and needs less memory.
			 */
		} else {
			for (i = 1; i <= squery.length; i++)
				costMatrix[i][0] = costMatrix[i - 1][0] + delete;
			for (j = 1; j <= ssubject.length; j++)
				costMatrix[0][j] = costMatrix[0][j - 1] + insert;
			for (i = 1; i <= squery.length; i++)
				for (j = 1; j <= ssubject.length; j++) {
					costMatrix[i][j] = Functions.min(costMatrix[i - 1][j]
							+ delete, costMatrix[i][j - 1] + insert,
							costMatrix[i - 1][j - 1]
									- ((squery[i-1] == ssubject[j-1]) ? -match
											: -replace));
				}
			/*
			 * Traceback for constant gap penalties.
			 */
			j = costMatrix[costMatrix.length - 1].length - 1;
			for (i = costMatrix.length - 1; i > 0;) {
				do {
					// only Insert.
					if (i == 0) {
						align[0].insert(0, '~');
						align[1].insert(0, ssubject[j--]);
						path.insert(0, ' ');

						// only Delete.
					} else if (j == 0) {
						align[0].insert(0, squery[i--]);
						align[1].insert(0, '~');
						path.insert(0, ' ');

						// Match/Replace
					} else if (costMatrix[i][j] == costMatrix[i - 1][j - 1]
							- ((squery[i-1] == ssubject[j-1]) ? -match : -replace)) {

						if (squery[i-1] == ssubject[j-1])
							path.insert(0, '|');
						else
							path.insert(0, ' ');
						align[0].insert(0, squery[i--]);
						align[1].insert(0, ssubject[j--]);

						// Insert
					} else if (costMatrix[i][j] == costMatrix[i][j - 1]
							+ insert) {
						align[0].insert(0, '-');
						align[1].insert(0, ssubject[j--]);
						path.insert(0, ' ');

						// Delete
					} else {
						align[0].insert(0, squery[i--]);
						align[1].insert(0, '-');
						path.insert(0, ' ');
					}
				} while (j > 0);
			}
		}
		return costMatrix[costMatrix.length - 1][costMatrix[costMatrix.length - 1].length - 1];
	}

}
