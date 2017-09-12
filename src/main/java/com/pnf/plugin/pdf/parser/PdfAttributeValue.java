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

import com.pnf.plugin.pdf.obj.AbstractPdfParsableAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.PdfBoolean;
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfName;
import com.pnf.plugin.pdf.obj.PdfNull;
import com.pnf.plugin.pdf.obj.PdfNumber;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnf.plugin.pdf.obj.PdfUnknown;

/**
 * Represent a simple Pdf Value: can be number, boolean, name or indirect reference
 * 
 * @author PNF Software
 * 
 */
public class PdfAttributeValue implements IPdfParsable {

    private String value;

    private PdfIndirectReference id = null;

    private AbstractPdfParsableAttribute parent;

    protected int startIndex;

    public PdfAttributeValue(AbstractPdfParsableAttribute parent, int startIndex) {
        this.parent = parent;
        this.startIndex = startIndex;
    }

    @Override
    public int parse(byte[] data, int cursor) {
        startIndex = cursor;
        while(!isEndToken(data, cursor + 1) && cursor < data.length) {
            // do not process next char since it can be a meaningful delimiter (for example an array: [16])
            cursor++;
        }
        this.value = new String(data, startIndex, cursor - startIndex + 1);
        if(value.matches("\\d+")) {
            int referenceCursor = cursor + 1;
            // test for an indirect reference?
            if(PdfSpecialCharacters.isSeparator(data, referenceCursor)) {
                return testIndirectReference(data, referenceCursor, cursor);
            }
        }
        return cursor;
    }

    private int testIndirectReference(byte[] data, int referenceCursor, int cursor) {
        referenceCursor = PdfSpecialCharacters.jumpSeparators(data, referenceCursor);
        if(referenceCursor >= data.length) {
            return cursor;
        }

        StringBuilder generationNumber = new StringBuilder();
        while(Character.isDigit((char)data[referenceCursor]) && referenceCursor < data.length) {
            generationNumber.append((char)data[referenceCursor]);
            referenceCursor++;
        }
        if(!PdfSpecialCharacters.isSeparator(data, referenceCursor) || generationNumber.length() == 0) {
            // not followed by a digit or followed by (digit + NotASpace)
            return cursor;
        }

        referenceCursor = PdfSpecialCharacters.jumpSeparators(data, referenceCursor);
        if(referenceCursor >= data.length) {
            return cursor;
        }

        if(PdfSpecialCharacters.isChar(data, referenceCursor, 'R')) {
            // this is indirect reference object
            id = new PdfIndirectReference(
                    new PdfObjId(Integer.valueOf(value), Integer.valueOf(generationNumber.toString()), startIndex),
                    parent, startIndex);
            return referenceCursor;
        }
        return cursor;
    }

    public int parseNexToken(byte[] data, int cursor) {
        while(PdfSpecialCharacters.isSeparator(data, cursor) && cursor < data.length) {
            cursor++;
        }
        if(cursor >= data.length) {
            return cursor;
        }
        return parse(data, cursor);
    }

    @Override
    public String toString() {
        try {
            return getPdfAttribute().toString();
        }
        catch(StartObjFoundException e) {
            return e.getMessage();
        }
    }

    public static boolean isStartToken(byte[] data, int cursor) {
        return !PdfSpecialCharacters.isDelimitor(data[cursor]);
    }

    public boolean isEndToken(byte[] data, int cursor) {
        return PdfSpecialCharacters.isDelimitorOrSeparator(data, cursor);
    }

    @Override
    public IPdfAttribute getPdfAttribute() throws StartObjFoundException {
        if(id != null) {
            return id;
        }
        else if(value == null || value.length() == 0) {
            return new PdfNull(parent, startIndex);
        }
        else if(PdfName.isName(value)) {
            return new PdfName(value, parent, startIndex);
        }
        else if(PdfNumber.isNumeric(value)) {
            return new PdfNumber(value, parent, startIndex);
        }
        else if(PdfBoolean.isBoolean(value)) {
            return new PdfBoolean(value, parent, startIndex);
        }
        else if(PdfNull.isNull(value)) {
            return new PdfNull(parent, startIndex);
        }
        // unknown object
        // if it is an obj, it means that endobj was missing
        if(value.equals("obj")) {
            throw new StartObjFoundException();
        }
        return new PdfUnknown(value, parent, startIndex);
    }
}
