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

import java.io.IOException;
import java.nio.ByteBuffer;

import com.pnf.plugin.pdf.obj.PdfDictionary;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class CCITTFaxDecode implements IFilter {
    private PdfDictionary decodeParms;

    public CCITTFaxDecode(PdfDictionary decodeParms) {
        this.decodeParms = decodeParms;
    }

    public byte[] decodeBytes(byte[] data, int fromByte, int length, PdfDictionary dictionary)
            throws FilterStreamException {
        ByteBuffer buf = ByteBufferUtils.getByteBuffer(data, fromByte, length);
        try {
            return com.sun.pdfview.decode.CCITTFaxDecode.decode(PDFObject.getInstance(dictionary),
                    PDFObject.getInstance(decodeParms), buf.array());
        }
        catch(IOException e) {
            throw new FilterStreamException(e);
        }
    }

}
