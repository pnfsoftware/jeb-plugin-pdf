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

import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnfsoftware.jeb.core.input.BytesInput;
import com.pnfsoftware.jeb.core.output.IUnitFormatter;
import com.pnfsoftware.jeb.core.units.AbstractBinaryUnit;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * Provide Utility method to build Dictionary & Encrypted/Encoded intermediate data stream.
 * 
 * @author PNF Software
 *
 */
@Ser
public abstract class AbstractStreamUnit extends AbstractBinaryUnit implements IPdfUnit {

    @SerId(1)
    protected String identifier;
    @SerId(2)
    protected PdfStream stream;
    @SerId(3)
    protected PdfStatistics statistics;

    public AbstractStreamUnit(IUnit parent, PdfStatistics statistics, PdfStream stream, String identifier) {
        this(parent, stream, identifier, statistics);
    }

    public AbstractStreamUnit(IUnit parent, PdfStream stream, String identifier, PdfStatistics statistics) {
        super(null, new BytesInput(stream.getDecodedData()), stream.getStreamType(), stream.getName(),
                parent.getUnitProcessor(), parent, parent.getPropertyDefinitionManager());
        this.stream = stream;
        this.identifier = identifier;
        this.statistics = statistics;
    }

    @Override
    public IUnitFormatter getFormatter() {
        return new StreamUnitProvider(null, stream).getFormatter();
    }

    @Override
    public PdfStatistics getStatistics() {
        return statistics;
    }

}
