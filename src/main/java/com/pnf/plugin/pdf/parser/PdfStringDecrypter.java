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

import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfString;
import com.sun.pdfview.decrypt.PDFDecrypter;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfStringDecrypter extends PdfHierarchyProcessor<Object> {

    private PDFDecrypter decrypter;

    public PdfStringDecrypter(PDFDecrypter decrypter) {
        this.decrypter = decrypter;
    }

    private void decryptString(PdfString attribute) {
        attribute.decrypt(decrypter);
    }

    @Override
    public void processSimpleObject(IPdfAttribute attribute, Object t) {
        if(attribute.getType() == Type.String) {
            decryptString((PdfString)attribute);
        }
    }

    @Override
    public void processIndirectReference(IPdfAttribute attribute, Object t) {
        // nothing to do
    }

    @Override
    public boolean processArrayElement(IPdfAttribute attribute, int i, Object t) {
        if(attribute.getType() == Type.String) {
            decryptString((PdfString)attribute);
            return false;
        }
        return true;
    }

    @Override
    public boolean processDictionaryAttribute(PdfDictionaryAttribute attribute, Object t) {
        if(attribute.getValue().getType() == Type.String) {
            decryptString((PdfString)attribute.getValue());
            return false;
        }
        return true;
    }

}
