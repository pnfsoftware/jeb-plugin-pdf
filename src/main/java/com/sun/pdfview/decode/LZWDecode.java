/*
 * $Id: LZWDecode.java,v 1.4 2009-02-22 00:45:32 tomoke Exp $
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

import com.pnf.plugin.pdf.filter.PDFObject;
import com.pnf.plugin.pdf.parser.PdfFilterException;
import com.sun.pdfview.PDFParseException;

/**
 * decode an LZW-encoded array of bytes.  LZW is a patented algorithm.
 *
 * <p>Feb 21, 2009 Legal statement on Intellectual Property from Unisys</p><pre>
 * <b><u>LZW Patent Information</u></b> (http://www.unisys.com/about__unisys/lzw)
 * <u>License Information on GIF and Other LZW-based Technologies
 * </u><p>
 * <b><i>Unisys U.S. LZW Patent No. 4,558,302 expired on June 20, 2003,
 * the counterpart patents in the United Kingdom, France, Germany and
 * Italy expired on June 18, 2004, the Japanese counterpart patents
 * expired on June 20, 2004 and the counterpart Canadian patent
 * expired on July 7, 2004.
 * </i></b><p>
 * Unisys Corporation holds and has patents pending on a number of
 * improvements on the inventions claimed in the above-expired patents.
 * Information on these improvement patents and terms under which they
 * may be licensed can be obtained by contacting the following:
 *<p>
 * Unisys Corporation
 * Welch Patent Licensing Department
 * Mail Stop E8-114
 * Unisys Way
 * Blue Bell, PA  19424
 *<p>
 * Via the Internet, send email to Robert.Marley@unisys.com.
 *<p>
 * Via facsimile, send inquiries to Welch Patent Licensing Department at
 * 215-986-3090.
 *<p>
 * The above is presented for information purposes only, and is subject
 * to change by Unisys.  Additionally, this information should not be
 * considered as legally obligating Unisys in any way with regard to license
 * availability, or as to the terms and conditions offered for a license,
 * or with regard to the interpretation of any license agreements.
 * You should consult with your own legal counsel regarding your
 * particular situation.
 * </pre></p>
 * 
 * @author Mike Wessler
 */
public class LZWDecode {

    ByteBuffer buf;
    int bytepos;
    int bitpos;
    byte[] dict[] = new byte[4096][];
    int dictlen = 0;
    int bitspercode = 9;
    static int STOP = 257;
    static int CLEARDICT = 256;

    /**
     * initialize this decoder with an array of encoded bytes
     * @param buf the buffer of bytes
     */
    private LZWDecode(ByteBuffer buf) throws PDFParseException {
        for (int i = 0; i < 256; i++) {
            dict[i] = new byte[1];
            dict[i][0] = (byte) i;
        }
        dictlen = 258;
        bitspercode = 9;
        this.buf = buf;
        bytepos = 0;
        bitpos = 0;
    }

    /**
     * reset the dictionary to the initial 258 entries
     */
    private void resetDict() {
        dictlen = 258;
        bitspercode = 9;
    }

    /**
     * get the next code from the input stream
     */
    private int nextCode() {
        int fillbits = bitspercode;
        int value = 0;
        if (bytepos >= buf.limit() - 1) {
            return -1;
        }
        while (fillbits > 0) {
            int nextbits = buf.get(bytepos);  // bitsource
            int bitsfromhere = 8 - bitpos;  // how many bits can we take?
            if (bitsfromhere > fillbits) { // don't take more than we need
                bitsfromhere = fillbits;
            }
            value |= ((nextbits >> (8 - bitpos - bitsfromhere)) &
                    (0xff >> (8 - bitsfromhere))) << (fillbits - bitsfromhere);
            fillbits -= bitsfromhere;
            bitpos += bitsfromhere;
            if (bitpos >= 8) {
                bitpos = 0;
                bytepos++;
            }
        }
        return value;
    }

    /**
     * decode the array.
     * @return the uncompressed byte array
     */
    private ByteBuffer decode() throws PdfFilterException {
        // algorithm derived from:
        // http://www.rasip.fer.hr/research/compress/algorithms/fund/lz/lzw.html
        // and the PDFReference
        int cW = CLEARDICT;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bufSize = buf.remaining();
        while (true) {
            int pW = cW;
            cW = nextCode();
            if (cW == -1) {
                throw new PdfFilterException("Missed the stop code in LZWDecode!", baos, bufSize - buf.remaining());
            }
            if (cW == STOP) {
                break;
            } else if (cW == CLEARDICT) {
                resetDict();
            //		pW= -1;
            } else if (pW == CLEARDICT) {
                baos.write(dict[cW], 0, dict[cW].length);
            } else {
                if (cW < dictlen) {  // it's a code in the dictionary
                    baos.write(dict[cW], 0, dict[cW].length);
                    byte[] p = new byte[dict[pW].length + 1];
                    System.arraycopy(dict[pW], 0, p, 0, dict[pW].length);
                    p[dict[pW].length] = dict[cW][0];
                    dict[dictlen++] = p;
                } else {  // not in the dictionary (should==dictlen)
                    //		    if (cW!=dictlen) {
                    //			System.out.println("Got a bouncy code: "+cW+" (dictlen="+dictlen+")");
                    //		    }
                    byte[] p = new byte[dict[pW].length + 1];
                    System.arraycopy(dict[pW], 0, p, 0, dict[pW].length);
                    p[dict[pW].length] = p[0];
                    baos.write(p, 0, p.length);
                    dict[dictlen++] = p;
                }
                if (dictlen >= (1 << bitspercode) - 1 && bitspercode < 12) {
                    bitspercode++;
                }
            }
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }

    /**
     * decode an array of LZW-encoded bytes to a byte array.
     *
     * @param buf the buffer of encoded bytes
     * @param params parameters for the decoder (unused)
     * @return the decoded uncompressed bytes
     */
    public static ByteBuffer decode(ByteBuffer buf, PDFObject params)
 throws IOException, PdfFilterException {
        // decode the array
        LZWDecode me = new LZWDecode(buf);
        ByteBuffer outBytes = me.decode();

        // undo a predictor algorithm, if any was used
        if (params != null && params.getDictRef("Predictor") != null) {
            Predictor predictor = Predictor.getPredictor(params);
            if (predictor != null) {
                outBytes = predictor.unpredict(outBytes);
            }
        }

        return outBytes;
    }
}