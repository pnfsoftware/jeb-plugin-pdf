package com.sun.pdfview.decode;

import java.io.IOException;

import com.pnf.plugin.pdf.filter.PDFObject;

public class CCITTFaxDecode {

    // protected static ByteBuffer decode(PDFObject dict, ByteBuffer buf,
    // PDFObject params) throws IOException {
    //
    // byte[] bytes = new byte[buf.remaining()];
    // buf.get(bytes, 0, bytes.length);
    // return ByteBuffer.wrap(decode(dict, bytes));
    // }

    public static byte[] decode(PDFObject dict, PDFObject decodeParms, byte[] source) throws IOException {
        int width = 1728;
        PDFObject widthDef = dict.getDictRef("Width");
        if (widthDef == null) {
            widthDef = dict.getDictRef("W");
        }
        if (widthDef != null) {
            width = widthDef.getIntValue();
        }
        int height = 0;
        PDFObject heightDef = dict.getDictRef("Height");
        if (heightDef == null) {
            heightDef = dict.getDictRef("H");
        }
        if (heightDef != null) {
            height = heightDef.getIntValue();
        }

        //
        int columns = getOptionFieldInt(decodeParms, "Columns", width);
        int rows = getOptionFieldInt(decodeParms, "Rows", height);
        int k = getOptionFieldInt(decodeParms, "K", 0);
        int size = rows * ((columns + 7) >> 3);
        byte[] destination = new byte[size];

        boolean align = getOptionFieldBoolean(decodeParms, "EncodedByteAlign", false);

        CCITTFaxDecoder decoder = new CCITTFaxDecoder(1, columns, rows);
        decoder.setAlign(align);
        if (k == 0) {
            decoder.decodeT41D(destination, source, 0, rows);
        } else if (k > 0) {
            decoder.decodeT42D(destination, source, 0, rows);
        } else if (k < 0) {
            decoder.decodeT6(destination, source, 0, rows);
        }
        if (!getOptionFieldBoolean(decodeParms, "BlackIs1", false)) {
            for (int i = 0; i < destination.length; i++) {
                // bitwise not
                destination[i] = (byte) ~destination[i];
            }
        }

        return destination;
    }

    public static int getOptionFieldInt(PDFObject dictParams, String name, int defaultValue) throws IOException {
        if (dictParams == null) {
            return defaultValue;
        }
        PDFObject value = dictParams.getDictRef(name);
        if (value == null) {
            return defaultValue;
        }
        return value.getIntValue();
    }

    public static boolean getOptionFieldBoolean(PDFObject dictParams, String name, boolean defaultValue)
            throws IOException {
        if (dictParams == null) {
            return defaultValue;
        }
        PDFObject value = dictParams.getDictRef(name);
        if (value == null) {
            return defaultValue;
        }
        return value.getBooleanValue();
    }

}
