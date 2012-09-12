/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * For more details and the latest version of this code please see
 * http://paninij.org
 * 
 * Contributor(s): Hridesh Rajan
 */

/*** 
 * Classic KWIC system using the Panini language 
 * 
 * This implementation of the KWIC system is based on the example 
 * presented in the following paper. 
 * D. L. Parnas. 1972. On the criteria to be used in decomposing systems 
 * into modules. Commun. ACM 15, 12 (December 1972), 1053-1058. 
 * DOI=10.1145/361598.361623 http://doi.acm.org/10.1145/361598.361623
 * 
 */

/**
 * LineStorage holds a number of lines and provides a number of public methods
 * to manipulate the lines. A line is defined as a set of words, and a word
 * consists of a number of characters. Methods defined by the LineStorage class
 * allow objects of other classes to:
 * <ul>
 * <li>set, read and delete a character from a particular word in a particular
 * line
 * <li>add a new character to a particular word in a particular line
 * <li>obtain the number of characters in a particular word in a particular line
 * <li>set, read and delete a word from a particular line
 * <li>add a new word to a particular line
 * <li>add an empty word to a particular line
 * <li>obtain words count in a particular line
 * <li>set, read and delete a particular line
 * <li>add a new line
 * <li>add an empty line
 * <li>obtain lines count
 * </ul>
 * 
 * @author dhelic
 * @version $Id$
 */

module LineStorage () {
	include Types;
	include java.util.ArrayList;

	/**
	 * ArrayList holding all lines. Each line itself is represeneted as an
	 * Arraylist object holding all words from that line. The ArrayList class is a
	 * standard Java Collection class, which implements the typical buffer
	 * functionality, i.e., it keeps its objects in an array of a fix capacity.
	 * When the current capacity is exceeded, ArrayList object resizes its array
	 * automatically, and copies the elements of the old array into the new one.
	 */
	ArrayList lines_ = new ArrayList();

	/**
	 * This method sets a new character on the specified index of a particular word
	 * in a particular line.
	 * 
	 * @param c
	 *         new character
	 * @param position
	 *         character index in the word
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @see #getChar
	 * @see #addChar
	 * @see #deleteChar
	 */
	void setChar(char c, int position, int word, int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		String current_word = (String) current_line.get(word);
		char[] chars = current_word.toCharArray();
		chars[position] = c;
		current_word = new String(chars);
		current_line.set(word, current_word);
	}

	/**
	 * Gets the character from the specified position in the specified word in a
	 * particular line.
	 * 
	 * @param position
	 *         character index in the word
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @return char
	 * @see #setChar
	 * @see #addChar
	 * @see #deleteChar
	 */
	CharC getChar(int position, int word, int line) {
		return ((String) ((ArrayList) lines_.get(line)).get(word)).charAt(position);
	}

	/**
	 * Adds a character at the end of the specified word in a particular line.
	 * 
	 * @param c
	 *         new character
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @see #setChar
	 * @see #getChar
	 * @see #deleteChar
	 */
	void addChar(char c, int word, int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		String current_word = (String) current_line.get(word);
		char[] chars = new char[current_word.length() + 1];
		current_word.getChars(0, chars.length - 1, chars, 0);
		chars[chars.length - 1] = c;
		current_word = new String(chars);
		current_line.set(word, current_word);
	}

	/**
	 * Deletes the character from the specified position in the specified word in a
	 * particular line.
	 * 
	 * @param position
	 *         character index in the word
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @see #setChar
	 * @see #getChar
	 * @see #addChar
	 */
	void deleteChar(int position, int word, int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		String current_word = (String) current_line.get(word);
		char[] chars = new char[current_word.length() - 1];
		current_word.getChars(0, position, chars, 0);
		current_word.getChars(position + 1, chars.length + 1, chars, position);
		current_word = new String(chars);
		current_line.set(word, current_word);
	}

	/**
	 * Gets the number of characters in this particular word.
	 * 
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @return int
	 */
	IntegerC getCharCount(int word, int line) {
		return ((String) ((ArrayList) lines_.get(line)).get(word)).length();
	}

	/**
	 * This method sets a new word on the specified index of a particular line.
	 * Character array is taken as an argument for the word.
	 * 
	 * @param chars
	 *         new word
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @see #getWord
	 * @see #addWord
	 * @see #addEmptyWord
	 * @see #deleteWord
	 */
	void setWord(char[] chars, int word, int line) {
		setWord(new String(chars), word, line);
	}

	/**
	 * This method sets a new word on the specified index of a particular line.
	 * String is taken as an argument for the word.
	 * 
	 * @param chars
	 *         new word
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @see #getWord
	 * @see #addWord
	 * @see #addEmptyWord
	 * @see #deleteWord
	 */
	void setWordToString(String chars, int word, int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		current_line.set(word, chars);
	}

	/**
	 * Gets the word from the specified position in a particular line String
	 * representing the word is returned.
	 * 
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @return String
	 * @see #setWord
	 * @see #addWord
	 * @see #addEmptyWord
	 * @see #deleteWord
	 */
	StringC getWord(int word, int line) {
		return (String) ((ArrayList) lines_.get(line)).get(word);
	}

	/**
	 * Adds a word at the end of the specified line. The method takes a character
	 * array as an argument.
	 * 
	 * @param chars
	 *         new word
	 * @param line
	 *         line index
	 * @see #addEmptyWord
	 * @see #setWord
	 * @see #getWord
	 * @see #deleteWord
	 */
	void addWord(char[] chars, int line) {
		addWord(new String(chars), line);
	}

