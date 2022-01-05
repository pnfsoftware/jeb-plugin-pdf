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

package com.pnf.plugin.pdf.parser;

import java.io.IOException;
import java.io.InputStream;

import com.pnfsoftware.jeb.core.input.IInput;
import com.pnfsoftware.jeb.util.io.IO;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfHeaderParser {
    private static final ILogger logger = GlobalLog.getLogger(PdfHeaderParser.class);

    private static final byte[] PDF_HEADER = "%PDF-1".getBytes();
    private IInput input;

    public PdfHeaderParser(IInput input) {
        this.input = input;
    }

    public boolean isPdf() {
        if(input == null) {
            return false;
        }

        try(InputStream in = input.getStream()) {
            // PDF can start with a comment or any white space character
            byte[] data = IO.readInputStream(in);

            for(int cursor = 0; cursor < data.length; cursor++) {
                if(PdfSpecialCharacters.isSeparator(data[cursor])) {
                    // ignore spacings
                }
                else if(PdfSpecialCharacters.isComment(data[cursor])) {
                    if(isPdfHeader(data, cursor)) {
                        return true;
                    }
                    cursor = PdfComment.skipCommentsIfSome(data, cursor - 1);
                }
                else {
                    // return false;
                }
            }
        }
        catch(IOException e) {
            logger.catching(e);
        }
        return false;
    }

    private boolean isPdfHeader(byte[] data, int cursor) {
        if(cursor + 5 >= data.length) {
            return false;
        }
        if(data[cursor + 1] == PDF_HEADER[1] && data[cursor + 2] == PDF_HEADER[2] && data[cursor + 3] == PDF_HEADER[3]
                && data[cursor + 4] == PDF_HEADER[4] && data[cursor + 5] == PDF_HEADER[5]) {
            return true;
        }
        return false;
    }
}
