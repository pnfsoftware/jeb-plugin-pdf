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

package com.pnf.plugin.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.pnf.plugin.pdf.PdfStatistics.SuspiciousType;
import com.pnf.plugin.pdf.address.IAddress;
import com.pnf.plugin.pdf.document.TreePdfDocument;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnf.plugin.pdf.obj.PdfStream.StreamType;
import com.pnf.plugin.pdf.obj.PdfTrailer;
import com.pnf.plugin.pdf.parser.PdfFile;
import com.pnf.plugin.pdf.parser.PdfParser;
import com.pnf.plugin.pdf.unit.BinaryStreamUnit;
import com.pnf.plugin.pdf.unit.IPdfUnit;
import com.pnf.plugin.pdf.unit.PdfObjStmUnit;
import com.pnf.plugin.pdf.unit.StreamUnitProvider;
import com.pnfsoftware.jeb.core.IUnitCreator;
import com.pnfsoftware.jeb.core.actions.ActionContext;
import com.pnfsoftware.jeb.core.actions.ActionXrefsData;
import com.pnfsoftware.jeb.core.actions.Actions;
import com.pnfsoftware.jeb.core.actions.IActionData;
import com.pnfsoftware.jeb.core.events.ClientNotification;
import com.pnfsoftware.jeb.core.events.ClientNotificationLevel;
import com.pnfsoftware.jeb.core.events.J;
import com.pnfsoftware.jeb.core.events.JebEvent;
import com.pnfsoftware.jeb.core.input.BytesInput;
import com.pnfsoftware.jeb.core.input.FileInputLocation;
import com.pnfsoftware.jeb.core.input.IInput;
import com.pnfsoftware.jeb.core.input.IInputLocation;
import com.pnfsoftware.jeb.core.output.AbstractTransientUnitRepresentation;
import com.pnfsoftware.jeb.core.output.IGenericDocument;
import com.pnfsoftware.jeb.core.output.IUnitFormatter;
import com.pnfsoftware.jeb.core.output.UnitFormatterUtil;
import com.pnfsoftware.jeb.core.output.text.impl.AsciiDocument;
import com.pnfsoftware.jeb.core.properties.IPropertyDefinitionManager;
import com.pnfsoftware.jeb.core.units.AbstractInteractiveBinaryUnit;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.core.units.IUnitProcessor;
import com.pnfsoftware.jeb.core.units.WellKnownUnitTypes;
import com.pnfsoftware.jeb.util.format.Strings;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;
import com.pnfsoftware.jeb.util.serialization.annotations.SerTransient;

