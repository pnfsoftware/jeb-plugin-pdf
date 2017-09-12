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

import java.util.List;

import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfStream;

/**
 * 
 * 
 * @author PNF Software
 *
 * @param <T>
 */
public abstract class PdfHierarchyProcessor<T> {

    public void browseElement(IPdfAttribute attribute, T t) {
        switch(attribute.getType()) {
        case Array:
            browseArray(((PdfArray)attribute).getAttributes(), t);
            break;
        case Boolean:
        case Name:
        case Null:
        case Number:
        case String:
        case Unknown:
            processSimpleObject(attribute, t);
            break;
        case IndirectReference:
            processIndirectReference(attribute, t);
            break;
        case Stream:
            browseDictionaryChildren(((PdfStream)attribute).getDictionary().getAttributes(), t);
            break;
        case Dictionary:
            browseDictionaryChildren(((PdfDictionary)attribute).getAttributes(), t);
            break;
        default:
            break;
        }
    }

    private void browseArray(List<IPdfAttribute> attributes, T t) {
        int i = 0;
        for(IPdfAttribute attribute: attributes) {
            boolean recursive = processArrayElement(attribute, i, t);
            if(recursive) {
                browseElement(attribute, t);
            }
            i++;
        }
    }

    private void browseDictionaryChildren(List<PdfDictionaryAttribute> attributes, T t) {
        for(PdfDictionaryAttribute attribute: attributes) {
            boolean recursive = processDictionaryAttribute(attribute, t);
            if(recursive) {
                browseElement(attribute.getValue(), t);
            }
        }
    }

    public abstract void processSimpleObject(IPdfAttribute attribute, T t);

    public abstract void processIndirectReference(IPdfAttribute attribute, T t);

    public abstract boolean processArrayElement(IPdfAttribute attribute, int i, T t);

    public abstract boolean processDictionaryAttribute(PdfDictionaryAttribute attribute, T t);

}
