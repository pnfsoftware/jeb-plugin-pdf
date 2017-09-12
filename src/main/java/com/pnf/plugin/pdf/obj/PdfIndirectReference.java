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
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfIndirectReference implements IPdfAttribute {

    private static final ILogger logger = GlobalLog.getLogger(PdfDictionary.class);

    @SerId(1)
    private PdfObjId id = null;

    @SerId(2)
    private AbstractPdfParsableAttribute parent;

    @SerId(3)
    protected int startIndex;

    public PdfIndirectReference(PdfObjId id, AbstractPdfParsableAttribute parent, int startIndex) {
        this.id = id;
        this.parent = parent;
        this.startIndex = startIndex;
    }

    @Override
    public PdfObjId getId() {
        return id;
    }

    @Override
    public Type getType() {
        return Type.IndirectReference;
    }

    @Override
    public String toString() {
        return String.format("%s R", id.toString());
    }

    @Override
    public AbstractPdfParsableAttribute getParent() {
        return parent;
    }

    @Override
    public InputOffset toInputOffset() {
        return InputOffset.getInstance(this, startIndex);
    }

    public static IPdfAttribute retrieveDirectObject(IPdfAttribute attribute) {
        if(attribute == null) {
            // not defined
        }
        else if(attribute.getType() == Type.IndirectReference) {
            PdfIndirectReference ref = (PdfIndirectReference)attribute;
            PdfIndirectObj oref = ref.getParent().getMainParent().getDirectObject(ref.getId());
            if(oref == null) {
                logger.info("Reference %s not foud: return null object as stated in specs", attribute.getId());
                return null;
            }
            else {
                attribute = oref.getAttribute();
            }
        }
        return attribute;
    }

}
