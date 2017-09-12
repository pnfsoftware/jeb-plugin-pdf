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

package com.pnf.plugin.pdf.obj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnf.plugin.pdf.filter.ByteBufferUtils;
import com.pnf.plugin.pdf.filter.Decoder;
import com.pnf.plugin.pdf.filter.FilterFactory;
import com.pnf.plugin.pdf.filter.IFilter;
import com.pnf.plugin.pdf.filter.PDFObject;
import com.pnf.plugin.pdf.parser.PdfSpecialCharacters;
import com.pnfsoftware.jeb.core.units.WellKnownUnitTypes;
import com.pnfsoftware.jeb.util.format.Strings;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerCustomInit;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;
import com.pnfsoftware.jeb.util.serialization.annotations.SerTransient;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.decrypt.PDFDecrypter;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfStream extends AbstractPdfParsableAttribute implements Comparable<PdfStream> {
    private static final ILogger logger = GlobalLog.getLogger(PdfStream.class);

    /**
     * Infered type from data
     */
    @Ser
    public enum StreamType {
        XML(new byte[][]{"<?xpacket".getBytes(), "<?xml".getBytes()}), // xml
        ADOBE_FONT("%!PS-AdobeFont".getBytes()), // font
        Flash(new byte[][]{"CWS".getBytes(), "FWS".getBytes(), "ZWS".getBytes()}), // flash headers
        U3D("U3D".getBytes()), // U3D
        Stream("".getBytes()), // default
        Javascript("".getBytes()), // JS
        Script("".getBytes()),
        Contents("".getBytes()),
        XFA_Fragment("".getBytes()),
        XFA("".getBytes());

        private byte[][] startBy;

        private StreamType(byte[][] startBy) {
            this.startBy = startBy;
        }

        private StreamType(byte[] startBy) {
            this.startBy = new byte[][]{startBy};
        }

        public byte[][] getStartBy() {
            return startBy;
        }

        public static StreamType getStreamType(byte[] data) {
            for(StreamType type: StreamType.values()) {
                if(isType(data, type)) {
                    return type;
                }
            }
            return Stream;
        }

        private static boolean isType(byte[] data, StreamType type) {
            for(byte[] startBy: type.getStartBy()) {
                boolean isType = true;
                for(int i = 0; i < startBy.length; i++) {
                    if(i >= data.length || data[i] != startBy[i]) {
                        isType = false;
                        break;
                    }
                }
                if(isType) {
                    return true;
                } // else, see next element
            }
            return false;
        }
    }

    @SerId(1)
    /** Dictionary attached to this stream */
    private PdfDictionary dictionary;
    @SerId(2)
    /** encoded data */
    private byte[] encodedData;
    @SerId(3)
    /** decoded data */
    private byte[] decodedData;
    @SerId(4)
    /** list of intermediate encoded data (when multiple filters/encryption) */
    private List<byte[]> encodedDataList = new ArrayList<byte[]>();

    @SerId(5)
    private StreamType streamType; // when no type is internally defined

    @SerId(6)
    /** start byte of encoded data */
    private int fromByte;
    @SerId(7)
    /** end byte of encoded data */
    private int toByte;

    @SerId(8)
    /** objects defined in stream if type is ObjStm */
    private Map<PdfObjId, PdfIndirectObj> objStmChildren = null;

    @SerId(9)
    /** Indicate the objects that represent a collection */
    private Map<PdfObjId, PdfIndirectObj> extendedByObjects = null;

    @SerTransient
    /** Only for XRef Stream */
    private PdfXref xref;

    @SerTransient
    /** extra characters after end of stream to check size */
    private byte[] extraChars = new byte[2];

    @SerCustomInit
    private void init() {
        extraChars = new byte[2];
    }

    public PdfStream(PdfDictionary dictionary, int startIndex) {
        super(dictionary.getParent(), startIndex); // same parent
        this.dictionary = dictionary;
    }

    /**
     * Build a stream from a list of streams (this happens when the stream is splitted into several
     * parts)
     */
    public PdfStream(PdfDictionary dictionary, List<PdfStream> xfaFragments) throws IOException {
        super(dictionary.getParent(), dictionary.getStartIndex());
        this.dictionary = dictionary;
        ByteArrayOutputStream encodedDataByteArray = new ByteArrayOutputStream();
        for(PdfStream xfaStream: xfaFragments) {
            encodedDataByteArray.write(xfaStream.getDecodedData());
        }
        encodedData = encodedDataByteArray.toByteArray();
        decodedData = encodedData;
    }

    /**
     * Build a stream from a simple Pdf String
     */
    public PdfStream(PdfDictionary dictionary, PdfString pdfString) {
        this(dictionary, pdfString.toString().getBytes());
    }

    public PdfStream(PdfDictionary dictionary, byte[] data) {
        super(dictionary.getParent(), dictionary.getStartIndex());
        this.dictionary = dictionary;
        encodedData = data;
        decodedData = encodedData;
    }

    @Override
    public int parse(byte[] data, int cursor) {
        cursor = retrieveStartOfStream(data, cursor);
        fromByte = cursor;
        for(; cursor < data.length; cursor++) {
            if(isEndToken(data, cursor)) {
                break;
            }
        }

        toByte = retrieveEndOfStream(data, cursor);
        if(toByte < fromByte) {
            // can happen when stream is empty with stream 0D 0A endstream
            // => start of stream is after 0A, end of stream should be before 0D
            toByte = fromByte - 1;
        }
        encodedData = new byte[getParsedLength()];
        System.arraycopy(data, fromByte, encodedData, 0, getParsedLength());
        if((toByte + 2) < data.length) {
            extraChars[0] = data[toByte + 1];
            extraChars[1] = data[toByte + 2];
        }
        return cursor;
    }

    private int retrieveStartOfStream(byte[] data, int cursor) {
        cursor++;
        cursor += PdfSpecialCharacters.STREAM_START_SEPARATOR.length;
        int endLine = PdfSpecialCharacters.testEndLine(data, cursor);
        if(endLine == 0) {
            logger.error("Stream begins at end of stream");
        }
        else if(endLine == -1) {
            logger.warn("Missing EOL character after stream token at address %x", cursor);
        }
        else {
            cursor += endLine;
        }
        return cursor;
    }

    private int retrieveEndOfStream(byte[] data, int cursor) {
        cursor--;
        int endLine = PdfSpecialCharacters.testPreviousEndLine(data, cursor);
        if(endLine == -1) {
            // NO EOL character
        }
        else {
            cursor -= endLine;
        }
        return cursor;
    }

    public boolean isEndToken(byte[] data, int cursor) {
        return PdfSpecialCharacters.isEndStream(data, cursor);
    }

    public String getName() {
        StringBuilder stb = new StringBuilder(getId().toString());
        String type = getStreamType();
        if(!Strings.isBlank(type)) {
            stb.append(" (").append(type).append(")");
        }
        return stb.toString();
    }

    @Override
    public String toString() {
        return String.format("Stream %s", dictionary.toString());
    }

    @Override
    public Type getType() {
        return Type.Stream;
    }

    public PdfDictionary getDictionary() {
        return dictionary;
    }

    public byte[] getEncodedData() {
        return encodedData;
    }

    public byte[] getDecodedData() {
        if(decodedData == null) {
            decodeStream();
        }
        return decodedData;
    }

    /** Search for an attribute value. It is an indirect reference, return the correct object */
    public IPdfAttribute getAttribute(String name) {
        return dictionary.getAttribute(name);
    }

    public Integer getLengthFromDictionary() {
        IPdfAttribute attribute = getAttribute("/Length");
        if(attribute == null) {
            // no length defined
        }
        else if(attribute.getType() == Type.Number) {
            return Integer.valueOf(attribute.toString());
        }
        else {
            getPdfStatictics().addUnitNotification(this, SuspiciousType.Malformed,
                    String.format("Unable to parse [Stream %s]/Length: expected number", getId()));
        }
        return null;
    }

    public int getParsedLength() {
        return toByte - fromByte + 1;
    }

    public List<IFilter> getFilters(IPdfAttribute decodeParms, PDFDecrypter decrypter) {
        return getFilters(getAttribute("/Filter"), decodeParms, decrypter);
    }

    private List<IFilter> getFilters(IPdfAttribute attribute, IPdfAttribute decodeParms, PDFDecrypter decrypter) {
        List<IFilter> filters = new ArrayList<IFilter>();
        if(attribute == null) {
            // no Filter
        }
        else if(attribute.getType() == Type.Name) {
            filters.add(FilterFactory.getFilterInstance(this, (PdfName)attribute, decodeParms, getPdfStatictics(),
                    decrypter));
        }
        else if(attribute.getType() == Type.Array) {
            PdfArray array = (PdfArray)attribute;
            if(array.getAttributes().size() > 1) {
                getPdfStatictics().addUnitNotification(this, SuspiciousType.StreamWithMultipleFilters,
                        String.format("[Stream %s] has several filters", getId()));
            }
            PdfArray decodeParmsArray = null;
            if(decodeParms != null && decodeParms.getType() != Type.Array) {
                getPdfStatictics().addUnitNotification(this, SuspiciousType.Malformed,
                        String.format("Unable to parse [Stream %s]/DecodeParms: expected array", getId()));
                decodeParms = null;
            }
            else {
                decodeParmsArray = (PdfArray)decodeParms;
            }
            for(int i = 0; i < array.getAttributes().size(); i++) {
                filters.addAll(
                        getFilters(array.getAttributes().get(i), getDecodeParms(decodeParmsArray, i), decrypter));
            }
        }
        else {
            getPdfStatictics().addUnitNotification(this, SuspiciousType.Malformed,
                    String.format("Unable to parse [Stream %s]/Filter: expected name or array", getId()));
        }
        return filters;
    }

    private IPdfAttribute getDecodeParms(PdfArray decodeParmsArray, int i) {
        if(decodeParmsArray == null || decodeParmsArray.getAttributes().size() <= i) {
            return null;
        }
        IPdfAttribute attribute = decodeParmsArray.getAttributes().get(i);
        if(attribute.getType() == Type.IndirectReference) {
            return PdfIndirectReference.retrieveDirectObject(attribute);
        }
        return attribute;
    }

    public void decodeStream() {
        if(decodedData != null) {
            return;
        }
        PDFDecrypter decrypter = getMainParent().getDecrypter();
        boolean isEncrypted = isEncrypted();
        IPdfAttribute extFile = getAttribute("/F");
        if(extFile != null && extFile.getType() != Type.Null) {
            getPdfStatictics().addUnitNotification(this, SuspiciousType.StreamUnfiltered,
                    "External Stream data is not implemented");
            decodedData = new byte[0];
            return;
        }
        byte[] rawData = encodedData;

        if(isEncrypted) {
            encodedDataList.add(rawData); // add encrypted
            if(decrypter != null) {
                ByteBuffer in = ByteBufferUtils.getByteBuffer(rawData, 0, getParsedLength());
                try {
                    ByteBuffer out = decrypter.decryptBuffer(null, PDFObject.getInstance(getParent()), in);
                    rawData = out.array();
                }
                catch(PDFParseException e) {
                    logger.catching(e);
                    getPdfStatictics().addUnitNotification(this, SuspiciousType.StreamUnfiltered, "Encrypted");
                    decodedData = new byte[0];
                    return;
                }
            }
            else {
                getPdfStatictics().addUnitNotification(this, SuspiciousType.StreamUnfiltered, "Encrypted");
                decodedData = new byte[0];
                return;
            }
        }

        IPdfAttribute decodeParms = getAttribute("/DecodeParms");
        List<IFilter> filters = getFilters(decodeParms, decrypter);
        if(filters.isEmpty()) {
            decodedData = rawData;
        }
        else {
            Decoder d = new Decoder(dictionary);
            encodedDataList.add(rawData);
            try {
                if(isEncrypted) {
                    rawData = d.parse(0, filters.get(0), rawData);
                }
                else {
                    rawData = d.parse(0, filters.get(0), rawData, encodedData, fromByte, getLengthFromDictionary());
                }
                if(filters.size() > 1) {
                    encodedDataList.add(rawData);
                    for(int i = 1; i < filters.size(); i++) {
                        if(rawData.length == 0) {
                            decodedData = new byte[0];
                            return; // a previous filter has nullify result
                        }
                        rawData = d.parse(i, filters.get(i), rawData);
                        if(i < filters.size() - 1) {
                            encodedDataList.add(rawData);
                        }
                    }
                }
                if(rawData.length != 0) {
                    decodedData = rawData;
                }
                else {
                    decodedData = new byte[0];
                }
                if(isDecodedStreamTooBig()) {
                    getPdfStatictics().addUnitNotification(this, SuspiciousType.PotentialHarmfulFile,
                            String.format("Decoded stream is %d Mb", decodedData.length / 1_000_000,
                                    getMaxDecodedSize() / 1_000_000));
                    // byte[] newDecodedData = new byte[getMaxDecodedSize()];
                    // System.arraycopy(decodedData, 0, newDecodedData, 0, getMaxDecodedSize());
                    // decodedData = newDecodedData;
                }

                if(d.getDecodingError() != null) {
                    logger.error(
                            "Unable to parse Stream %s over filter [%d] error while decoding: %s. Processed bytes: %d",
                            getId(), d.getFilterIndex(), d.getDecodingError(), d.getDecodingError().getProcessed());
                    getPdfStatictics().addUnitNotification(this, SuspiciousType.MalformedStream,
                            "Unable to parse Stream: filter failed");
                }
            }
            catch(Exception e) {
                e.printStackTrace();
                logger.error("Unable to parse Stream %s can not be read: %s", getId(), e.getMessage());
                getPdfStatictics().addUnitNotification(this, SuspiciousType.MalformedStream,
                        "Unable to parse Stream: filter failed");
                decodedData = new byte[0];
            }
        }
    }

    public void checkSize() {
        Integer lengthFromDictionary = getLengthFromDictionary();
        if(lengthFromDictionary == null) {
            logger.warn("No Length defined. Parser found %d", getParsedLength());
        }
        else if(getParsedLength() != lengthFromDictionary) {
            boolean extraEOL = PdfSpecialCharacters.isEndLine(extraChars[0]);
            boolean extra2ndEOL = PdfSpecialCharacters.isEndLine(extraChars[1]);
            boolean lastIsEOL = encodedData.length > 0 ? (encodedData[getParsedLength() - 1] == 0x0D): true;
            if((getParsedLength() + 1 == lengthFromDictionary && extraEOL)
                    || (getParsedLength() + 2 == lengthFromDictionary && extraEOL && extra2ndEOL)) {
                // there can be an extra EOL marker as said in spec
                // parser did not count the last EOL
            }
            else if(getParsedLength() == lengthFromDictionary + 1 && lastIsEOL) {
                // the extra EOL marker is a 0x0D which is normally illegal, but it happens sometimes
            }
            else {
                logger.warn("Length defined: %s. Parser found %d", lengthFromDictionary.toString(), getParsedLength());
            }
        }
    }

    private PdfName getLastFilter() {
        IPdfAttribute filter = getAttribute("/Filter");
        if(filter == null) {
            return null;
        }
        else if(filter.getType() == Type.Name) {
            return (PdfName)filter;
        }
        else if(filter.getType() == Type.Array) {
            List<IPdfAttribute> filters = ((PdfArray)filter).getAttributes();
            return (PdfName)PdfDictionary.retrieveDirectObject(filters.get(filters.size() - 1));
        }
        return null;
    }

    public boolean isBinaryOnlyDisplay() {
        return isDecodedStreamTooBig() || getStreamType().endsWith("Image");
    }

    private boolean isDecodedStreamTooBig() {
        return decodedData != null && decodedData.length > getMaxDecodedSize();
    }

    private int getMaxDecodedSize() {
        return 10_000_000;
    }

    public String getWantedType() {
        if(streamType == null) {
            return null;
        }
        switch(streamType) {
        case Javascript:
            return WellKnownUnitTypes.typeJavaScript;
        case XML:
            return WellKnownUnitTypes.typeXml;
        default:
            return null;
        }
    }

    public String getStreamType() {
        if(streamType != null) {
            return streamType.toString();
        }
        if(dictionary.getDictionaryFullType() != null) {
            return dictionary.getDictionaryFullType();
        }
        // try to inferate type
        if(decodedData != null) {
            StreamType type = StreamType.getStreamType(decodedData);
            if(type == StreamType.Stream) {
                // other determination
            }
            return type.toString();
        }
        return StreamType.Stream.toString();
    }

    public void addObjStmChild(PdfIndirectOjbStm newobj) {
        if(objStmChildren == null) {
            objStmChildren = new HashMap<PdfObjId, PdfIndirectObj>();
        }
        objStmChildren.put(newobj.getId(), newobj);
    }

    public List<PdfObjId> getObjStmId() {
        if(objStmChildren == null) {
            return new ArrayList<PdfObjId>();
        }
        List<PdfObjId> ids = new ArrayList<PdfObjId>(objStmChildren.keySet());
        Collections.sort(ids);
        return ids;
    }

    public boolean isObjStm() {
        return "/ObjStm".equals(this.getDictionary().getDictionaryType());
    }

    public List<PdfIndirectObj> getObjStmList() {
        if(objStmChildren == null) {
            return new ArrayList<PdfIndirectObj>();
        }
        List<PdfIndirectObj> list = new ArrayList<PdfIndirectObj>(objStmChildren.values());
        Collections.sort(list);
        return list;
    }

    public void addExtendedBy(PdfIndirectObj stream) {
        if(extendedByObjects == null) {
            extendedByObjects = new HashMap<PdfObjId, PdfIndirectObj>();
        }
        extendedByObjects.put(stream.getId(), stream);
    }

    public boolean hasExtendedStream() {
        return extendedByObjects != null;
    }

    public boolean isObjStmExtends() {
        return isObjStm() && getAttribute("/Extends") != null;
    }

    public List<PdfIndirectObj> getExtendedByList() {
        if(extendedByObjects == null) {
            return new ArrayList<PdfIndirectObj>();
        }
        List<PdfIndirectObj> list = new ArrayList<PdfIndirectObj>(extendedByObjects.values());
        Collections.sort(list);
        return list;
    }

    public List<byte[]> getEncodedDataList() {
        return encodedDataList;
    }

    public void setType(StreamType type) {
        streamType = type;
    }

    public boolean isEncrypted() {
        boolean isEncrypted = getMainParent().isEncrypted();
        if("/XRef".equals(dictionary.getDictionaryType())) {
            isEncrypted = false; // XRef is not encrypted
        }
        return isEncrypted;
    }

    public String getAsText() {
        if(decodedData == null || decodedData.length == 0) {
            return null;
        }

        if("/XRef".equals(dictionary.getDictionaryType())) {
            try {
                if(xref == null) {
                    xref = new PdfXref(this);
                }
                return xref.getXRefText();
            }
            catch(Exception e) {
                logger.catching(e);
                return null;
            }
        }
        return null;
    }

    public boolean isImage() {
        return getStreamType().equals("/XObject/Image");
    }

    public boolean isJpeg() {
        PdfName lastFilter = getLastFilter();
        return isImage() && (lastFilter != null
                && (lastFilter.toString().equals("/DCTDecode") || lastFilter.toString().equals("/DCT")));
    }

    @Override
    public int compareTo(PdfStream o) {
        return getId().compareTo(o.getId());
    }

}
