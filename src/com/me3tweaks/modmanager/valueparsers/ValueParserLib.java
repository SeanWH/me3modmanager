package com.me3tweaks.modmanager.valueparsers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import com.me3tweaks.modmanager.valueparsers.biodifficulty.Stat;
import com.me3tweaks.modmanager.valueparsers.mpstorepack.SlotPool;

/**
 * ValueParserLib provides utility functions for things such as structs.
 * 
 * @author mgamerz
 *
 */
public class ValueParserLib {

	public static void main(String[] args) {
		String input = "((test1";
		System.out.println(getSplitValues(input).toString());
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static ArrayList<String> getSplitValues(String inputString) {
		try {
			ArrayList<String> values = new ArrayList<>();
			String workingStr = inputString;

			while (workingStr.length() > 2 && workingStr.charAt(1) == '(') {
				workingStr = workingStr.substring(1);
			}

			int charIndex = 0;
			int openBraces = 0;
			while (workingStr.length() > 0) {
				if (workingStr.charAt(charIndex) == '(') {
					openBraces++;
					charIndex++;
					continue;
				}
				if (workingStr.charAt(charIndex) == ')') {
					openBraces--;
					charIndex++;
					if (openBraces == 0) {
						//we finished one item
						values.add(workingStr.substring(0, charIndex));
						if (charIndex < workingStr.length()) {
							workingStr = workingStr.substring(charIndex + 1);
						} else {
							break;
						}
						charIndex = 0;
					} else if (openBraces < 0) {
						break;
					}
					continue;
				}
				//its none of the above 2
				charIndex++;
			}
			//category finished.

			return values;
		} catch (Exception e) {
			return null;
		}
	}

	public static String getStringProperty(String inputString, String propertyName, boolean isQuoted) {
		int charIndex = inputString.indexOf(propertyName);
		//System.out.println(inputString.charAt(charIndex - 1));
		if (charIndex > 0
				&& (inputString.charAt(charIndex - 1) == '(' || inputString.charAt(charIndex - 1) == ',' || inputString.charAt(charIndex - 1) == '"' || inputString
						.charAt(charIndex - 1) == ' ')) {
			//at least one instance was found.
			while (charIndex < inputString.length()) {
				String workingStr = inputString.substring(charIndex + propertyName.length());
				if (workingStr.charAt(0) == '=' || workingStr.charAt(1) == '=') { //next char, or after space char is =
					workingStr = workingStr.substring(workingStr.indexOf('=') + 1); //cut off =
					boolean startedWithParenthesis = workingStr.charAt(0) == '(';
					if (isQuoted) {
						workingStr = workingStr.substring(workingStr.indexOf('\"') + 1); //cut off " from quoted items.
					}
					//value is next.
					charIndex = 0;
					while (charIndex < workingStr.length()) {
						if (isQuoted) {
							if (workingStr.charAt(charIndex) == '\"') {
								return workingStr.substring(0, charIndex).trim();
							}
						} else {
							if (workingStr.charAt(charIndex) == ')') {
								if (startedWithParenthesis) {
									return workingStr.substring(0, charIndex+1).trim(); //+1 is 
								} else {
									return workingStr.substring(0, charIndex).trim();
								}
							} else if (workingStr.charAt(charIndex) == ',') {
								return  workingStr.substring(0, charIndex).trim();
							}
						}
						charIndex++;
					}
					System.out.println("DID NOT FIND TERMINATING CHAR.");

					break;
				} else {
					System.out.println(workingStr);
					return "nextchars were not =.";
				}

				//charIndex++;
				//if (inputString.charAt(charIndex) == '')
			}
		} else {
			return null;
		}
		return "derp";
	}

	public static int getIntProperty(String inputString, String propertyName) {
		int charIndex = inputString.indexOf(propertyName);
		if (charIndex > 0
				&& (inputString.charAt(charIndex - 1) == '(' || inputString.charAt(charIndex - 1) == ',' || inputString.charAt(charIndex - 1) == '"' || inputString
						.charAt(charIndex - 1) == ' ')) {
			//at least one instance was found.
			while (charIndex < inputString.length()) {
				String workingStr = inputString.substring(charIndex + propertyName.length());
				if (workingStr.charAt(0) == '=' || workingStr.charAt(1) == '=') { //next char, or after space char is =
					workingStr = workingStr.substring(workingStr.indexOf('=') + 1); //cut off =
					//value is next.
					charIndex = 0;
					while (charIndex < workingStr.length()) {
						if (workingStr.charAt(charIndex) == ')' || workingStr.charAt(charIndex) == ',') {
							if (charIndex <= 0) {
								return -50;
							}
							return Integer.parseInt(workingStr.substring(0, charIndex).trim());
						}
						charIndex++;
					}
					break;
				} else {
					return -1;
				}

				//charIndex++;
				//if (inputString.charAt(charIndex) == '')
			}
		} else {
			return -1;
		}
		return -1;
	}

	public static double getFloatProperty(String inputString, String propertyName) {
		int charIndex = inputString.indexOf(propertyName);
		if (charIndex > 0
				&& (inputString.charAt(charIndex - 1) == '(' || inputString.charAt(charIndex - 1) == ',' || inputString.charAt(charIndex - 1) == '"' || inputString
						.charAt(charIndex - 1) == ' ')) {
			//at least one instance was found.
			while (charIndex < inputString.length()) {
				String workingStr = inputString.substring(charIndex + propertyName.length());
				if (workingStr.charAt(0) == '=' || workingStr.charAt(1) == '=') { //next char, or after space char is =
					workingStr = workingStr.substring(workingStr.indexOf('=') + 1); //cut off =
					//value is next.
					charIndex = 0;
					while (charIndex < workingStr.length()) {
						if (workingStr.charAt(charIndex) == ')' || workingStr.charAt(charIndex) == ',') {
							if (charIndex <= 0) {
								return -40;
							}
							return Double.parseDouble(workingStr.substring(0, charIndex).trim());
						}
						charIndex++;
					}
					break;
				} else {
					return -1;
				}

				//charIndex++;
				//if (inputString.charAt(charIndex) == '')
			}
		} else {
			return -1;
		}
		return -1;
	}

	/**
	 * An object of type RomanNumeral is an integer between 1 and 3999. It can
	 * be constructed either from an integer or from a string that represents a
	 * Roman numeral in this range. The function toString() will return a
	 * standardized Roman numeral representation of the number. The function
	 * toInt() will return the number as a value of type int.
	 */
	public static class RomanNumeral {

		private final int num; // The number represented by this Roman numeral.

		/*
		 * The following arrays are used by the toString() function to construct
		 * the standard Roman numeral representation of the number. For each i,
		 * the number numbers[i] is represented by the corresponding string,
		 * letters[i].
		 */

		private static final int[] numbers = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };

		private static final String[] letters = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };

