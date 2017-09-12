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

package com.pnf.plugin.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnf.plugin.pdf.obj.PdfStream;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class CrossReferences {
    private Map<Long, List<String>> crossReferences = new HashMap<Long, List<String>>();

    public CrossReferences(List<PdfIndirectObj> objList) {
        int i = 0;
        for(PdfIndirectObj obj: objList) {
            String prefix = String.valueOf(i);
            addCrossReference(obj.getId(), prefix);
            parseChild(obj.getAttribute(), prefix);
            i++;
        }
    }

    public void parseChildren(List<IPdfAttribute> attributes, String prefix) {
        int i = 0;
        for(IPdfAttribute attribute: attributes) {
            String prefixAttr = addAddress(prefix, i);
            parseChild(attribute, prefixAttr);
            i++;
        }
    }

    public void parseChild(IPdfAttribute attribute, String prefix) {
        switch(attribute.getType()) {
        case Array:
            List<IPdfAttribute> arrayAttributes = ((PdfArray)attribute).getAttributes();
            for(int j = 0; j < arrayAttributes.size(); j++) {
                parseChild(arrayAttributes.get(j), addAddress(prefix, j));
            }
            break;
        case Boolean:
        case Name:
        case Null:
        case Number:
        case String:
        case Unknown:
            // no cross reference
            break;
        case IndirectReference:
            PdfObjId idRef = ((PdfIndirectReference)attribute).getId();
            addCrossReference(idRef, prefix);
            break;
        case Stream:
            parseDictionaryChildren(((PdfStream)attribute).getDictionary().getAttributes(), prefix);
            break;
        case Dictionary:
            parseDictionaryChildren(((PdfDictionary)attribute).getAttributes(), prefix);
            break;
        default:
            break;
        }
    }

    public void parseDictionaryChildren(List<PdfDictionaryAttribute> attributes, String prefix) {
        int i = 0;
        for(PdfDictionaryAttribute attribute: attributes) {
            String prefixAttr = addAddress(prefix, i);
            switch(attribute.getValue().getType()) {
            case Array:
                parseChild(attribute.getValue(), prefixAttr);
                break;
            case Boolean:
            case Name:
            case Null:
            case Number:
            case String:
            case Unknown:
                // no cross reference
                break;
            case IndirectReference:
                PdfObjId idRef = ((PdfIndirectReference)attribute.getValue()).getId();
                addCrossReference(idRef, prefixAttr);
                break;
            case Stream:
            case Dictionary:
                parseChild(attribute.getValue(), prefixAttr);
                break;
            default:
                break;
            }
            i++;
        }
    }

    private String addAddress(String prefix, int line) {
        return prefix + "/" + line;
    }

    private void addCrossReference(PdfObjId id, String address) {
        List<String> addresses = crossReferences.get(Long.valueOf(id.getObjectNumber()));
        if(addresses == null) {
            addresses = new ArrayList<String>();
            crossReferences.put(Long.valueOf(id.getObjectNumber()), addresses);
        }
        addresses.add(address);
    }

    public List<String> getCrossReference(long itemId) {
        return crossReferences.get(itemId);
    }

}