	/**
	 * Adds a word at the end of the specified line. The method takes a string as
	 * an argument.
	 * 
	 * @param chars
	 *         new word
	 * @param line
	 *         line index
	 * @see #addEmptyWord
	 * @see #setWord
	 * @see #getWord
	 * @see #deleteWord
	 */
	void addWordInString(String chars, int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		current_line.add(chars);
	}

	/**
	 * Adds an empty word at the end of the specified line.
	 * 
	 * @param line
	 *         line index
	 * @see #setWord
	 * @see #getWord
	 * @see #addWord
	 * @see #deleteWord
	 */
	void addEmptyWord(int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		current_line.add(new String());
	}

	/**
	 * Deletes the word from the specified position in a particular line.
	 * 
	 * @param word
	 *         word index in the line
	 * @param line
	 *         line index
	 * @see #setWord
	 * @see #getWord
	 * @see #addWord
	 * @see #addEmptyWord
	 */
	void deleteWord(int word, int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		current_line.remove(word);
	}

	/**
	 * Gets the number of words in this particular line.
	 * 
	 * @param line
	 *         line index
	 * @return int
	 */
	IntegerC getWordCount(int line) {
		return ((ArrayList) lines_.get(line)).size();
	}

	/**
	 * This method sets a new line on the specified index. This method takes two
	 * dimensional character array as an argument for the line.
	 * 
	 * @param words
	 *         new line
	 * @param line
	 *         line index
	 * @see #getLine
	 * @see #getLineAsString
	 * @see #addLine
	 * @see #addEmptyLine
	 * @see #deleteLine
	 */
	void setLine(char[][] words, int line) {
		String[] tmp = new String[words.length];
		for (int i = 0; i < words.length; i++)
			tmp[i] = new String(words[i]);
		setLine(tmp, line);
	}

	/**
	 * This method sets a new line on the specified index. This method takes a
	 * string array as argument
	 * 
	 * @param words
	 *         new line
	 * @param line
	 *         line index
	 * @see #getLine
	 * @see #getLineAsString
	 * @see #addLine
	 * @see #addEmptyLine
	 * @see #deleteLine
	 */
	void setLineToWords(String[] words, int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		current_line.clear();
		for (int i = 0; i < words.length; i++)
			current_line.add(words[i]);
	}

	/**
	 * Gets the line from the specified position. String array representing the
	 * line is returned.
	 * 
	 * @param line
	 *         line index
	 * @return String[]
	 * @see #setLine
	 * @see #getLineAsString
	 * @see #addLine
	 * @see #addEmptyLine
	 * @see #deleteLine
	 */
	Strings getLine(int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		String[] tmp = new String[current_line.size()];
		for (int i = 0; i < tmp.length; i++)
			tmp[i] = (String) current_line.get(i);
		return new Strings(tmp);
	}

	/**
	 * Gets the line from the specified position. A single String representing the
	 * line is returned.
	 * 
	 * @param line
	 *         line index
	 * @return String
	 * @see #setLine
	 * @see #getLine
	 * @see #addLine
	 * @see #addEmptyLine
	 * @see #deleteLine
	 */
	StringC getLineAsString(int line) {
		ArrayList current_line = (ArrayList) lines_.get(line);
		int size = current_line.size();

		int length = 0;
		for (int i = 0; i < size; i++)
			length += getWord(i, line).length();
		length += size - 1;

		char[] tmp = new char[length];
		int count = 0;
		for (int i = 0; i < size; i++) {
			getWord(i, line).getChars(0, getWord(i, line).length(), tmp, count);
			count += getWord(i, line).length();
			if (i != (size - 1))
				tmp[count++] = ' ';
		}

		return new String(tmp);
	}

	/**
	 * Adds a line at the end of the line array. Two dimensional array is the
	 * argument for the new line
	 * 
	 * @param words
	 *         new line
	 * @see #addEmptyLine
	 * @see #setLine
	 * @see #getLine
	 * @see #deleteLine
	 */
	void addCharsToLine(char[][] words) {
		String[] tmp = new String[words.length];
		for (int i = 0; i < words.length; i++)
			tmp[i] = new String(words[i]);
		addLine(tmp);
	}

	/**
	 * Adds a line at the end of the line array. String array is the argument for
	 * the new line
	 * 
	 * @param words
	 *         new line
	 * @see #addEmptyLine
	 * @see #setLine
	 * @see #getLine
	 * @see #deleteLine
	 */
	void addLine(String[] words) {
		ArrayList current_line = new ArrayList();
		for (int i = 0; i < words.length; i++)
			current_line.add(words[i]);
		lines_.add(current_line);
	}

	/**
	 * Adds an empty line at the end of the lines array.
	 * 
	 * @see #setLine
	 * @see #getLine
	 * @see #getLineAsString
	 * @see #addLine
	 * @see #deleteLine
	 */
	void addEmptyLine() {
		ArrayList current_line = new ArrayList();
		lines_.add(current_line);
	}

	/**
	 * Deletes the line from the specified position.
	 * 
	 * @param line
	 *         line index
	 * @see #setLine
	 * @see #getLine
	 * @see #getLineAsString
	 * @see #addLine
	 * @see #addEmptyLine
	 */
	void deleteLine(int line) {
		lines_.remove(line);
	}

	/**
	 * Gets the number of lines.
	 * 
	 * @return int
	 */
	IntegerC getLineCount() {
		return lines_.size();
	}
}