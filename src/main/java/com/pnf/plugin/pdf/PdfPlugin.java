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

package com.pnf.plugin.pdf;

import com.pnf.plugin.pdf.parser.PdfHeaderParser;
import com.pnfsoftware.jeb.core.IUnitCreator;
import com.pnfsoftware.jeb.core.PluginInformation;
import com.pnfsoftware.jeb.core.Version;
import com.pnfsoftware.jeb.core.input.IInput;
import com.pnfsoftware.jeb.core.properties.IPropertyDefinitionManager;
import com.pnfsoftware.jeb.core.units.AbstractUnitIdentifier;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.core.units.IUnitProcessor;
import com.pnfsoftware.jeb.core.units.WellKnownUnitTypes;

/**
 * PDF JEB2 unit plugin. Detects PDF files and extract their content
 * 
 * @author PNF Software
 * 
 */
public class PdfPlugin extends AbstractUnitIdentifier {
    public static final String TYPE = WellKnownUnitTypes.typePdf;

    public PdfPlugin() {
        super(TYPE, 0);
    }

    @Override
    public PluginInformation getPluginInformation() {
        return new PluginInformation("PDF parser", "Adobe PDF file parsing", "PNF Software", Version.create(1, 0, 7));
    }

    @Override
    public void initialize(IPropertyDefinitionManager parentPdm) {
        super.initialize(parentPdm);
    }

    @Override
    public boolean canIdentify(IInput input, IUnitCreator parent) {
        if(checkBytes(input, 0, (byte)0x25, (byte)0x50, (byte)0x44, (byte)0x46)) {
            return true;
        }
        // determine PDF file
        return new PdfHeaderParser(input).isPdf();
    }

    @Override
    public IUnit prepare(String name, IInput data, IUnitProcessor unitProcessor, IUnitCreator parent) {
        return new PdfFileUnit(name, data, unitProcessor, parent, pdm);
    }
}
