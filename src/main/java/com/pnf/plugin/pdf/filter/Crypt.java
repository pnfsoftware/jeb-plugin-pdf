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

import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.sun.pdfview.decrypt.PDFDecrypter;
import com.sun.pdfview.decrypt.PDFDecrypterFactory;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class Crypt implements IFilter {
    private PdfDictionary decodeParms;
    private PDFDecrypter decrypter;

    public Crypt(PdfDictionary decodeParms, PDFDecrypter decrypter) {
        this.decodeParms = decodeParms;
        this.decrypter = decrypter;
    }

    public byte[] decodeBytes(byte[] data, int fromByte, int length, PdfDictionary dictionary)
            throws FilterStreamException {

        String cfName = PDFDecrypterFactory.CF_IDENTITY;
        if(decodeParms != null) {
            final IPdfAttribute nameObj = decodeParms.getAttribute("/Name");
            if(nameObj != null && nameObj.getType() == Type.Name) {
                cfName = nameObj.toString().substring(1);
            }
        }

        ByteBuffer bais = ByteBufferUtils.getByteBuffer(data, fromByte, length);
        try {
            ByteBuffer bb = decrypter.decryptBuffer(cfName, null, bais);
            return bb.array();
        }
        catch(IOException e) {
            throw new FilterStreamException(e);
        }
    }

}
