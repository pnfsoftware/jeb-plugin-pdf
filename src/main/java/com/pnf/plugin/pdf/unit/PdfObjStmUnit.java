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

package com.pnf.plugin.pdf.unit;

import java.util.List;

import com.pnf.plugin.pdf.AddressUtils;
import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.document.TreePdfDocument;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnfsoftware.jeb.core.output.AbstractTransientUnitRepresentation;
import com.pnfsoftware.jeb.core.output.IGenericDocument;
import com.pnfsoftware.jeb.core.output.IUnitFormatter;
import com.pnfsoftware.jeb.core.output.UnitFormatterUtil;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerTransient;

/**
 * Displays an ObjStm Stream: this kind of stream contains a list of Indirect Objects that are
 * displayed the same way it is for main view.
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfObjStmUnit extends AbstractStreamUnit implements IPdfUnit {

    @SerTransient
    private List<PdfIndirectObj> objects;
    @SerTransient
    private AddressUtils addressManager;

    public PdfObjStmUnit(IUnit parent, PdfStream stream, String identifier, PdfStatistics statistics) {
        super(parent, stream, identifier, statistics);

        // delegate streams extends
        if(stream.hasExtendedStream()) {
            for(PdfIndirectObj streamO: stream.getExtendedByList()) {
                PdfStream substream = (PdfStream)streamO.getAttribute();
                IUnit streamUnit = new PdfObjStmUnit(this, substream, identifier, statistics);
                addChild(streamUnit);
            }
        }
    }

    @Override
    public boolean process() {
        // stream was already processed for generic view
        setProcessed(true);
        return true;
    }

    @Override
    public IUnitFormatter getFormatter() {
        IUnitFormatter formatter = super.getFormatter();
        if(UnitFormatterUtil.getPresentationByName(formatter, "ObjStm Tree") == null) {
            formatter.insertPresentation(0, new AbstractTransientUnitRepresentation("ObjStm Tree", true) {
                @Override
                public IGenericDocument createDocument() {
                    TreePdfDocument document = new TreePdfDocument(getObjectList(), PdfObjStmUnit.this);
                    return document;
                }
            }, false);
        }
        return formatter;
    }

    public List<PdfIndirectObj> getObjectList() {
        if(objects == null) {
            objects = stream.getObjStmList();
        }
        return objects;
    }

    @Override
    public AddressUtils getAddressUtils() {
        if(addressManager == null) {
            addressManager = new AddressUtils(objects);
        }
        return addressManager;
    }
}
