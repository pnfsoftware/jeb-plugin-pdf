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

import com.pnf.plugin.pdf.AddressUtils;
import com.pnf.plugin.pdf.PdfFileUnit;
import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnfsoftware.jeb.core.input.BytesInput;
import com.pnfsoftware.jeb.core.output.AbstractTransientUnitRepresentation;
import com.pnfsoftware.jeb.core.output.IGenericDocument;
import com.pnfsoftware.jeb.core.output.IUnitFormatter;
import com.pnfsoftware.jeb.core.output.UnitFormatterUtil;
import com.pnfsoftware.jeb.core.output.text.impl.AsciiDocument;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * Simple Binary stream that displays dictionary and encoded Stream (not decoded)
 * 
 * @author PNF Software
 *
 */
@Ser
public class BinaryStreamUnit extends AbstractStreamUnit {
    @SerId(1)
    boolean decode;

    public BinaryStreamUnit(IUnit parent, PdfStatistics statistics, PdfStream stream, String identifier,
            boolean decode) {
        super(parent, statistics, stream, identifier);
        this.decode = decode;
    }

    public BinaryStreamUnit(PdfFileUnit parent, PdfStream stream, String identifier, boolean decode) {
        super(parent, parent.getStatistics(), stream, identifier);
        this.decode = decode;
    }

    @Override
    public boolean process() {
        setProcessed(true);
        return true;
    }

    @Override
    public IUnitFormatter getFormatter() {
        IUnitFormatter formatter = super.getFormatter();
        if(decode) {
            final String docLabel = "Decoded Stream";
            final String defaultText = stream.getAsText();
            if(getInput().getCurrentSize() == 0) {
                ;
            }
            else if(UnitFormatterUtil.getPresentationByName(formatter, docLabel) == null) {
                if(defaultText != null) {
                    formatter.insertPresentation(0, new AbstractTransientUnitRepresentation(docLabel, true) {
                        @Override
                        public IGenericDocument getDocument() {
                            return new AsciiDocument(new BytesInput(defaultText.getBytes()));
                        }
                    }, false);
                }
                else {
                    formatter.insertPresentation(0, new AbstractTransientUnitRepresentation(docLabel, true) {
                        @Override
                        public IGenericDocument getDocument() {
                            return new AsciiDocument(getInput());
                        }
                    }, false);
                }
            }
        }
        return formatter;
    }

    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder(super.getDescription());
        description.append("- Obj start Address: ").append(stream.getParent().getStartIndex());
        description.append("\n- Stream start Address: ").append(stream.getStartIndex());
        description.append("\n- Stream parsed length: ").append(stream.getParsedLength());
        Integer dictionaryLength = stream.getLengthFromDictionary();
        if(dictionaryLength == null) {
            description.append("\n- Stream has no length defined");
        }
        else {
            description.append("\n- Stream length: ").append(dictionaryLength);
        }
        return description.toString();
    }

    @Override
    public AddressUtils getAddressUtils() {
        return null;
    }
}
