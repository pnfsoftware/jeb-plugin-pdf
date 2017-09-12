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

import java.nio.ByteBuffer;

import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfComment {

    private static final ILogger logger = GlobalLog.getLogger(PdfComment.class);

    public static int skipCommentsIfSome(byte[] data, int cursor) {
        int startLine = cursor;
        if(PdfSpecialCharacters.isComment(data[cursor + 1])) {
            for(; cursor < data.length; cursor++) {
                if(PdfSpecialCharacters.isEndComment(data, cursor + 1)) {
                    return cursor;
                }
            }
        }
        logger.info("Found comment: %s", new String(data, startLine, cursor - startLine));
        return cursor;
    }

    public static boolean skipComments(ByteBuffer data) {
        while(data.hasRemaining()) {
            if(PdfSpecialCharacters.isEndComment(data.get())) {
                return false;
            }
        }
        return true;
    }
}
