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

import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfDictionaryAttribute extends AbstractPdfSimpleValue {

    @SerId(1)
    private PdfName key;

    @SerId(2)
    private IPdfAttribute value;

    public PdfDictionaryAttribute(PdfDictionary parent, PdfName key, int startIndex) {
        super(null, parent, startIndex);
        this.key = key;
    }

    public PdfDictionaryAttribute(PdfDictionary parent, PdfName key, IPdfAttribute value, int startIndex) {
        super(null, parent, startIndex);
        this.key = key;
        this.value = value;
    }

    public PdfName getKey() {
        return key;
    }

    public IPdfAttribute getValue() {
        return value;
    }

    protected void setValue(IPdfAttribute value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return Type.DictionaryAttribute;
    }

    @Override
    public String toString() {
        return String.format("{%s=%s}", key, value);
    }

}
