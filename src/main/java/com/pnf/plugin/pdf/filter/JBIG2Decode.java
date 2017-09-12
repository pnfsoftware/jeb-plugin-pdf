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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.filter.JBIG2Filter;

import com.pnf.plugin.pdf.obj.PdfDictionary;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class JBIG2Decode implements IFilter {
    private PdfDictionary decodeParms;

    public JBIG2Decode(PdfDictionary decodeParms) {
        this.decodeParms = decodeParms;
    }

    public byte[] decodeBytes(byte[] data, int fromByte, int length, PdfDictionary dictionary)
            throws FilterStreamException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(data, fromByte, length);
        try {
            new JBIG2Filter().decode(bais, baos, PDFObject.getInstance(dictionary), PDFObject.getInstance(decodeParms));
            return baos.toByteArray();
            // https://github.com/katjas/PDFrenderer fork of pdf-renderer use jpedal where license seems commercial
            // return new JBig2Decode().decode(PDFObject.getInstance(decodeParms),
            // ByteBufferUtils.getByteArray(data, fromByte, length).toByteArray());
        }
        catch(IOException e) {
            throw new FilterStreamException(e);
        }
    }

}
