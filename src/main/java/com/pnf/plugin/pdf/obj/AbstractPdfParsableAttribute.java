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

import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.parser.IPdfParsable;
import com.pnf.plugin.pdf.parser.InputOffset;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * Complex type
 * 
 * @author PNF Software
 *
 */
@Ser
public abstract class AbstractPdfParsableAttribute implements IPdfAttribute, IPdfParsable {

    @SerId(1)
    private AbstractPdfParsableAttribute parent;

    @SerId(2)
    protected int startIndex;

    @SerId(3)
    protected int endIndex;

    public AbstractPdfParsableAttribute(AbstractPdfParsableAttribute parent, int startIndex) {
        this.parent = parent;
        this.startIndex = startIndex;
    }

    @Override
    public IPdfAttribute getPdfAttribute() {
        return this;
    }

    public PdfIndirectObj getMainParent() {
        if(parent == null) {
            return ((PdfIndirectObj)this);
        }
        IPdfAttribute masterParent = parent;
        while(masterParent.getParent() != null) {
            masterParent = masterParent.getParent();
        }
        return ((PdfIndirectObj)masterParent);
    }

    @Override
    public AbstractPdfParsableAttribute getParent() {
        return parent;
    }

    protected PdfStatistics getPdfStatictics() {
        if(parent != null) {
            return parent.getPdfStatictics();
        }
        return null;
    }

    @Override
    public PdfObjId getId() {
        return getMainParent().getId();
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    @Override
    public InputOffset toInputOffset() {
        return InputOffset.getInstance(this, startIndex);
    }

    protected void setEndIndex(int cursor) {
        this.endIndex = cursor;
    }

}
