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

import com.pnf.plugin.pdf.PdfFileUnit;
import com.pnf.plugin.pdf.parser.PdfFile;
import com.pnf.plugin.pdf.parser.PdfSpecialCharacters;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfTrailer extends PdfIndirectObj {

    @Ser
    public static class PdfTrailerId extends PdfObjId {
        public static final int TRAILER_ID = -1;

        public PdfTrailerId(int generationNumber, int startAddress) {
            super(TRAILER_ID, generationNumber, startAddress);
        }

        @Override
        public String toString() {
            return String.format("xref");
        }

    }

    @SerId(1)
    private PdfTrailer prev;

    public PdfTrailer(PdfFile file, PdfFileUnit unit, int startIndex, int cursor, int trailerCount) {
        super(file, unit, startIndex);
        this.id = new PdfTrailerId(trailerCount, cursor);
    }

    public PdfTrailer(PdfFile file, PdfDictionary dictionary, PdfFileUnit unit, int trailerCount) {
        super(file, unit, dictionary.startIndex);
        this.attribute = dictionary;
        this.id = new PdfTrailerId(trailerCount, dictionary.startIndex);
    }

    @Override
    public boolean isEncrypted() {
        return getEncrypt() != null;
    }

    public IPdfAttribute getEncrypt() {
        return getDictionaryAttribute("/Encrypt");
    }

    public IPdfAttribute getDictionaryAttribute(String attributeName) {
        if(getAttribute() == null || getAttribute().getType() != Type.Dictionary) {
            return null;
        }
        PdfDictionary dictionary = (PdfDictionary)getAttribute();
        IPdfAttribute attributeValue = dictionary.getAttribute(attributeName);
        if(attributeValue == null) {
            if(prev == null) {
                IPdfAttribute prevObj = dictionary.getAttribute("/Prev");
                if(prevObj != null && prevObj.getType() == Type.Number) {
                    prev = unit.getTrailers().get(((PdfNumber)prevObj).intValue());
                }
            }
            if(prev != null) {
                return prev.getDictionaryAttribute(attributeName);
            }
        }
        return attributeValue;
    }

    public IPdfAttribute getID() {
        return getDictionaryAttribute("/ID");
    }

    @Override
    public int parse(byte[] data, int cursor) {
        return super.parse(data, cursor, this.id);
    }

    @Override
    public Type getType() {
        return Type.Trailer;
    }

    @Override
    protected byte[] getStartToken() {
        return PdfSpecialCharacters.TRAILER_START_SEPARATOR;
    }

    @Override
    protected byte[] getEndToken() {
        return PdfSpecialCharacters.XREF_START_SEPARATOR;
    }

    @Override
    public boolean isEndToken(byte[] data, int cursor) {
        return PdfSpecialCharacters.isStartXref(data, cursor);
    }

}
