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

package com.pnf.plugin.pdf.document;

import java.util.List;
import java.util.Map;

import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnfsoftware.jeb.core.output.IActionableItem;
import com.pnfsoftware.jeb.core.units.IUnitNotification;

/**
 * Interactive Node that allow jump from indirect references to Indirect Objects
 * 
 * @author PNF Software
 * 
 */
public class PdfObjectNode extends PdfNode implements IActionableItem {
    private long itemId;
    private int flags;

    public PdfObjectNode(IPdfAttribute attribute, Map<IPdfAttribute, List<IUnitNotification>> anomalies,
            boolean master) {
        super(attribute, anomalies);
        this.itemId = getItemId(attribute);
        this.flags = master ? ROLE_MASTER: 0;
    }

    public PdfObjectNode(IPdfAttribute attribute, PdfObjId id, Map<IPdfAttribute, List<IUnitNotification>> anomalies,
            boolean master) {
        this(attribute, anomalies, master);
        additionalLabels[0] = additionalLabels[0] != null ? (id.toString() + " " + additionalLabels[0]): id.toString();
    }

    public PdfObjectNode(PdfDictionaryAttribute dictionaryAttribute,
            Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        super(dictionaryAttribute, anomalies);
        this.itemId = getItemId(dictionaryAttribute.getValue());
        this.flags = 0;
    }

    private static long getItemId(IPdfAttribute attribute) {
        // bind only on object number since generation can show new version and shall be bound as well (the master flag
        // should then be set on the higher generation number)
        IPdfAttribute parent = attribute.getParent();
        if(parent.getType() == Type.IndirectObject || parent.getType() == Type.IndirectObjectStream
                || parent.getType() == Type.Trailer) {
            return ((PdfIndirectObj)parent).getId().getObjectNumber();
        }
        else if(attribute.getType() == Type.IndirectReference) {
            return ((PdfIndirectReference)attribute).getId().getObjectNumber();
        }
        return 0;
    }

    @Override
    public int getItemFlags() {
        return flags;
    }

    @Override
    public long getItemId() {
        return itemId;
    }

}
