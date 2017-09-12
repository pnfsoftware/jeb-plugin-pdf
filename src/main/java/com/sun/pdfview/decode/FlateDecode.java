/*
 * $Id: FlateDecode.java,v 1.4 2009-01-03 17:23:30 tomoke Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.sun.pdfview.decode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.pnf.plugin.pdf.filter.PDFObject;
import com.pnf.plugin.pdf.parser.PdfFilterException;

/**
 * decode a deFlated byte array
 * 
 * @author Mike Wessler
 */
public class FlateDecode {

    /**
     * decode a byte buffer in Flate format.
     * <p>
     * Flate is a built-in Java algorithm. It's part of the java.util.zip package.
     *
     * @param buf
     *            the deflated input buffer
     * @param params
     *            parameters to the decoder (unused)
     * @return the decoded (inflated) bytes
     */
    public static ByteBuffer decode(PDFObject dict, ByteBuffer buf, PDFObject params) throws PdfFilterException {
        Inflater inf = new Inflater(false);

        int bufSize = buf.remaining();

        // copy the data, since the array() method is not supported
        // on raf-based ByteBuffers
        byte[] data = new byte[bufSize];
        buf.get(data);

        // set the input to the inflater
        inf.setInput(data);

        // output to a byte-array output stream, since we don't
        // know how big the output will be
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] decomp = new byte[bufSize];
        int loc = 0;
        int read = 0;

        try {
            while (!inf.finished()) {
                read = inf.inflate(decomp);
                if (read <= 0) {
                    // System.out.println("Read = " + read + "! Params: " + params);
                    if (inf.needsDictionary()) {
                        throw new PdfFilterException("Don't know how to ask for a dictionary in FlateDecode", baos,
                                bufSize - inf.getRemaining());
                    } else {
                        // System.out.println("Inflate data length=" + buf.remaining());
                        return ByteBuffer.allocate(0);
                        // throw new PDFParseException("Inflater wants more data... but it's already here!");
                    }
                }
                baos.write(decomp, 0, read);
            }
        } catch (DataFormatException dfe) {
            throw new PdfFilterException(dfe, baos, bufSize - inf.getRemaining());
        }

        // return the output as a byte buffer
        ByteBuffer outBytes = ByteBuffer.wrap(baos.toByteArray());

        try {
            // undo a predictor algorithm, if any was used
            if (params != null && params.getDictionary().containsKey("Predictor")) {
                Predictor predictor = Predictor.getPredictor(params);
                if (predictor != null) {
                    outBytes = predictor.unpredict(outBytes);
                }
            }

        } catch (IOException dfe) {
            throw new PdfFilterException(dfe, baos, bufSize - inf.getRemaining());
        }
        return outBytes;
    }
}
