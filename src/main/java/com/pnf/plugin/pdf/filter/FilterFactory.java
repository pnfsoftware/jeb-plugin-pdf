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

package com.pnf.plugin.pdf.filter;

import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfName;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.sun.pdfview.decrypt.PDFDecrypter;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class FilterFactory {

    public static IFilter getFilterInstance(PdfStream stream, PdfName attribute, IPdfAttribute decodeParms,
            PdfStatistics statistics, PDFDecrypter decrypter) {
        if(decodeParms != null && decodeParms.getType() == Type.Null) {
            decodeParms = null;
        }
        else if(decodeParms != null && decodeParms.getType() != Type.Dictionary) {
            statistics.addUnitNotification(stream, SuspiciousType.Malformed, String
                    .format("Unable to parse [Stream %s]/DecodeParms: expected dictionary or null", stream.getId()));
            decodeParms = null;
        }
        PdfDictionary parms = (PdfDictionary)decodeParms;

        String filterName = attribute.toString();
        statistics.addFiltersUsed(filterName);
        if(filterName.equalsIgnoreCase("/FlateDecode") || filterName.equalsIgnoreCase("/Fl")) {
            return new FlateDecode(parms);
        }
        else if(filterName.equalsIgnoreCase("/CCITTFaxDecode") || filterName.equalsIgnoreCase("/CCF")) {
            return new CCITTFaxDecode(parms);
        }
        else if(filterName.equalsIgnoreCase("/ASCIIHexDecode") || filterName.equalsIgnoreCase("/AHx")) {
            return new ASCIIHexDecode();
        }
        else if(filterName.equalsIgnoreCase("/ASCII85Decode") || filterName.equalsIgnoreCase("/A85")) {
            return new ASCII85Decode();
        }
        else if(filterName.equalsIgnoreCase("/DCTDecode") || filterName.equalsIgnoreCase("/DCT")) {
            return new DCTDecode(parms);
        }
        else if(filterName.equalsIgnoreCase("/LZWDecode") || filterName.equalsIgnoreCase("/LZW")) {
            return new LZWDecode(parms);
        }
        else if(filterName.equalsIgnoreCase("/JBIG2Decode")) {
            return new JBIG2Decode(parms);
        }
        else if(filterName.equalsIgnoreCase("/RunLengthDecode") || filterName.equalsIgnoreCase("/RL")) {
            return new RunLengthDecode();
        }
        else if(filterName.equalsIgnoreCase("/JPXDecode")) {
            return new JPXDecode(parms);
        }
        else if(filterName.equalsIgnoreCase("/Crypt")) {
            return new Crypt(parms, decrypter);

        }
        else {
            statistics.addUnitNotification(stream, SuspiciousType.StreamUnfiltered,
                    String.format("Unable to parse [Stream %s]%s: not supported filter", stream.getId(), attribute));
            return new NullFilter();
        }
    }

}
