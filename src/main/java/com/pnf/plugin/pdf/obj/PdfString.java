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

package com.pnf.plugin.pdf.obj;

import java.util.HashMap;
import java.util.Map;

import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnf.plugin.pdf.parser.PdfSpecialCharacters;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.decrypt.PDFDecrypter;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfString extends AbstractPdfParsableAttribute {

    private static final Map<Character, Character> escapedCharacters = new HashMap<Character, Character>();

    static {
        escapedCharacters.put('n', (char)0x0A);
        escapedCharacters.put('r', (char)0x0D);
        escapedCharacters.put('t', (char)0x09);
        escapedCharacters.put('b', (char)0x08);
        escapedCharacters.put('f', (char)0xFF);
        escapedCharacters.put('(', (char)0x28);
        escapedCharacters.put(')', (char)0x29);
        escapedCharacters.put('\\', (char)0x5C);
    }

    @SerId(1)
    private String value;

    @SerId(2)
    private String decryptedValue;

    public PdfString(AbstractPdfParsableAttribute parent, int startIndex) {
        super(parent, startIndex);
    }

    public String getValue() {
        return value;
    }

    public String getDecryptedValue() {
        return decryptedValue;
    }

    @Override
    public int parse(byte[] data, int cursor) {
        boolean isHexa = false;
        int intermediateParenthesis = 0;
        if(data[cursor] == '<') {
            isHexa = true;
        }
        cursor++; // skip first (
        int startLine = cursor;
        StringBuilder stb = new StringBuilder();
        while(cursor < data.length && !isEndToken(data, cursor, isHexa, intermediateParenthesis)) {
            if(isHexa) {
                // post process
            }
            else {
                if(data[cursor] == '\\') {
                    // escape
                    int endLine = PdfSpecialCharacters.testEndLine(data, cursor + 1);
                    Character c;
                    if(endLine == 0) {
                        // last character is \ do not add it to buffer
                    }
                    else if(endLine > 0) {
                        cursor += endLine; // skip current char + number of EOL chars
                    }
                    else if((c = escapedCharacters.get((char)data[cursor + 1])) != null) {
                        stb.append(c);
                        cursor++;
                    }
                    else if(Character.isDigit(data[cursor + 1])) {
                        int nbDigit = 1;
                        while(cursor + nbDigit < data.length && nbDigit < 3) {
                            // look for all digit
                            if(Character.isDigit(data[cursor + nbDigit + 1])) {
                                nbDigit++;
                            }
                            else {
                                break;
                            }
                        }
                        if(nbDigit == 3) {
                            stb.append((char)Integer.valueOf(new String(data, cursor + 1, nbDigit), 8).byteValue());
                            cursor += nbDigit; // skip interpreted digit
                        } // else ignore unnecessary/malformed escape
                    }
                    else {
                        // just ignore the unnecessary escape
                    }
                }
                else if(data[cursor] == '(') {
                    intermediateParenthesis++;
                }
                else if(data[cursor] == ')') {
                    intermediateParenthesis--;
                }
                else {
                    stb.append((char)data[cursor]);
                }
            }
            cursor++;
        }
        if(cursor >= data.length) {
            getPdfStatictics().addUnitNotification(this, SuspiciousType.Malformed, "Unclosed string");
        }
        setEndIndex(cursor);
        this.value = isHexa ? decodeHexa(data, startLine, cursor): stb.toString();

        return cursor;
    }

    private String decodeHexa(byte[] data, int fromIndex, int toIndex) {
        // first build pure string without any space char
        StringBuilder dataWitoutSpace = new StringBuilder();
        for(int i = fromIndex; i < toIndex; i++) {
            if(!PdfSpecialCharacters.isSeparator(data[i])) {
                dataWitoutSpace.append((char)data[i]);
            }
        }
        if(dataWitoutSpace.length() % 2 == 1) {
            // pad with 0
            dataWitoutSpace.append('0');
        }
        String hexaString = dataWitoutSpace.toString();

        // convert hexa to chars
        StringBuilder stb = new StringBuilder();
        for(int i = 0; i < hexaString.length(); i += 2) {
            try {
                String str = hexaString.substring(i, i + 2);
                stb.append((char)Integer.parseInt(str, 16));
            }
            catch(Exception e) {
                getPdfStatictics().addUnitNotification(this, SuspiciousType.Malformed, "Incorrect String format", e);
                return '<' + new String(data, fromIndex, toIndex - fromIndex) + '>';
            }
        }
        return stb.toString();
    }

    @Override
    public String toString() {
        return decryptedValue == null ? value: decryptedValue;
    }

    public static boolean isStartToken(byte[] data, int cursor) {
        return data[cursor] == '(' || (data[cursor] == '<' && data[cursor + 1] != '<');
    }

    public boolean isEndToken(byte[] data, int cursor, boolean isHexa, int intermediateParenthesis) {
        return (!isHexa && data[cursor] == ')' && intermediateParenthesis == 0) || (isHexa && data[cursor] == '>');
    }

    @Override
    public Type getType() {
        return Type.String;
    }

    public void decrypt(PDFDecrypter decrypter) {
        try {
            decryptedValue = decrypter.decryptString(getId().getObjectNumber(), getId().getGenerationNumber(), value);
        }
        catch(PDFParseException e) {
            e.printStackTrace();
            getMainParent().getPdfStatictics().addUnitNotification(this, SuspiciousType.StreamUnfiltered,
                    "Can not unencrypt String", e);
        }
    }

}
