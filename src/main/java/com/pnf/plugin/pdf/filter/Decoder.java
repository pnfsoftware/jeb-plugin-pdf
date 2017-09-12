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

import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.parser.PdfFilterException;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class Decoder {
    private static final ILogger logger = GlobalLog.getLogger(Decoder.class);

    private PdfDictionary dictionary;

    private PdfFilterException decodingError;

    private int errorFilterIndex;

    public Decoder(PdfDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public byte[] parse(int filterIndex, IFilter filter, byte[] encodedData, byte[] data, int fromByte,
            Integer lengthFromDictionary) throws Exception {
        try {
            byte[] result = parse(filterIndex, filter, encodedData);
            if(decodingError == null) {
                return result;
            }

            if(lengthFromDictionary == null || lengthFromDictionary == encodedData.length
                    || fromByte + lengthFromDictionary > data.length) {
                // useless to process with another length
                return decodingError.getBaos().toByteArray();
            }
        }
        catch(Exception e) {
            if(lengthFromDictionary == null || lengthFromDictionary == encodedData.length
                    || fromByte + lengthFromDictionary > data.length) {
                throw e;
            }
        }

        // try to decode with argument length
        return parse(filterIndex, filter,
                ByteBufferUtils.getByteArray(data, fromByte, lengthFromDictionary).toByteArray());
    }

    public byte[] parse(int filterIndex, IFilter filter, byte[] data) throws Exception {
        try {
            return filter.decodeBytes(data, 0, data.length, dictionary);
        }
        catch(PdfFilterException e) {
            setDecodingError(e, filterIndex);
            return e.getBaos().toByteArray();
        }
        catch(Exception | Error e) {
            // unknown error probably related to third party should not break the project
            logger.error("An error occurred during stream parsing. Make sure you added all dependencies to project.");
            logger.catching(e);
            return new byte[0];
        }
    }

    public PdfFilterException getDecodingError() {
        return decodingError;
    }

    public int getFilterIndex() {
        return errorFilterIndex;
    }

    private void setDecodingError(PdfFilterException decodingError, int filterIndex) {
        if(this.decodingError == null) {
            this.decodingError = decodingError;
            this.errorFilterIndex = filterIndex;
        }
    }

}
