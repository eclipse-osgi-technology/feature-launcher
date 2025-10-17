/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.eclipse.osgi.technology.featurelauncher.featureservice.base.external;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * 
 * Code from: org.apache.felix.cm.json.io.impl.JsonSupport;
 * 
 * Helper class to create a BufferedReader that implicitly removes inline and
 * blockcomments from the input
 */
public class CommentRemovingReader extends FilterReader {

	private boolean closed = false;
	private boolean insideComment = false;
	private boolean insideLineComment = false;
	private boolean insideString = false;
	private boolean isSkippedSlash = false;
	private char oldChar = 0; // priming with 0 as it is not part of comment or string escaping chars

	public CommentRemovingReader(Reader reader) {
		super(new BufferedReader(reader));
	}

	@Override
	public int read() throws IOException {
		return 0;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int charsRead = super.read(cbuf, off, len);
		if (charsRead > 0) {
			StringBuilder filteredContent = new StringBuilder();
			StringBuilder currentLine = new StringBuilder();

			for (int i = off; i < off + charsRead; i++) {
				char c = cbuf[i];

				// Detect String start/end if not inside a comment
				if (!insideComment && !insideLineComment) {
					if (c == '"') {
						// only flip if not escaped quotes
						if (oldChar != '\\') {
							insideString = !insideString;
						}
						currentLine.append(c);
						oldChar = c;
						continue;
					}
				}

				// Handle comments only if not inside a string
				if (!insideString) {
					// Detect potential start of a comment by detecting a slash
					if (!insideComment && !insideLineComment && c == '/') {
						// If the previous character was also a slash, we are inside a single-line
						// comment
						if (oldChar == '/') {
							insideLineComment = true;
							isSkippedSlash = false;
						} else {
							// skipping slash for verification if this is comment - will be ammended on next
							// char if non-comment
							isSkippedSlash = true;
						}
						oldChar = c;
						continue;
					}
					// Detect potential start of a multiline comment by detecting a star
					if (!insideComment && !insideLineComment && c == '*') {
						// If the previous character was also a slash, we are inside a multi-line
						// comment
						if (oldChar == '/') {
							insideComment = true;
							isSkippedSlash = false;
						} else {
							// otherwise this is not a comment, just a star
							currentLine.append(c);
						}
						oldChar = c;
						continue;
					}

					// if if we skipped over a / above and we're not within a comment, we need to
					// append the oldChar to the currentLine
					if (!insideComment && !insideLineComment && isSkippedSlash) {
						currentLine.append('/');
						isSkippedSlash = false;
					}

					// Detect potential end of a linecomment by detecting a newline
					if (insideLineComment && c == '\n') {
						insideLineComment = false;
						currentLine.append(c);
						oldChar = c;
						continue;
					}

					// Skip over characters inside single-line comments
					if (insideLineComment) {
						oldChar = c;
						continue;
					}

					// Detect potential end of a multiline comment by detecting a slash that is
					// preceded by a star
					if (insideComment && c == '/' && oldChar == '*') {
						insideComment = false;
						oldChar = c;
						continue;
					}

					// Skip over characters inside multi-line comments but preserve newlines
					if (insideComment) {
						if (c == '\n') {
							currentLine.append(c);
						}
						oldChar = c;
						continue;
					}
				}
				// Preserve characters outside comments
				if (!insideComment && !insideLineComment) {
					currentLine.append(c);
				}
				oldChar = c;
			}

			filteredContent.append(currentLine.toString());

			char[] filteredChars = filteredContent.toString().toCharArray();
			int filteredLen = Math.min(filteredChars.length, len);
			System.arraycopy(filteredChars, 0, cbuf, off, filteredLen);
			return filteredLen;

		}
		return charsRead;
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			closed = false;
			insideComment = false;
			insideLineComment = false;
			insideString = false;
			isSkippedSlash = false;
			in.close();
			super.close();
		}
	}
}