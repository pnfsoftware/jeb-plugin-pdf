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
import java.util.Objects;

import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnf.plugin.pdf.parser.PdfComment;
import com.pnf.plugin.pdf.parser.PdfObjectParser;
import com.pnf.plugin.pdf.parser.PdfSpecialCharacters;
import com.pnf.plugin.pdf.parser.StartObjFoundException;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;
import com.pnfsoftware.jeb.util.serialization.annotations.SerTransient;

/**
 * Stores Key/value of a dictionary. The key is always a string, the value can be a simple value:
 * int, string, array of a dictionary set (<</foo (bar)>>).
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfDictionary extends AbstractPdfParsableAttribute {

    @SerId(1)
    private List<PdfDictionaryAttribute> attributes = new ArrayList<PdfDictionaryAttribute>();

    @SerId(2)
    private PdfStream stream = null;

    @SerTransient
    private String type;

    @SerTransient
    private String subtype;

    public PdfDictionary(AbstractPdfParsableAttribute parent, int startIndex) {
        super(parent, startIndex);
    }

    public static boolean isStartToken(byte[] data, int cursor) {
        return data[cursor] == '<' && data[cursor + 1] == '<';
    }

    public boolean isEndToken(byte[] data, int cursor) {
        return data[cursor] == '>' && data[cursor + 1] == '>';
    }

    @Override
    public int parse(byte[] data, int cursor) throws StartObjFoundException {
        cursor++; // ignore next '<'
        cursor++; // ignore next '<'
        PdfDictionaryAttribute currentAttribute = null;
        for(; cursor < data.length; cursor++) {
            if(PdfSpecialCharacters.isSeparator(data[cursor])) {
                // ignore spaces
            }
            else if(PdfSpecialCharacters.isComment(data[cursor])) {
                cursor = PdfComment.skipCommentsIfSome(data, cursor - 1);
            }
            else if(isEndToken(data, cursor)) {
                // is it a stream?
                cursor++; // ignore next '>'
                while(PdfSpecialCharacters.isSeparator(data, cursor + 1) && cursor + 1 < data.length) {
                    cursor++;
                }
                if(PdfSpecialCharacters.isStartStream(data, cursor + 1)) {
                    stream = new PdfStream(this, cursor);
                    cursor = stream.parse(data, cursor);
                    cursor += PdfSpecialCharacters.STREAM_END_SEPARATOR.length;
                }
                break;
            }
            else if(currentAttribute == null) {
                if(data[cursor] == '/') {
                    // key
                    int startLine = cursor;
                    while(!PdfSpecialCharacters.isDelimitorOrSeparator(data, cursor + 1)) {
                        cursor++;
                    }
                    PdfName key = new PdfName(new String(data, startLine, cursor - startLine + 1), this, startLine);
                    currentAttribute = new PdfDictionaryAttribute(this, key, startLine);
                }
                else {
                    // incorrect sequence/ corrupted file
                    getPdfStatictics().addUnitNotification(this, SuspiciousType.Malformed,
                            String.format("Incorrect Dictionary entries at address %X", cursor));
                    return cursor;
                }
            }
            else {
                PdfObjectParser parser = new PdfObjectParser(this);
                cursor = parser.parse(data, cursor);
                IPdfAttribute attribute = parser.getPdfAttribute();
                if(attribute != null) {
                    currentAttribute.setValue(attribute);
                    attributes.add(currentAttribute);
                    currentAttribute = null;
                }
            }

        }
        return cursor;
    }

    public List<PdfDictionaryAttribute> getAttributes() {
        return attributes;
    }

    public List<String> getAttributeKeys() {
        List<String> keys = new ArrayList<String>();
        if(attributes != null) {
            for(PdfDictionaryAttribute att: attributes) {
                keys.add(att.getKey().toString().substring(1));
            }
        }
        return keys;
    }

    private IPdfAttribute getDirectAttribute(String name) {
        for(PdfDictionaryAttribute dicAttr: getAttributes()) {
            if(dicAttr.getKey().toString().equals(name)) {
                return dicAttr.getValue();
            }
        }
        return null;
    }

    public boolean hasAttribute(String name) {
        IPdfAttribute attribute = getDirectAttribute(name);
        return attribute != null && attribute.getType() != Type.Null;
    }

    /** Search for an attribute value. It is an indirect reference, return the correct object */
    public IPdfAttribute getAttribute(String name) {
        return retrieveDirectObject(getDirectAttribute(name));
    }

    public static IPdfAttribute retrieveDirectObject(IPdfAttribute attribute) {
        if(attribute == null) {
            // not defined
        }
        else if(attribute.getType() == Type.IndirectReference) {
            return PdfIndirectReference.retrieveDirectObject(attribute);
        }
        return attribute;
    }

    public String getDictionaryType() {
        if(type == null) {
            type = Objects.toString(getAttribute("/Type"), null);
        }
        return type;
    }

    public String getDictionarySubtype() {
        if(subtype == null) {
            subtype = Objects.toString(getAttribute("/Subtype"), null);
        }
        return subtype;
    }

    public String getDictionaryFullType() {
        if(getDictionaryType() != null) {
            if(getDictionarySubtype() != null) {
                return getDictionaryType() + getDictionarySubtype();
            }
            return getDictionaryType();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder("<<");
        for(PdfDictionaryAttribute attribute: attributes) {
            stb.append("  ");
            stb.append(attribute.getKey()).append(" ");
            if(attribute.getValue().getType() == Type.String) {
                stb.append("(").append(attribute.getValue()).append(")");
            }
            else {
                stb.append(attribute.getValue());
            }
            stb.append("  ");
        }
        stb.append(">>");
        return stb.toString();
    }

    @Override
    public Type getType() {
        return Type.Dictionary;
    }

    @Override
    public IPdfAttribute getPdfAttribute() {
        if(stream != null) {
            return stream;
        }
        return this;
    }

}
