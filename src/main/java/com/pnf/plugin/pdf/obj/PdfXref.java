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

import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnfsoftware.jeb.util.format.Formatter;
import com.pnfsoftware.jeb.util.format.Strings;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfXref {

    private static final ILogger logger = GlobalLog.getLogger(PdfXref.class);

    private XrefSubsection[] indexes;

    public static class Xref {
        byte[] type;
        byte[] field2;
        byte[] field3;
    }

    public static class XrefSubsection {
        int[] index; // first object / size
        int[] w; // type / obj number or offset / gen number or index in ObjStm
        int objectSize = 0;
        List<Xref> xrefs = new ArrayList<Xref>();

        public int getObjectSize() {
            if(objectSize == 0) {
                objectSize = w[0] + w[1] + w[2];
            }
            return objectSize;
        }
    }

    public PdfXref(PdfStream stream) {
        indexes = populateReferences(stream);
    }

    public String getXRefText() {
        StringBuilder stb = new StringBuilder();
        for(XrefSubsection index: indexes) {
            for(Xref xref: index.xrefs) {
                stb.append(getStringLine(xref.type, xref.field2, xref.field3));
                stb.append('\n');
            }
            stb.append('\n');
        }
        return stb.toString();
    }

    private static String getStringLine(byte[]... array) {
        List<String> elements = new ArrayList<String>();
        for(byte[] r: array) {
            String element = toString(r);
            if(element != null) {
                elements.add(element);
            }

        }
        return Strings.joinList(elements);
    }

    private static String toString(byte[] r) {
        if(r == null || r.length == 0) {
            return "-";
        }

        return Formatter.byteArrayToHexString(r);
    }

    public static XrefSubsection[] populateReferences(PdfStream stream) {
        XrefSubsection[] indexes = getXRefSubsections(stream.getDictionary());
        byte[] decoded = stream.getDecodedData();
        int cursor = 0;
        for(XrefSubsection index: indexes) {
            for(int i = 0; i < index.index[1]; i++) {
                Xref xref = new Xref();
                int offset = 0;
                if(index.w[0] == 0) {
                    xref.type = new byte[]{(byte)1};// type 1
                }
                else {
                    xref.type = new byte[index.w[0]];
                    System.arraycopy(decoded, cursor, xref.type, 0, xref.type.length);
                }

                offset = index.w[0];
                if(index.w[1] == 0) {
                    // ???
                }
                else {
                    xref.field2 = new byte[index.w[1]];
                    System.arraycopy(decoded, cursor + offset, xref.field2, 0, xref.field2.length);
                }

                offset += index.w[1];
                if(index.w[2] == 0) {
                    // ???
                }
                else {
                    xref.field3 = new byte[index.w[2]];
                    System.arraycopy(decoded, cursor + offset, xref.field3, 0, xref.field3.length);
                }

                index.xrefs.add(xref);
                cursor += index.getObjectSize();
            }
        }
        return indexes;
    }

    private static XrefSubsection[] getXRefSubsections(PdfDictionary dictionary) {
        XrefSubsection[] indexes = null;
        IPdfAttribute index = dictionary.getAttribute("/Index");
        if(index != null && index.getType() == Type.Array) {
            List<IPdfAttribute> indexesObj = ((PdfArray)index).getAttributes();
            if(indexesObj.size() % 2 != 0) {
                logger.error("Index should be pair in stream %s", dictionary.getId());
                return null;
            }
            indexes = new XrefSubsection[indexesObj.size() / 2];
            for(int i = 0; i < indexes.length; i++) {
                indexes[i] = new XrefSubsection();
                indexes[i].index = new int[2];
                if(indexesObj.get(i * 2).getType() == Type.Number
                        && indexesObj.get(i * 2 + 1).getType() == Type.Number) {
                    indexes[i].index[0] = ((PdfNumber)indexesObj.get(i * 2)).intValue();
                    indexes[i].index[1] = ((PdfNumber)indexesObj.get(i * 2 + 1)).intValue();
                }
                else {
                    logger.error("Index should be pair of integer in stream %s", dictionary.getId());
                    return null;
                }
            }
        }
        else {
            IPdfAttribute size = dictionary.getAttribute("/Size");
            if(size != null && size.getType() == Type.Number) {
                indexes = new XrefSubsection[1];
                indexes[0] = new XrefSubsection();
                indexes[0].index = new int[2];
                indexes[0].index[0] = 0;
                indexes[0].index[1] = ((PdfNumber)size).intValue();
            }
            else {
                logger.error("Can not retrieve Index from Size in stream %s", dictionary.getId());
                return null;
            }
        }

        IPdfAttribute w = dictionary.getAttribute("/W");
        if(w != null && w.getType() == Type.Array) {
            List<IPdfAttribute> wsObj = ((PdfArray)w).getAttributes();
            if(wsObj.size() % 3 != 0 || !(wsObj.size() / 3 == indexes.length || wsObj.size() == 3)) {
                logger.error("W is not compatible with Index in stream %s", dictionary.getId());
                return null;
            }

            for(int i = 0; i < indexes.length; i++) {
                indexes[i].w = new int[3];
                int offset = wsObj.size() == 3 ? 0: (i * 3);
                if(wsObj.get(offset).getType() == Type.Number && wsObj.get(offset + 1).getType() == Type.Number
                        && wsObj.get(offset + 2).getType() == Type.Number) {
                    indexes[i].w[0] = ((PdfNumber)wsObj.get(offset)).intValue();
                    indexes[i].w[1] = ((PdfNumber)wsObj.get(offset + 1)).intValue();
                    indexes[i].w[2] = ((PdfNumber)wsObj.get(offset + 2)).intValue();
                }
                else {
                    logger.error("Index should be pair of integer in stream %s", dictionary.getId());
                    return null;
                }
            }
            return indexes;
        }
        else {
            logger.error("Can not retrieve W in stream %s", dictionary.getId());
            return null;
        }
    }

}
