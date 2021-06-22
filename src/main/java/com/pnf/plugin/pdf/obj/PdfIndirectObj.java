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

import java.util.List;
import java.util.Map;

import com.pnf.plugin.pdf.PdfFileUnit;
import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnf.plugin.pdf.parser.InputOffset;
import com.pnf.plugin.pdf.parser.PdfAttributeValue;
import com.pnf.plugin.pdf.parser.PdfComment;
import com.pnf.plugin.pdf.parser.PdfFile;
import com.pnf.plugin.pdf.parser.PdfObjectParser;
import com.pnf.plugin.pdf.parser.PdfSpecialCharacters;
import com.pnf.plugin.pdf.parser.StartObjFoundException;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;
import com.sun.pdfview.decrypt.PDFDecrypter;

/**
 * Represent the main container of an Indirect object, identified by a unique {@link PdfObjId}. It
 * contains a unique child (sometimes can be null).
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfIndirectObj extends AbstractPdfParsableAttribute implements Comparable<PdfIndirectObj> {

    private static final ILogger logger = GlobalLog.getLogger(PdfIndirectObj.class);

    @SerId(1)
    /** Id of the indirect object */
    protected PdfObjId id;

    @SerId(2)
    /** unique child */
    protected IPdfAttribute attribute = null;

    @SerId(3)
    /** PdfUnit instance */
    protected PdfFileUnit unit;

    @SerId(4)
    /** Represent the version of this object (and associated encrypter/derypter) */
    private PdfFile file;

    public PdfIndirectObj(PdfFile file, PdfFileUnit unit, int startIndex) {
        super(null, startIndex);
        this.file = file;
        this.unit = unit;
    }

    @Override
    public int parse(byte[] data, int cursor) {
        return parse(data, cursor, PdfObjId.getObjId(data, cursor));
    }

    public int parse(byte[] data, int cursor, List<PdfAttributeValue> previousTokens) throws StartObjFoundException {
        PdfObjId objId = PdfObjId.getObjId(previousTokens);
        if(objId == null) {
            objId = PdfObjId.getObjId(data, cursor);
        }
        return parse(data, cursor, objId);
    }

    public int parse(byte[] data, int cursor, PdfObjId objId) {
        this.id = objId;
        if(this.id != null) {
            startIndex = this.id.getStartAddress();
        }
        logger.trace("Processing obj %s", this.id);
        // Retrieve the ID of the obj
        cursor += getStartToken().length;

        for(; cursor < data.length; cursor++) {
            if(PdfSpecialCharacters.isSeparator(data[cursor])) {
                // ignore spacings
            }
            else if(PdfSpecialCharacters.isComment(data[cursor])) {
                cursor = PdfComment.skipCommentsIfSome(data, cursor - 1);
            }
            else if(isEndToken(data, cursor)) {
                if(attribute == null) {
                    attribute = new PdfNull(this, cursor);
                }
                setEndIndex(cursor);
                cursor += getEndToken().length;
                return cursor;
            }
            else if(attribute == null) {
                PdfObjectParser parser = new PdfObjectParser(this);
                try {
                    cursor = parser.parse(data, cursor);
                    attribute = parser.getPdfAttribute();
                }
                catch(StartObjFoundException e) {
                    getPdfStatictics().addUnitNotification(this, SuspiciousType.Malformed,
                            String.format("Unexpected new obj definition in Indirect Object at address %x", cursor));
                    cursor -= 4;
                    break;
                }
            }
            else {
                if(getEndToken() == PdfSpecialCharacters.OBJ_END_SEPARATOR) {
                    logger.info("endobj is missing for Indirect Object %s", getId());
                }
                return cursor - 1;
            }
        }
        setEndIndex(cursor);
        return cursor;
    }

    protected byte[] getStartToken() {
        return PdfSpecialCharacters.OBJ_START_SEPARATOR;
    }

    protected byte[] getEndToken() {
        return PdfSpecialCharacters.OBJ_END_SEPARATOR;
    }

    public boolean isEndToken(byte[] data, int cursor) {
        return PdfSpecialCharacters.isEndObj(data, cursor);
    }

    @Override
    public Type getType() {
        return Type.IndirectObject;
    }

    @Override
    public PdfObjId getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        if(file.isEncrypted()) {
            stb.append("*");
        }
        stb.append(String.format("@(%d,%d) %s ", startIndex, endIndex, id.toString()));
        if(getType() == Type.IndirectObject) {
            stb.append("obj ");
        }
        if(attribute != null && attribute.getType() == Type.String) {
            stb.append("(").append(attribute).append(")");
        }
        else {
            stb.append(attribute);
        }
        return stb.toString();
    }

    @Override
    public int compareTo(PdfIndirectObj o) {
        return this.id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof PdfIndirectObj) {
            return this.id.equals(((PdfIndirectObj)obj).id);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public IPdfAttribute getAttribute() {
        return attribute;
    }

    public Map<PdfObjId, PdfIndirectObj> getObjects() {
        return file.getObjects();
    }

    @Override
    public InputOffset toInputOffset() {
        return InputOffset.getInstance(this, startIndex);
    }

    @Override
    public PdfStatistics getPdfStatictics() {
        return unit.getStatistics();
    }

    public PDFDecrypter getDecrypter() {
        return file.getDecrypter();
    }

    public boolean isEncrypted() {
        return file.isEncrypted();
    }

    public PdfIndirectObj getDirectObject(PdfObjId indirectId) {
        PdfFile refFile = unit.getObjects().get(indirectId);
        if(refFile == null) {
            return null;
        }
        return unit.getObjects().get(indirectId).getObject(indirectId);
    }

}
