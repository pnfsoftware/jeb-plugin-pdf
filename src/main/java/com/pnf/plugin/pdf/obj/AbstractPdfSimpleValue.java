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

import com.pnf.plugin.pdf.parser.InputOffset;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * Simple value (Name, Number...) that does not require specific parsing
 * 
 * @author PNF Software
 *
 */
@Ser
public abstract class AbstractPdfSimpleValue implements IPdfAttribute {

    @SerId(1)
    private String value;

    @SerId(2)
    private AbstractPdfParsableAttribute parent;

    @SerId(3)
    protected int startIndex;

    public AbstractPdfSimpleValue(String value, AbstractPdfParsableAttribute parent, int startIndex) {
        this.value = value;
        this.parent = parent;
        this.startIndex = startIndex;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public AbstractPdfParsableAttribute getParent() {
        return parent;
    }

    @Override
    public InputOffset toInputOffset() {
        return InputOffset.getInstance(this, startIndex);
    }

    @Override
    public PdfObjId getId() {
        return parent.getId();
    }

}