		/**
		 * Constructor. Creates the Roman number with the int value specified by
		 * the parameter. Throws a NumberFormatException if arabic is not in the
		 * range 1 to 3999 inclusive.
		 */
		public RomanNumeral(int arabic) {
			if (arabic < 1)
				throw new NumberFormatException("Value of RomanNumeral must be positive.");
			if (arabic > 3999)
				throw new NumberFormatException("Value of RomanNumeral must be 3999 or less.");
			num = arabic;
		}

		/*
		 * Constructor. Creates the Roman number with the given representation.
		 * For example, RomanNumeral("xvii") is 17. If the parameter is not a
		 * legal Roman numeral, a NumberFormatException is thrown. Both upper
		 * and lower case letters are allowed.
		 */
		public RomanNumeral(String roman) {

			if (roman.length() == 0)
				throw new NumberFormatException("An empty string does not define a Roman numeral.");

			roman = roman.toUpperCase(); // Convert to upper case letters.

			int i = 0; // A position in the string, roman;
			int arabic = 0; // Arabic numeral equivalent of the part of the string that has
							//    been converted so far.

			while (i < roman.length()) {

				char letter = roman.charAt(i); // Letter at current position in string.
				int number = letterToNumber(letter); // Numerical equivalent of letter.

				i++; // Move on to next position in the string

				if (i == roman.length()) {
					// There is no letter in the string following the one we have just processed.
					// So just add the number corresponding to the single letter to arabic.
					arabic += number;
				} else {
					// Look at the next letter in the string.  If it has a larger Roman numeral
					// equivalent than number, then the two letters are counted together as
					// a Roman numeral with value (nextNumber - number).
					int nextNumber = letterToNumber(roman.charAt(i));
					if (nextNumber > number) {
						// Combine the two letters to get one value, and move on to next position in string.
						arabic += (nextNumber - number);
						i++;
					} else {
						// Don't combine the letters.  Just add the value of the one letter onto the number.
						arabic += number;
					}
				}

			} // end while

			if (arabic > 3999)
				throw new NumberFormatException("Roman numeral must have value 3999 or less.");

			num = arabic;

		} // end constructor

