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
import com.pnf.plugin.pdf.parser.PdfAttributeValue;
import com.pnf.plugin.pdf.parser.PdfSpecialCharacters;
import com.pnf.plugin.pdf.parser.StartObjFoundException;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * Contains the tuple (object number/generation number)
 * 
 * @author PNF Software
 * 
 */
@Ser
public class PdfObjId implements Comparable<PdfObjId> {

    private static final ILogger logger = GlobalLog.getLogger(PdfDictionary.class);

    @SerId(1)
    private int objectNumber;

    @SerId(2)
    private int generationNumber;

    @SerId(3)
    private int startAddress;

    public PdfObjId(int objectNumber, int generationNumber, int startAddress) {
        this.objectNumber = objectNumber;
        this.generationNumber = generationNumber;
        this.startAddress = startAddress;
    }

    public int getObjectNumber() {
        return objectNumber;
    }

    public int getGenerationNumber() {
        return generationNumber;
    }

    public int getStartAddress() {
        return startAddress;
    }

    @Override
    public int compareTo(PdfObjId o) {
        int compare = Integer.compare(this.objectNumber, o.objectNumber);
        if(compare != 0) {
            return compare;
        }
        return Integer.compare(this.generationNumber, o.generationNumber);
    }

    public static PdfObjId getObjId(List<PdfAttributeValue> previousTokens) throws StartObjFoundException {
        if(previousTokens != null && previousTokens.size() >= 2) {
            IPdfAttribute objN = previousTokens.get(previousTokens.size() - 2).getPdfAttribute();
            IPdfAttribute genN = previousTokens.get(previousTokens.size() - 1).getPdfAttribute();
            if(objN.getType() == Type.Number && genN.getType() == Type.Number) {
                return new PdfObjId(Integer.valueOf(objN.toString()), Integer.valueOf(genN.toString()),
                        ((PdfNumber)objN).startIndex);
            }
        }
        return null;
    }

    public static PdfObjId getObjId(byte[] data, int cursor) {
        int startAddress = cursor - 1;
        int previousSeparator = 0;
        List<Integer> objIdentifier = new ArrayList<Integer>();
        StringBuilder stb = new StringBuilder();
        while(startAddress >= 0) {
            if(PdfSpecialCharacters.isSeparator(data[startAddress])) {
                if(!PdfSpecialCharacters.isSeparator(data[startAddress + 1])) {
                    previousSeparator++;
                    if(stb.length() > 0) {
                        objIdentifier.add(Integer.valueOf(stb.toString()));
                    }
                    stb = new StringBuilder();
                    if(previousSeparator == 3) {
                        // third non consecutive space
                        break;
                    }
                } // else consecutive spaces to be ignored
            }
            else if(Character.isDigit(data[startAddress])) {
                stb.insert(0, (char)data[startAddress]);
            }
            else if(previousSeparator == 2 && PdfSpecialCharacters.isDelimitor(data[startAddress])) {
                // character before id can be any delimiter since endobj may be optional
                if(stb.length() > 0) {
                    objIdentifier.add(Integer.valueOf(stb.toString()));
                }
                break;
            }
            else {
                logger.error("Unsupported sequence in obj id '%s' at address: %x", (char)data[startAddress], cursor);
                break;
            }
            startAddress--;
        }

        if(startAddress != 0) {
            // cursor is on the third non consecutive space > increment
            // else cursor is at the beginning of file
            startAddress++;
        }
        switch(objIdentifier.size()) {
        case 0:
            logger.error("No Id found at address %x", cursor);
            return new PdfObjId(0, 0, cursor);
        case 1:
            logger.error("Id found only 1 int, assuming it is object number %d", objIdentifier.get(0));
            return new PdfObjId(objIdentifier.get(0), 0, cursor);
        case 2:
            return new PdfObjId(objIdentifier.get(1), objIdentifier.get(0), startAddress);
        default:
            logger.error("Incorrect Id found at address %x", cursor);
            break;
        }
        return new PdfObjId(0, 0, cursor);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof PdfObjId) {
            PdfObjId o = (PdfObjId)obj;
            return this.objectNumber == o.objectNumber && this.generationNumber == o.generationNumber;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.objectNumber;
    }

    @Override
    public String toString() {
        if(objectNumber == 0 && generationNumber == 0) {
            return "undefined";
        }
        return String.format("%d %d", objectNumber, generationNumber);
    }

}