/**
 * Main PDF File Unit. It contains all the objects parsed in a PDF file.
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfFileUnit extends AbstractInteractiveBinaryUnit implements IPdfUnit {
    private static final ILogger logger = GlobalLog.getLogger(PdfFileUnit.class);

    @SerId(1)
    private String identifier;
    /** All objects by object id bound to their pdf section in file */
    @SerId(2)
    private Map<PdfObjId, PdfFile> objects = new TreeMap<PdfObjId, PdfFile>();
    /** trailer list by buffer position */
    @SerId(3)
    private Map<Integer, PdfTrailer> trailers = new TreeMap<Integer, PdfTrailer>();
    @SerId(4)
    private byte[] simpleView;
    @SerId(5)
    private PdfStatistics statistics;

    @SerTransient
    private List<PdfIndirectObj> objectList;
    /** Built dynamically from objects */
    @SerTransient
    private AddressUtils addressManager;
    /** Built dynamically from objects */
    @SerTransient
    private CrossReferences crossReferences;

    public PdfFileUnit(String name, IInput data, IUnitProcessor unitProcessor, IUnitCreator parent,
            IPropertyDefinitionManager pdm) {
        super(null, data, PdfPlugin.TYPE, name, unitProcessor, parent, pdm);
        identifier = name + UUID.randomUUID().toString();
    }

    @Override
    public boolean process() {
        PdfParser parser = new PdfParser(this);
        try(InputStream is = getInput().getStream()) {
            parser.parse(is);
            simpleView = parser.getSimpleView();
        }
        catch(IOException e) {
            logger.catching(e);
        }

        // delegate streams
        for(PdfStream stream: parser.getStreams()) {
            try {
                IUnit streamUnit = null;
                if(stream.isObjStm()) {
                    streamUnit = new PdfObjStmUnit(this, stream, identifier, getStatistics());
                    // } else if (stream.getStreamType().equals("Javascript")) {
                    // streamUnit =
                    // unitProcessor.process(stream.getId().toString(), new
                    // BytesInput(stream.getDecodedData()),
                    // this, WellKnownUnitTypes.typeJavascript, false);
                }
                else if(stream.getStreamType().endsWith("XML")) { // TODO this is not really XML but XMP
                    streamUnit = getUnitProcessor().process(stream.getName(), new BytesInput(stream.getDecodedData()),
                            this, stream.getWantedType(), true);
                    streamUnit = StreamUnitProvider.wrap(streamUnit, stream, null);
                }
                else if(stream.isImage()) {
                    if(stream.isJpeg()) {
                        List<byte[]> encodedDataList = stream.getEncodedDataList();
                        streamUnit = getUnitProcessor().process(stream.getName(),
                                new BytesInput(encodedDataList.get(encodedDataList.size() - 1)), this);
                        streamUnit = StreamUnitProvider.wrap(streamUnit, stream, stream.getStreamType());
                    }
                    else {
                        // TODO manage PBM and PPM: do not decode since data is already raw
                        streamUnit = new BinaryStreamUnit(this, stream, identifier, false);
                    }
                }
                //else if(stream.isBinaryOnlyDisplay()) {
                //    streamUnit = new BinaryStreamUnit(stream, identifier, getUnitProcessor(), this, getPropertyDefinitionManager());
                //}
                else {
                    streamUnit = buildDefaultStreamUnit(stream);

                    if(stream.getStreamType().equals("XFA")) {

                        try {
                            XFAParser xfaParser = new XFAParser();
                            xfaParser.parse(stream.getDecodedData());
                            addXFAChildren(stream, streamUnit, xfaParser.getJavaScripts(), StreamType.Javascript,
                                    WellKnownUnitTypes.typeJavaScript);
                            addXFAChildren(stream, streamUnit, xfaParser.getScripts(), StreamType.Script, null);
                        }
                        catch(Exception e) {
                            logger.catching(e);
                            logger.error("Error while processing XFA %s", stream.getId());
                        }

                    }
                }
                if(streamUnit == null) {
                    logger.error("Can not create specific unit for stream %s", stream.getId());
                    streamUnit = new BinaryStreamUnit(this, stream, identifier, true);
                }
                addChild(streamUnit);
            }
            catch(Exception e) {
                logger.catching(e);
                logger.error("Can not create unit for stream %s", stream.getId());
            }
        }

        if(getStatistics().isUserPasswordRequired()) {
            this.notifyListeners(new JebEvent(J.Notification,
                    new ClientNotification("Pdf requires password from user: streams and strings can not be decoded",
                            ClientNotificationLevel.WARNING)));
        }

        logger.debug("Cross References successfully built.");
        setProcessed(true);
        return true;
    }

    private void addXFAChildren(PdfStream stream, IUnit parentUnit, List<byte[]> xfaScripts, StreamType streamType,
            String wantedType) {
        if(!xfaScripts.isEmpty()) {
            for(byte[] script: xfaScripts) {
                getStatistics().addUnitNotification(stream, SuspiciousType.PotentialHarmfulFile,
                        "XFA contains " + streamType);
                IUnit jsUnit = buildDefaultStreamUnit(parentUnit, stream, streamType, script, wantedType);
                parentUnit.addChild(jsUnit);
            }
        }
    }

    private IUnit buildDefaultStreamUnit(IUnit creator, PdfStream stream, StreamType streamType, byte[] data,
            String wantedType) {
        StringBuilder name = new StringBuilder(stream.getName()).append(" (").append(streamType).append(")");
        IUnit streamUnit = getUnitProcessor().process(name.toString(), new BytesInput(data), creator, wantedType, true);
        if(streamUnit == null) {
            PdfStream virtualStream = new PdfStream(stream.getDictionary(), data);
            virtualStream.setType(streamType);
            return new BinaryStreamUnit(creator, getStatistics(), virtualStream, stream.getName(), true);
        }
        return streamUnit;
    }

    private IUnit buildDefaultStreamUnit(PdfStream stream) {
        IUnit streamUnit = getUnitProcessor().process(stream.getName(), new BytesInput(stream.getDecodedData()), this,
                stream.getWantedType(), true);
        if(streamUnit == null) {
            streamUnit = new BinaryStreamUnit(this, stream, identifier, true);
        }
        else {
            streamUnit = StreamUnitProvider.wrap(streamUnit, stream, null);
        }
        return streamUnit;
    }

    public List<PdfIndirectObj> getObjectList() {
        if(objectList == null) {
            objectList = new ArrayList<PdfIndirectObj>(toMap().values());
            Collections.sort(objectList);

        }
        return objectList;
    }

    public Map<PdfObjId, PdfFile> getObjects() {
        return objects;
    }

    public Map<Integer, PdfTrailer> getTrailers() {
        return trailers;
    }

    private Map<PdfObjId, PdfIndirectObj> toMap() {
        Map<PdfObjId, PdfIndirectObj> objectsMap = new TreeMap<PdfObjId, PdfIndirectObj>();
        for(Entry<PdfObjId, PdfFile> objectEntrySet: objects.entrySet()) {
            objectsMap.put(objectEntrySet.getKey(), objectEntrySet.getValue().getObject(objectEntrySet.getKey()));
        }
        return objectsMap;
    }

    @Override
    public AddressUtils getAddressUtils() {
        if(addressManager == null) {
            addressManager = new AddressUtils(getObjectList());
        }
        return addressManager;
    }

    private CrossReferences getCrossReferences() {
        if(crossReferences == null) {
            crossReferences = new CrossReferences(getObjectList());
        }
        return crossReferences;
    }

    @Override
    public PdfStatistics getStatistics() {
        if(statistics == null) {
            statistics = new PdfStatistics(this);
        }
        return statistics;
    }

    protected List<PdfIndirectObj> getAreaOfInterestObjectList() {
        List<PdfIndirectObj> areaOfInterest = new ArrayList<PdfIndirectObj>(getStatistics().anomalyKeys());
        Collections.sort(areaOfInterest);
        return areaOfInterest;
    }

    @Override
    public IUnitFormatter getFormatter() {
        IUnitFormatter formatter = super.getFormatter();
        if(UnitFormatterUtil.getPresentationByName(formatter, "Indirect Objects") == null) {
            formatter.addPresentation(new AbstractTransientUnitRepresentation("Indirect Objects", true) {
                @Override
                public IGenericDocument getDocument() {
                    TreePdfDocument document = new TreePdfDocument(getObjectList(), PdfFileUnit.this);
                    return document;
                }
            }, false);
        }
        if(UnitFormatterUtil.getPresentationByName(formatter, "Simple view") == null) {
            final byte[] simple = simpleView;
            formatter.addPresentation(new AbstractTransientUnitRepresentation("Simple view", false) {
                @Override
                public IGenericDocument getDocument() {
                    return new AsciiDocument(new BytesInput(simple));
                }
            }, false);
        }
        return formatter;
    }

    @Override
    public List<Integer> getAddressActions(String arg0) {
        logger.info("getAddressActions %s", arg0);
        return null;
    }

    @Override
    public String getAddressOfItem(long itemId) {
        logger.info("getAddressOfItem %d", itemId);
        return null;
    }

    @Override
    public List<Integer> getGlobalActions() {
        logger.info("getGlobalActions");
        return null;
    }

    @Override
    public List<Integer> getItemActions(long id) {
        logger.info("getItemActions %d", id);
        return Arrays.asList(Actions.QUERY_XREFS);
    }

    @Override
    public long getItemAtAddress(String address) {
        return getAddressUtils().getByAddress(address).getRange()[0];
    }

    @Override
    public String getAddressLabel(String address) {
        return getAddressUtils().getByAddress(address).getLabel();
    }

    @Override
    public Map<String, String> getAddressLabels() {
        logger.info("getAddressLabels");
        return null;
    }

    @Override
    public String getComment(String address) {
        return getAddressUtils().getByAddress(address).getComment();
    }

    @Override
    public Map<String, String> getComments() {
        // no comments
        return null;
    }

    @Override
    public IInputLocation addressToLocation(String address) {
        if(address.endsWith("h")) {
            return new FileInputLocation(Long.valueOf(address.substring(0, address.length() - 1), 16));
        }
        else if(address.startsWith("@")) {
            return null;
        }
        IAddress addressImpl = getAddressUtils().getByAddress(address);
        return new FileInputLocation(addressImpl.getRange()[0]);
    }

    @Override
    public String locationToAddress(IInputLocation location) {
        if(location instanceof FileInputLocation) {
            long offset = ((FileInputLocation)location).getOffset();
            return getAddressUtils().getByOffset(offset).getLabel();
        }
        return null;
    }

    @Override
    public boolean canExecuteAction(ActionContext actionContext) {
        if(actionContext.getActionId() == Actions.QUERY_XREFS && actionContext.getItemId() != 0L) {
            return true;
        }
        return false;
    }

    @Override
    public boolean prepareExecution(ActionContext actionContext, IActionData actionData) {
        if(actionContext.getActionId() == Actions.QUERY_XREFS) {
            ActionXrefsData data = (ActionXrefsData)actionData;
            List<String> addresses = getCrossReferences().getCrossReference(actionContext.getItemId());
            data.setAddresses(addresses);
            return true;
        }

        return false;
    }

    @Override
    public boolean executeAction(ActionContext actionContext, IActionData actionData) {
        if(actionContext.getActionId() == Actions.QUERY_XREFS) {
            // ActionXrefsData data = (ActionXrefsData) actionData;
            // no action to perform: jump
        }
        return false;
    }

    @Override
    public String getDescription() {
        PdfStatistics statistics = getStatistics();
        StringBuilder stb = new StringBuilder(super.getDescription());
        stb.append("- Number of indirect objects: ").append(statistics.getNbIndirectObjects());
        stb.append("\n- Number of stream objects: ").append(statistics.getNbStreamedObjects());
        stb.append("\n- Number of streams: ").append(statistics.getNbStreams());
        if(statistics.isEncrypted()) {
            stb.append("\n- Pdf trailer defines Encryption: streams and strings can not be decoded");
        }
        Set<String> filters = statistics.getFiltersUsed();
        if(!filters.isEmpty()) {
            stb.append("\n- Filters used: ").append(Strings.join(", ", filters));
        }
        stb.append("\n").append(statistics.toStringTokens());
        stb.append(getStatistics().toStringCorruptions());
        return stb.toString();
    }

    @Override
    public PdfStatistics getNotificationManager() {
        return getStatistics();
    }
}
