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

import java.util.ArrayList;
import java.util.List;

import com.pnf.plugin.pdf.document.TreePdfDocument;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnfsoftware.jeb.core.input.BytesInput;
import com.pnfsoftware.jeb.core.output.AbstractTransientUnitRepresentation;
import com.pnfsoftware.jeb.core.output.AbstractUnitRepresentation;
import com.pnfsoftware.jeb.core.output.IGenericDocument;
import com.pnfsoftware.jeb.core.output.IUnitDocumentPresentation;
import com.pnfsoftware.jeb.core.output.IUnitFormatter;
import com.pnfsoftware.jeb.core.output.UnitFormatterAdapter;
import com.pnfsoftware.jeb.core.output.UnitFormatterUtil;
import com.pnfsoftware.jeb.core.output.text.impl.HexDumpDocument;
import com.pnfsoftware.jeb.core.units.IBinaryUnit;
import com.pnfsoftware.jeb.core.units.IInteractiveUnit;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.core.units.impl.AbstractUnitProvider;
import com.pnfsoftware.jeb.core.units.impl.BinaryWrapperUnit;
import com.pnfsoftware.jeb.core.units.impl.InteractiveWrapperUnit;
import com.pnfsoftware.jeb.core.units.impl.WrapperUnit;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * Adds dictionary and encoded/encrypted stream representations
 * 
 * @author PNF Software
 *
 */
@Ser
public class StreamUnitProvider extends AbstractUnitProvider {
    @SerId(1)
    private PdfStream stream;

    public StreamUnitProvider(String formatType, PdfStream stream) {
        super(formatType, null, null);
        this.stream = stream;
    }

    @Override
    public IUnitFormatter getFormatter() {
        UnitFormatterAdapter formatter = new UnitFormatterAdapter();
        formatter.addPresentation(getDictionaryRepresentation(stream), false);
        UnitFormatterUtil.addAllPresentations(formatter, getEncodedRepresentations(stream), false);
        return formatter;
    }

    protected static AbstractUnitRepresentation getDictionaryRepresentation(final PdfStream stream) {
        return new AbstractTransientUnitRepresentation("Dictionary", false) {
            @Override
            public IGenericDocument getDocument() {
                return new TreePdfDocument(stream.getDictionary());
            }
        };
    }

    protected static List<IUnitDocumentPresentation> getEncodedRepresentations(final PdfStream stream) {
        List<IUnitDocumentPresentation> representations = new ArrayList<>();
        final List<byte[]> encodedDataList = stream.getEncodedDataList();
        if(!encodedDataList.isEmpty()) {

            if(stream.isEncrypted()) {
                representations.add(new AbstractTransientUnitRepresentation("Encrypted", false) {
                    @Override
                    public IGenericDocument getDocument() {
                        return new HexDumpDocument(new BytesInput(encodedDataList.get(0)));
                    }
                });
            }
            IPdfAttribute filters = stream.getAttribute("/Filter");
            for(int i = 0; i < encodedDataList.size() - (stream.isEncrypted() ? 1: 0); i++) {
                final byte[] encodedData = encodedDataList.get(i + (stream.isEncrypted() ? 1: 0));
                representations.add(new AbstractTransientUnitRepresentation(
                        String.format("Encoded[%d] with %s", i, getFilterName(filters, i)), false) {
                    @Override
                    public IGenericDocument getDocument() {
                        return new HexDumpDocument(new BytesInput(encodedData));
                    }
                });
            }
        }
        return representations;
    }

    private static String getFilterName(IPdfAttribute filters, int i) {
        IPdfAttribute filter = getFilter(filters, i);
        return filter.toString().substring(1);
    }

    private static IPdfAttribute getFilter(IPdfAttribute filters, int i) {
        if(filters.getType() == Type.Array) {
            IPdfAttribute filter = ((PdfArray)filters).getAttributes().get(i);
            if(filter.getType() == Type.IndirectReference) {
                return PdfIndirectReference.retrieveDirectObject(filter);
            }
            return filter;
        }
        return filters;
    }

    public static IUnit wrap(IUnit streamUnit, PdfStream stream, String formatType) {
        if(streamUnit == null) {
            return null;
        }
        if(streamUnit instanceof IInteractiveUnit) {
            return new InteractiveWrapperUnit((IInteractiveUnit)streamUnit, new StreamUnitProvider(formatType, stream));
        }
        if(streamUnit instanceof IBinaryUnit) {
            return new BinaryWrapperUnit((IBinaryUnit)streamUnit, new StreamUnitProvider(formatType, stream));
        }
        return new WrapperUnit<IUnit>(streamUnit, new StreamUnitProvider(formatType, stream));
    }
}
