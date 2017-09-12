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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.pnf.plugin.pdf.PdfFileUnit;
import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfIndirectOjbStm;
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfNumber;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnf.plugin.pdf.obj.PdfStream.StreamType;
import com.pnf.plugin.pdf.obj.PdfString;
import com.pnf.plugin.pdf.obj.PdfTrailer;
import com.pnfsoftware.jeb.util.io.IO;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfParser {
    private static final ILogger logger = GlobalLog.getLogger(PdfParser.class);

    /** Master streams to be displayed (all except /Extends ones) */
    private Set<PdfStream> streams = new TreeSet<PdfStream>();

    /** Decoded strings */
    private List<PdfString> strings = new ArrayList<PdfString>();

    private final PdfFileUnit unit;

    /** Simulates a generation number for trailer */
    private int trailerCount = 0;

    /** Objects displayed in simple view: all elements encountered with stream filtered */
    private List<Object> simpleView = new ArrayList<Object>();

    public PdfParser(PdfFileUnit unit) {
        this.unit = unit;
    }

    /**
     * Parse Bytes representing a PDF. Once an object is detected, it is processed in
     * {@link IPdfParsable#parse(byte[], int)}
     */
    public void parse(InputStream data) throws IOException {
        parseBytes(data);
        logger.debug("PDF Ojbect parsing terminated. Parsing streams...");
        PdfStatistics statistics = unit.getStatistics();

        statistics.setNbIndirectObjects(unit.getObjects().size());

        processObjStm();
        processDecryptionAndDecoding();
        statistics.setNbStreams(streams.size());
        statistics.setNbStreamedObjects(unit.getObjects().size() - statistics.getNbIndirectObjects());
        processFuntional();

        logger.debug("PDF Streams parsed successfully.");
        logger.debug("Found %d indirect objects", unit.getObjects().size());
    }

    private void parseBytes(InputStream input) throws IOException {
        int cursor = 0;
        byte[] data = IO.readInputStream(input);
        // Map<PdfObjId, PdfIndirectObj> objects = new TreeMap<PdfObjId, PdfIndirectObj>();
        PdfFile file = new PdfFile(unit);
        List<PdfAttributeValue> nonProcessedAttributes = new ArrayList<PdfAttributeValue>();
        int xrefStart = -1;
        for(cursor = 0; cursor < data.length; cursor++) {
            if(PdfSpecialCharacters.isSeparator(data[cursor])) {
                // ignore spacings
            }
            else if(PdfSpecialCharacters.isEOF(data, cursor)) {
                cursor = cursor + PdfSpecialCharacters.EOF.length;
                postProcessDictionaries(file);
                file = new PdfFile(unit);
                xrefStart = -1;
                simpleView.add(new String(PdfSpecialCharacters.EOF));
            }
            else if(PdfSpecialCharacters.isComment(data[cursor])) {
                cursor = PdfComment.skipCommentsIfSome(data, cursor - 1);
            }
            else if(PdfSpecialCharacters.isStartObj(data, cursor)) {
                try {
                    PdfIndirectObj o = new PdfIndirectObj(file, unit, cursor);
                    cursor = o.parse(data, cursor, nonProcessedAttributes);
                    file.putObject(o.getId(), o);
                    unit.getObjects().put(o.getId(), file);
                    simpleView.add(o);
                }
                catch(Exception e) {
                    e.printStackTrace();
                    logger.error("Error while parsing obj at address %x", cursor);
                }
            }
            else if(PdfSpecialCharacters.isXref(data, cursor)) {
                xrefStart = cursor;
                while(cursor + 1 < data.length && isXrefChar(data[cursor + 1])) {
                    cursor++;
                }
            }
            else if(PdfSpecialCharacters.isStartXref(data, cursor)) {
                cursor = cursor + PdfSpecialCharacters.XREF_START_SEPARATOR.length;
                PdfAttributeValue value = new PdfAttributeValue(null, cursor);
                cursor = value.parseNexToken(data, cursor);
                try {
                    IPdfAttribute startxref = value.getPdfAttribute();
                    simpleView.add(new String(PdfSpecialCharacters.XREF_START_SEPARATOR) + " " + startxref.toString());
                    if(startxref.getType() == Type.Number) {
                        int startxrefPos = ((PdfNumber)startxref).intValue();
                        if(startxrefPos != 0) {
                            file.setStartXref(unit.getTrailers().get(startxrefPos));
                        }
                    }
                }
                catch(StartObjFoundException e) {
                    logger.catching(e);
                }
            }
            else if(PdfSpecialCharacters.isStartTrailer(data, cursor)) {
                try {
                    int startIndex = xrefStart == -1 ? cursor: xrefStart;
                    PdfTrailer o = new PdfTrailer(file, unit, startIndex, cursor, trailerCount);
                    cursor = o.parse(data, cursor);
                    file.putObject(o.getId(), o);
                    unit.getObjects().put(o.getId(), file);
                    trailerCount++;
                    file.setTrailer(o);
                    unit.getTrailers().put(startIndex, o);
                    // go back to startxref token
                    if(PdfSpecialCharacters.isStartXref(data,
                            cursor - PdfSpecialCharacters.XREF_START_SEPARATOR.length)) {
                        cursor = cursor - PdfSpecialCharacters.XREF_START_SEPARATOR.length - 1;
                    }
                    simpleView.add(o);
                }
                catch(Exception e) {
                    e.printStackTrace();
                    logger.error("Error while parsing obj at address %x", cursor);
                }
            }
            else {
                PdfAttributeValue attribute = new PdfAttributeValue(null, cursor);
                cursor = attribute.parse(data, cursor);
                nonProcessedAttributes.add(attribute);
            }
        }
        if(file.getObjectNumber() != 0) {
            postProcessDictionaries(file);
        }
    }

    /**
     * Quick function to jump xref
     */
    private boolean isXrefChar(byte b) {
        return PdfSpecialCharacters.isSeparator(b) || Character.isDigit((char)b) || b == 'n' || b == 'f';
    }

    private void postProcessDictionaries(PdfFile file) {
        // determine some attributes that can be indirect references (type, subtype...)

        List<PdfIndirectObj> objectList = file.getObjectList();

        PdfDictionary dictionary = null;
        for(PdfIndirectObj o: objectList) {
            try {
                dictionary = null;
                switch(o.getAttribute().getType()) {
                case Stream:
                    dictionary = ((PdfStream)o.getAttribute()).getDictionary();
                    break;
                case Dictionary:
                    dictionary = ((PdfDictionary)o.getAttribute());
                    break;
                default:
                    break;
                }
                if(dictionary != null) {
                    if("/XRef".equals(dictionary.getDictionaryType())) {
                        // Report Xref as trailer
                        PdfTrailer t = new PdfTrailer(file, dictionary, unit, trailerCount);
                        file.putObject(t.getId(), t);
                        trailerCount++;
                        file.setTrailer(t);
                        unit.getTrailers().put(t.getStartIndex(), t);
                    }
                }
            }
            catch(Exception e) {
                logger.catching(e);
            }
        }
    }

    /**
     * Process some check + decrypt streams: streams can not be processed in standard flow since
     * /filter can be an indirect reference to an object which is still not processed.
     */
    private void processDecryptionAndDecoding() {
        for(Entry<PdfObjId, PdfFile> objectEntrySet: unit.getObjects().entrySet()) {
            PdfFile file = objectEntrySet.getValue();
            PdfIndirectObj o = file.getObject(objectEntrySet.getKey());
            try {
                switch(o.getAttribute().getType()) {
                case Stream:
                    PdfStream stream = (PdfStream)o.getAttribute();

                    stream.checkSize();

                    // decrypt
                    stream.decodeStream();

                    if(!stream.isObjStmExtends()) {
                        // extends streams are bound to their parent stream,
                        // so they won't be displayed in the main stream tree
                        streams.add(stream);
                    }
                    break;
                default:
                    break;
                }

                if(o.getType() == Type.IndirectObject && file.getStringDecrypt() != null
                        && !o.getId().equals(file.getEncryptDictionary().getId())) {
                    // when encryption is set, decrypt all string except /U and /O
                    // TODO can also be indirect references? Need to manage this
                    file.getStringDecrypt().browseElement(o.getAttribute(), null);
                }
            }
            catch(Exception e) {
                logger.catching(e);
            }
        }
    }

    private void processObjStm() {
        // Unpack ObjStm streams first in case they contains necessary data for other objects
        Map<PdfObjId, PdfFile> createdObjects = new TreeMap<PdfObjId, PdfFile>();
        for(Entry<PdfObjId, PdfFile> objectEntrySet: unit.getObjects().entrySet()) {
            PdfIndirectObj o = objectEntrySet.getValue().getObject(objectEntrySet.getKey());
            try {
                switch(o.getAttribute().getType()) {
                case Stream:
                    PdfStream stream = (PdfStream)o.getAttribute();

                    // set extends hierarchy
                    PdfStream extendsAttribute = (PdfStream)stream.getAttribute("/Extends");
                    if(extendsAttribute != null) {
                        extendsAttribute.addExtendedBy(o);
                    }

                    // decode objStm
                    if(stream.isObjStm()) {
                        // decrypt
                        stream.decodeStream();
                        if(stream.getDecodedData().length > 0) {
                            // parse sub object
                            byte[] data = ((PdfStream)o.getAttribute()).getDecodedData();
                            int n = Integer.valueOf(stream.getAttribute("/N").toString());
                            int firstOffset = Integer.valueOf(stream.getAttribute("/First").toString());
                            int idCursor = 0;
                            for(int i = 0; i < n; i++) {
                                // parse id
                                PdfAttributeValue pdfValue = new PdfAttributeValue(stream, 0);
                                idCursor = pdfValue.parseNexToken(data, idCursor) + 1;
                                IPdfAttribute objNumber = PdfIndirectReference
                                        .retrieveDirectObject(pdfValue.getPdfAttribute());

                                // start offset
                                pdfValue = new PdfAttributeValue(stream, 0);
                                idCursor = pdfValue.parseNexToken(data, idCursor) + 1;
                                IPdfAttribute startOffset = PdfIndirectReference
                                        .retrieveDirectObject(pdfValue.getPdfAttribute());

                                int startIndex = firstOffset + Integer.valueOf(startOffset.toString());
                                PdfFile file = objectEntrySet.getValue();
                                PdfIndirectOjbStm newobj = new PdfIndirectOjbStm(file, unit, startIndex);
                                newobj.parse(data, startIndex, objNumber.toString());

                                file.putObject(newobj.getId(), newobj);
                                createdObjects.put(newobj.getId(), file);

                                stream.addObjStmChild(newobj);
                            }
                        }
                    }
                    break;
                default:
                    break;
                }
            }
            catch(Exception e) {
                logger.catching(e);
                logger.error("Unable to parse Stream %s, an error occurred during parsing ObjStm: %s", o.getId(),
                        e.getMessage());
                if(o.getAttribute() != null) {
                    unit.getStatistics().addUnitNotification(o.getAttribute(), SuspiciousType.MalformedStream,
                            String.format("Unable to parse Stream/ObjStm stream"), e);
                }
            }
        }
        unit.getObjects().putAll(createdObjects);
    }

    private void processFuntional() {
        for(Entry<PdfObjId, PdfFile> objectEntrySet: unit.getObjects().entrySet()) {
            PdfIndirectObj o = objectEntrySet.getValue().getObject(objectEntrySet.getKey());
            processFunctionalElement(o.getAttribute());
        }
    }

    private void processFunctionalElement(IPdfAttribute attribute) {
        try {
            switch(attribute.getType()) {
            case Stream:
                break;
            case Dictionary:
                processFunctionalDictionary((PdfDictionary)attribute);
                break;
            case Array:
                PdfArray array = ((PdfArray)attribute);
                for(IPdfAttribute attribute2: array.getAttributes()) {
                    processFunctionalElement(attribute2);
                }
                break;
            default:
                break;
            }
        }
        catch(Exception e) {
            logger.catching(e);
        }
    }

    private void processFunctionalDictionary(PdfDictionary dict) throws IOException {
        IPdfAttribute actionType = dict.getAttribute("/S");
        if(actionType != null && "/JavaScript".equals(actionType.toString())) {
            IPdfAttribute js = dict.getAttribute("/JS");
            if(js == null) {
                // no JS? Probably dead obj
            }
            else if(js.getType() == Type.Stream) {
                ((PdfStream)js).setType(StreamType.Javascript);
            }
            else if(js.getType() == Type.String) {
                strings.add((PdfString)js);
                // rebuild JS Stream for better delegation
                PdfStream jsStream = new PdfStream(dict, (PdfString)js);
                jsStream.setType(StreamType.Javascript);
                streams.add(jsStream);
            }
            else {
                logger.error("Can not manage /JS type for dictionary %s", dict.getId());
            }
        }
        if("/Page".equals(dict.getDictionaryType())) {
            IPdfAttribute contents = dict.getAttribute("/Contents");
            if(contents == null) {
            }
            else if(contents.getType() == Type.Stream) {
                ((PdfStream)contents).setType(StreamType.Contents);
            }
            else if(contents.getType() == Type.Array) {
                List<IPdfAttribute> arrayAttributes = ((PdfArray)contents).getAttributes();
                for(IPdfAttribute c: arrayAttributes) {
                    IPdfAttribute content = PdfDictionary.retrieveDirectObject(c);
                    if(content == null) {
                    }
                    else if(content.getType() == Type.Stream) {
                        ((PdfStream)content).setType(StreamType.Contents);
                    }

                }
            }
        }
        else if("/Catalog".equals(dict.getDictionaryType())) {
            IPdfAttribute acroform = dict.getAttribute("/AcroForm");
            if(acroform != null && acroform.getType() == Type.Dictionary) {
                //((PdfDictionary) acroform).setType(StreamType.Contents);
                IPdfAttribute xfaRaw = ((PdfDictionary)acroform).getAttribute("/XFA");
                if(xfaRaw != null) {
                    if(xfaRaw.getType() == Type.Stream) {
                        ((PdfStream)xfaRaw).setType(StreamType.XFA);
                    }
                    else if(xfaRaw.getType() == Type.Array) {
                        // retrieve all streams
                        List<PdfStream> xfaFragments = new ArrayList<>();
                        //String description = "";
                        for(IPdfAttribute fragment: ((PdfArray)xfaRaw).getAttributes()) {
                            if(fragment.getType() == Type.IndirectReference) {
                                fragment = PdfIndirectReference.retrieveDirectObject(fragment);
                            }
                            if(fragment.getType() == Type.String) {
                                //description = ((PdfString)fragment).getDecryptedValue();
                            }
                            else if(fragment.getType() == Type.Stream) {
                                xfaFragments.add((PdfStream)fragment);
                                ((PdfStream)fragment).setType(StreamType.XFA_Fragment);
                            }
                        }
                        if(xfaFragments.size() == 1) {
                            xfaFragments.get(0).setType(StreamType.XFA);
                        }
                        else {
                            // rebuild XFA Stream
                            PdfStream xfaStream = new PdfStream((PdfDictionary)acroform, xfaFragments);
                            xfaStream.setType(StreamType.XFA);
                            streams.add(xfaStream);
                        }
                    }
                }
            }
        }

        // process Child recursively
        for(PdfDictionaryAttribute attribute: dict.getAttributes()) {
            processFunctionalElement(attribute.getValue());
        }
    }

    public Set<PdfStream> getStreams() {
        return streams;
    }

    public byte[] getSimpleView() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for(Object o: simpleView) {
            bos.write(o.toString().getBytes());
            bos.write(0x0A);
        }
        return bos.toByteArray();
    }

}
