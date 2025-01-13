package wuan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static wuan.TokenType.*;

class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private static final Map<String, TokenType> keywords;

	private int start = 0;
	private int current = 0;
	private int line = 1;

	static {
		keywords = new HashMap<>();
		keywords.put("and", AND);
		keywords.put("class", CLASS);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("for", FOR);
		keywords.put("fun", FUN);
		keywords.put("if", IF);
		keywords.put("nil", NIL);
		keywords.put("or", OR);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("super", SUPER);
		keywords.put("this", THIS);
		keywords.put("true", TRUE);
		keywords.put("var", VAR);
		keywords.put("while", WHILE);
	}

	Scanner(String source) {
		this.source = source;
	}

	/**
	 * The scanner will work its way through the source code, adding tokens
	 * until it runs out of characters. Once it has ran out of characters, it
	 * appends one final "end of file" token.
	 *
	 * @return A list of tokens that have been scanned
	 */
	List<Token> scanTokens() {
		while (!isAtEnd()) {
			// We are at the beginning of the next lexeme.
			start = current;
			scanToken();
		}

		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}

	/**
	 * Goes through each individual token and will add them to our list of tokens
	 * if they are valid tokens.
	 */
	private void scanToken() {
		char c = advance();
		switch (c) {
			case '(':
				addToken(LEFT_PAREN);
				break;
			case ')':
				addToken(RIGHT_PAREN);
				break;
			case '{':
				addToken(LEFT_BRACE);
				break;
			case '}':
				addToken(RIGHT_BRACE);
				break;
			case ',':
				addToken(COMMA);
				break;
			case '.':
				addToken(DOT);
				break;
			case '-':
				addToken(MINUS);
				break;
			case '+':
				addToken(PLUS);
				break;
			case ';':
				addToken(SEMICOLON);
				break;
			case '*':
				addToken(STAR);
				break;
			case '!':
				addToken(match('=') ? BANG_EQUAL : BANG);
				break;
			case '=':
				addToken(match('=') ? EQUAL_EQUAL : EQUAL);
				break;
			case '<':
				addToken(match('=') ? LESS_EQUAL : LESS);
				break;
			case '>':
				addToken(match('=') ? GREATER_EQUAL : GREATER);
				break;
			case '/':
				if (match('/')) {
					// A comment goes until the end of line.
					while (peek() != '\n' && !isAtEnd())
						advance();
				} else if (match('*')) {
					skipBlockComment();
				} else {
					addToken(SLASH);
				}
				break;
			case ' ':
			case '\r':
			case '\t':
				// Ignore whitespace.
				break;
			case '\n':
				line++;
				break;
			case '"':
				string();
				break;
			default:
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) {
					identifier();
				} else {
					Wuan.error(line, "Unexpected character.");
				}
				break;
		}
	}

	private void identifier() {
		while (isAlphaNumeric(peek()))
			advance();

		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		if (type == null)
			type = IDENTIFIER;
		addToken(type);
	}

	/**
	 * Goes through a number literal to and checks to see if it is an integer
	 * or float before adding it to the list of tokens.
	 */
	private void number() {
		while (isDigit(peek()))
			advance();

		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();

			while (isDigit(peek()))
				advance();
		}

		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}

	/**
	 * Goes through a string and adds everything inside the quotes (not including
	 * the quotes) to a token.
	 */
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n')
				line++;
			advance();
		}

		if (isAtEnd()) {
			Wuan.error(line, "Unterminated string.");
			return;
		}

		// The closing ".
		advance();

		// Trim the surrounding quotes.
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
	}

	/**
	 * Check to see if the current character is what we are looking for. We can
	 * recognize the lexemes in two stages. (Example: If the lexeme starts with
	 * '!', we then look at the next character for a '=' to see if it can form a
	 * '!='.)
	 *
	 * @param expected - The character that could come next in the lexeme
	 * @return true if a match was found and false if it wasn't
	 */
	private boolean match(char expected) {
		if (isAtEnd())
			return false;
		if (source.charAt(current) != expected)
			return false;

		current++;
		return true;
	}

	/**
	 * Similar to the advance() function, but doesn't consume the character. This
	 * is called a lookahead. Since it only looks at the current unconsumed
	 * character, we have one character of lookahead.
	 *
	 * @return The current character
	 */
	private char peek() {
		if (isAtEnd())
			return '\0';
		return source.charAt(current);
	}

	/**
	 * Looks ahead two characters.
	 *
	 * @return The character two places ahead or the if we have reached the end,
	 *         '\0' (NULL)
	 */
	private char peekNext() {
		if (current + 1 >= source.length())
			return '\0';
		return source.charAt(current + 1);
	}

	/**
	 * Skips C like block comments and handles nested block comments.
	 */
	private void skipBlockComment() {
		int nesting = 1;
		while (nesting > 0) {
			if (peek() == '\0') {
				Wuan.error(line, "Unterminated block comment.");
				return;
			}

			// Enter block comment
			if (peek() == '/' && peekNext() == '*') {
				advance();
				advance();
				nesting++;
				continue;
			}

			// Exit block comment
			if (peek() == '*' && peekNext() == '/') {
				advance();
				advance();
				nesting--;
				continue;
			}

			// Regular comment character.
			advance();
		}
	}

	/**
	 * Check to see if the character is a lowercase/uppercase letter or an
	 * underscore.
	 * 
	 * @param c - A character
	 * @return true if the character is a lowercase/uppercase letter or an
	 *         underscore,
	 *         false otherwise.
	 */
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z') ||
				c == '_';
	}

	/**
	 * @param c - A character
	 * @return true if the character is a number, lowercase/uppercase letter, or
	 *         underscore.
	 */
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	/**
	 * Check to see if the current character is a digit/number.
	 *
	 * @param c - A character
	 * @return true if it is in the range of 0-9 and false if it isn't
	 */
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * Helper function to tell if all of the characters have been consumed.
	 *
	 * @return true if we have gone past the source's length and false otherwise
	 */
	private boolean isAtEnd() {
		return current >= source.length();
	}

	/**
	 * Consumes the next character in the source file and returns it.
	 *
	 * @return The next character in the source file.
	 */
	private char advance() {
		return source.charAt(current++);
	}

	/**
	 * Grabs the text of the current lexeme and creates a new token for it.
	 *
	 * @param type - Type of token to add
	 */
	private void addToken(TokenType type) {
		addToken(type, null);
	}

	/**
	 * Overload of addToken() used to handle tokens with literal values.
	 *
	 * @param type    - Type of token to add
	 * @param literal - IDENTIFIER, STRING, NUMBER
	 */
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}

}
