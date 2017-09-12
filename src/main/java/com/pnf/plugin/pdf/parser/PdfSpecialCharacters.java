/*
Copyright PNF Software, Inc.

    https://www.pnfsoftware.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.pnf.plugin.pdf.parser;

import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfSpecialCharacters {

    public static final byte[] EMPTY_ARRAY = {};

    public static final byte[] OBJ_START_SEPARATOR = {0x6F, 0x62, 0x6A};
    public static final byte[] OBJ_END_SEPARATOR = {0x65, 0x6E, 0x64, 0x6F, 0x62, 0x6A};

    public static final byte[] STREAM_START_SEPARATOR = {0x73, 0x74, 0x72, 0x65, 0x61, 0x6D};
    public static final byte[] STREAM_END_SEPARATOR = {0x65, 0x6E, 0x64, 0x73, 0x74, 0x72, 0x65, 0x61, 0x6D};

    public static final byte[] TRAILER_START_SEPARATOR = "trailer".getBytes();

    public static final byte[] XREF_START_SEPARATOR = "startxref".getBytes();

    public static final byte[] XREF_SEPARATOR = "xref".getBytes();

    public static final byte[] PDF_HEADER = "%PDF".getBytes();

    public static final byte[] EOF = "%%EOF".getBytes();

    private static final ILogger logger = GlobalLog.getLogger(PdfSpecialCharacters.class);

    public static boolean isSeparator(byte[] data, int cursor) {
        if(cursor >= data.length) {
            return true;
        }
        return isSeparator(data[cursor]);
    }

    public static boolean isSeparator(byte b) {
        return b == 0x00 || b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D || b == 0x20;
    }

    public static int jumpSeparators(byte[] data, int cursor) {
        while(isSeparator(data, cursor) && cursor < data.length) {
            cursor++;
        }
        return cursor;
    }

    public static int testEndLine(byte[] data, int cursor) {
        if(cursor >= data.length) {
            return 0;
        }
        if(data[cursor] == 0x0A) {
            return 1;
        }
        else if(data[cursor] == 0x0D) {
            if(cursor + 1 >= data.length) {
                return 1; // end of stream
            }
            else if(data[cursor + 1] == 0x0A) {
                return 2;
            }
            else {
                return 1;
            }
        }
        return -1;
    }

    public static int testPreviousEndLine(byte[] data, int cursor) {
        if(cursor < 0) {
            return 0;
        }
        if(data[cursor] == 0x0A) {
            if(cursor == 0) {
                return 1; // beginning of stream
            }
            else if(data[cursor - 1] == 0x0D) {
                return 2;
            }
            else {
                return 1;
            }
        }
        if(data[cursor] == 0x0D) {
            return 1;
        }
        return -1;
    }

    public static boolean isChar(byte[] data, int cursor, char expected) {
        if(cursor >= data.length) {
            return false;
        }
        return (char)data[cursor] == expected;
    }

    public static boolean isEndComment(byte[] data, int cursor) {
        if(cursor >= data.length) {
            return true;
        }
        return isEndComment(data[cursor]);
    }

    public static boolean isEndComment(byte b) {
        return isEndLine(b) || b == 0x00 || b == 0x0C;
    }

    public static boolean isEndLine(byte b) {
        return b == 0x0A || b == 0x0D;
    }

    public static boolean isDelimitorOrSeparator(byte[] data, int cursor) {
        if(cursor >= data.length) {
            return true;
        }
        return isDelimitor(data[cursor]) || isSeparator(data[cursor]);
    }

    public static boolean isDelimitor(byte b) {
        return b == '(' || b == ')' || b == '<' || b == '>' || b == '[' || b == ']' || b == '{' || b == '}' || b == '/'
                || b == '%';
    }

    public static boolean isComment(byte b) {
        return b == '%';
    }

    public static boolean isStartObj(byte[] data, int i) {
        return isObj(data, i, OBJ_START_SEPARATOR, true);
    }

    public static boolean isEndObj(byte[] data, int i) {
        return isObj(data, i, OBJ_END_SEPARATOR, true);
    }

    public static boolean isStartStream(byte[] data, int i) {
        return isObj(data, i, STREAM_START_SEPARATOR, true);
    }

    public static boolean isEndStream(byte[] data, int i) {
        return isObj(data, i, STREAM_END_SEPARATOR, false);
    }

    public static boolean isStartTrailer(byte[] data, int i) {
        return isObj(data, i, TRAILER_START_SEPARATOR, true);
    }

    public static boolean isStartXref(byte[] data, int i) {
        return isObj(data, i, XREF_START_SEPARATOR, true);
    }

    public static boolean isXref(byte[] data, int i) {
        return isObj(data, i, XREF_SEPARATOR, true);
    }

    public static boolean isEOF(byte[] data, int i) {
        return isObj(data, i, EOF, true);
    }

    private static boolean isObj(byte[] data, int i, byte[] objSeparator, boolean previousCharShallBeSeparator) {
        if(i + objSeparator.length > data.length) {
            return false;
        }
        for(int j = 0; j < objSeparator.length; j++) {
            if(data[i + j] != objSeparator[j]) {
                return false;
            }
        }
        if(previousCharShallBeSeparator && i > 0 && !isDelimitorOrSeparator(data, i - 1)) {
            // the previous char in not separator => this is not the expected token
            return false;
        }
        if(i + objSeparator.length <= data.length || isDelimitorOrSeparator(data, i + objSeparator.length)) {
            return true;
        }
        logger.error("%s is not at index %x", new String(objSeparator), i);
        return false;
    }

}
