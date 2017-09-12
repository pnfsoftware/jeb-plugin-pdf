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

import java.util.Arrays;
import java.util.List;

import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfName extends AbstractPdfSimpleValue {

    private static final ILogger logger = GlobalLog.getLogger(PdfName.class);

    public static final List<String> SUSPICIOUS_JS = Arrays.asList("/JavaScript", "/JS");

    public static final List<String> SUSPICIOUS_AUTOMATIC_ACTION = Arrays.asList("/OpenAction", "/AA");

    public static final List<String> SUSPICIOUS_FLASH = Arrays.asList("/RichMedia");

    public static final List<String> SUSPICIOUS_FORM = Arrays.asList("/AcroForm");

    public static final List<String> SUSPICIOUS_XFA = Arrays.asList("/XFA");

    public PdfName(String value, AbstractPdfParsableAttribute parent, int startIndex) {
        super(escapeValue(value), parent, startIndex);
        if(SUSPICIOUS_JS.contains(value)) {
            parent.getPdfStatictics().addUnitNotification(this, SuspiciousType.PotentialHarmfulToken, "Javascript",
                    null, value.equals("/JS"));
        }
        else if(SUSPICIOUS_AUTOMATIC_ACTION.contains(value)) {
            parent.getPdfStatictics().addUnitNotification(this, SuspiciousType.PotentialHarmfulToken,
                    "Automatic Action");
        }
        else if(SUSPICIOUS_FLASH.contains(value)) {
            parent.getPdfStatictics().addUnitNotification(this, SuspiciousType.PotentialHarmfulToken, "Flash");
        }
        else if(SUSPICIOUS_FORM.contains(value)) {
            parent.getPdfStatictics().addUnitNotification(this, SuspiciousType.PotentialHarmfulToken, "AcroForm");
        }
        else if(SUSPICIOUS_XFA.contains(value)) {
            parent.getPdfStatictics().addUnitNotification(this, SuspiciousType.PotentialHarmfulToken,
                    "XML Forms Architecture");
        }
    }

    private static String escapeValue(String value) {
        if(!value.contains("#")) {
            return value;
        }
        StringBuilder stb = new StringBuilder();
        for(int i = 0; i < value.length(); i++) {
            char valueChar = value.charAt(i);
            if(valueChar == '#') {
                String hexaRep = value.substring(i + 1, i + 3);
                Integer specialChar = null;
                try {
                    specialChar = Integer.valueOf(hexaRep, 16);
                    stb.append((char)specialChar.intValue());
                }
                catch(Exception e) {
                    logger.catching(e);
                }
                i = i + 2;
            }
            else {
                stb.append(valueChar);
            }
        }
        return stb.toString();
    }

    public static boolean isName(String value) {
        return value.charAt(0) == '/';
    }

    @Override
    public Type getType() {
        return Type.Name;
    }

}
