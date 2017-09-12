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

import java.util.ArrayList;
import java.util.List;

import com.pnf.plugin.pdf.parser.PdfObjectParser;
import com.pnf.plugin.pdf.parser.PdfSpecialCharacters;
import com.pnf.plugin.pdf.parser.StartObjFoundException;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfArray extends AbstractPdfParsableAttribute {

    @SerId(1)
    private List<IPdfAttribute> attributes = new ArrayList<IPdfAttribute>();

    public PdfArray(AbstractPdfParsableAttribute parent, int startIndex) {
        super(parent, startIndex);
    }

    @Override
    public int parse(byte[] data, int cursor) throws StartObjFoundException {
        cursor++; // skip first [
        for(; cursor < data.length; cursor++) {
            if(PdfSpecialCharacters.isSeparator(data[cursor])) {
                // ignore spaces
            }
            else if(isEndToken(data, cursor)) {
                break;
            }
            else {
                PdfObjectParser parser = new PdfObjectParser(this);
                cursor = parser.parse(data, cursor);
                IPdfAttribute attribute = parser.getPdfAttribute();
                if(attribute != null) {
                    attributes.add(attribute);
                }
            }
        }
        return cursor;
    }

    public static boolean isStartToken(byte[] data, int cursor) {
        return data[cursor] == '[';
    }

    public boolean isEndToken(byte[] data, int cursor) {
        return data[cursor] == ']';
    }

    public List<IPdfAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder("[");
        for(IPdfAttribute attribute: attributes) {
            if(attribute.getType() == Type.String) {
                stb.append(" (").append(attribute).append(") ");
            }
            else {
                stb.append(" ").append(attribute).append(" ");
            }
        }
        stb.append("]");
        return stb.toString();
    }

    @Override
    public Type getType() {
        return Type.Array;
    }

}