		/**
		 * Find the integer value of letter considered as a Roman numeral.
		 * Throws NumberFormatException if letter is not a legal Roman numeral.
		 * The letter must be upper case.
		 */
		private int letterToNumber(char letter) {
			switch (letter) {
			case 'I':
				return 1;
			case 'V':
				return 5;
			case 'X':
				return 10;
			case 'L':
				return 50;
			case 'C':
				return 100;
			case 'D':
				return 500;
			case 'M':
				return 1000;
			default:
				throw new NumberFormatException("Illegal character \"" + letter + "\" in Roman numeral");
			}
		}

		/**
		 * Return the standard representation of this Roman numeral.
		 */
		public String toString() {
			String roman = ""; // The roman numeral.
			int N = num; // N represents the part of num that still has
							//   to be converted to Roman numeral representation.
			for (int i = 0; i < numbers.length; i++) {
				while (N >= numbers[i]) {
					roman += letters[i];
					N -= numbers[i];
				}
			}
			return roman;
		}

		/**
		 * Return the value of this Roman numeral as an int.
		 */
		public int toInt() {
			return num;
		}
	}

	public static boolean getBooleanProperty(String inputString, String propertyName, boolean defaultVal) {
		int charIndex = inputString.indexOf(propertyName);
		if (charIndex > 0
				&& (inputString.charAt(charIndex - 1) == '(' || inputString.charAt(charIndex - 1) == ',' || inputString.charAt(charIndex - 1) == '"' || inputString
						.charAt(charIndex - 1) == ' ')) {
			//at least one instance was found.
			while (charIndex < inputString.length()) {
				String workingStr = inputString.substring(charIndex + propertyName.length());
				if (workingStr.charAt(0) == '=' || workingStr.charAt(1) == '=') { //next char, or after space char is =
					workingStr = workingStr.substring(workingStr.indexOf('=') + 1); //cut off =
					//value is next.
					charIndex = 0;
					while (charIndex < workingStr.length()) {
						if (workingStr.charAt(charIndex) == ')' || workingStr.charAt(charIndex) == ',') {
							String valueStr = workingStr.substring(0, charIndex).trim();
							if (valueStr.toLowerCase().startsWith("true")) {
								return true;
							}
							if (valueStr.toLowerCase().startsWith("false")) {
								return false;
							}
							System.err.println("UNKNOWN BOOLEAN PROP VAL: " + valueStr);
							return defaultVal;
						}
						charIndex++;
					}
					break;
				} else {
					return defaultVal;
				}

				//charIndex++;
				//if (inputString.charAt(charIndex) == '')
			}
		} else {
			return defaultVal;
		}
		return defaultVal;
	}

}
