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
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfString;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfObjectParser {

    private IPdfParsable attribute = null;

    private AbstractPdfParsableAttribute parent;

    public PdfObjectParser(AbstractPdfParsableAttribute parent) {
        this.parent = parent;
    }

    public int parse(byte[] data, int cursor) throws StartObjFoundException {
        if(PdfDictionary.isStartToken(data, cursor)) {
            attribute = new PdfDictionary(parent, cursor);
        }
        else if(PdfString.isStartToken(data, cursor)) {
            attribute = new PdfString(parent, cursor);
        }
        else if(PdfArray.isStartToken(data, cursor)) {
            attribute = new PdfArray(parent, cursor);
        }
        else if(PdfSpecialCharacters.isComment(data[cursor])) {
            return PdfComment.skipCommentsIfSome(data, cursor - 1);
        }
        else {
            // boolean or number
            attribute = new PdfAttributeValue(parent, cursor);
        }
        return attribute.parse(data, cursor);
    }

    public IPdfAttribute getPdfAttribute() throws StartObjFoundException {
        return attribute != null ? attribute.getPdfAttribute(): null;
    }
}
