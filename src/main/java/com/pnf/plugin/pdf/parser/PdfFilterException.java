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

import java.io.ByteArrayOutputStream;

import com.pnf.plugin.pdf.filter.FilterStreamException;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfFilterException extends FilterStreamException {

    private static final long serialVersionUID = -3617446891328919322L;

    private ByteArrayOutputStream baos;
    private int processed;

    public PdfFilterException(String message, ByteArrayOutputStream baos, int processed) {
        super(message);
        this.baos = baos;
        this.processed = processed;
    }

    public PdfFilterException(Throwable cause, ByteArrayOutputStream baos, int processed) {
        super(cause);
        this.baos = baos;
        this.processed = processed;
    }

    public ByteArrayOutputStream getBaos() {
        return baos;
    }

    public int getProcessed() {
        return processed;
    }

}
