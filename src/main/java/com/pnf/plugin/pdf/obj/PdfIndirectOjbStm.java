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

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfIndirectOjbStm extends PdfIndirectObj {

    public PdfIndirectOjbStm(PdfFile file, PdfFileUnit unit, int startIndex) {
        super(file, unit, startIndex);
    }

    @Override
    public int parse(byte[] data, int cursor) {
        throw new UnsupportedOperationException();
    }

    public void parse(byte[] data, int cursor, String objId) {
        super.parse(data, cursor, new PdfObjId(Integer.valueOf(objId), 0, 0));
    }

    @Override
    public Type getType() {
        return Type.IndirectObjectStream;
    }

    @Override
    protected byte[] getStartToken() {
        return PdfSpecialCharacters.EMPTY_ARRAY;
    }

    @Override
    protected byte[] getEndToken() {
        return PdfSpecialCharacters.EMPTY_ARRAY;
    }

    @Override
    public boolean isEndToken(byte[] data, int cursor) {
        return false;
    }

}
