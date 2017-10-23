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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.pnf.plugin.pdf.obj.AbstractPdfParsableAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.statistics.PdfUnitNotification;
import com.pnf.plugin.pdf.unit.IPdfUnit;
import com.pnfsoftware.jeb.core.units.IUnitNotification;
import com.pnfsoftware.jeb.core.units.IUnitNotificationManager;
import com.pnfsoftware.jeb.core.units.NotificationType;
import com.pnfsoftware.jeb.util.format.Strings;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;

/**
 * Data used for statistics of a PDF File
 * 
 * @author PNF Software
 *
 */
@Ser
public class PdfStatistics implements IUnitNotificationManager {

    private static final ILogger logger = GlobalLog.getLogger(PdfStatistics.class);

    public static boolean BATCH_MODE = false;

    public enum SuspiciousType {
        Malformed,
        StreamWithMultipleFilters,
        PotentialHarmfulToken,
        StreamUnfiltered,
        MalformedStream,
        PotentialHarmfulFile
    };

    @SerId(1)
    private int nbIndirectObjects = 0;

    @SerId(2)
    private int nbStreamedObjects = 0;

    @SerId(3)
    private int nbStreams = 0;

    @SerId(4)
    private Map<PdfIndirectObj, Map<IPdfAttribute, List<IUnitNotification>>> anomalies = new TreeMap<>();

    @SerId(5)
    private Set<String> filtersUsed = new TreeSet<>();

    @SerId(6)
    private Map<String, Integer> tokens = new HashMap<>();

    @SerId(7)
    private boolean isEncrypted = false;

    @SerId(8)
    private boolean userPasswordRequired = false;

    @SerId(9)
    private IPdfUnit unit;

    @SerId(10)
    private String version = "";

    protected PdfStatistics(IPdfUnit unit) {
        this.unit = unit;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Return Version of a PDF (be careful that version is optional in PDF documents)
     * 
     * @return version (1.3, 1.4 for example)
     */
    public String getVersion() {
        return version;
    }

    public int getNbIndirectObjects() {
        return nbIndirectObjects;
    }

    public void setNbIndirectObjects(int nbIndirectObjects) {
        this.nbIndirectObjects = nbIndirectObjects;
    }

    public int getNbStreamedObjects() {
        return nbStreamedObjects;
    }

    public void setNbStreamedObjects(int nbStreamedObjects) {
        this.nbStreamedObjects = nbStreamedObjects;
    }

    public int getNbStreams() {
        return nbStreams;
    }

    public void setNbStreams(int nbStreams) {
        this.nbStreams = nbStreams;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public boolean isUserPasswordRequired() {
        return userPasswordRequired;
    }

    public void setEncrypted(boolean isEncrypted) {
        logger.info("Pdf trailer defines Encryption: Trying to decode");
        setEncrypted(isEncrypted, false);
    }

    public void setEncrypted(boolean isEncrypted, boolean requiresUserPassword) {
        this.isEncrypted |= isEncrypted;
        this.userPasswordRequired |= requiresUserPassword;
    }

    public Set<String> getFiltersUsed() {
        return filtersUsed;
    }

    public void addFiltersUsed(String filter) {
        this.filtersUsed.add(filter);
    }

    public Map<IPdfAttribute, List<IUnitNotification>> getAnomalies(PdfIndirectObj obj) {
        return anomalies.get(obj);
    }

    public Set<PdfIndirectObj> anomalyKeys() {
        return anomalies.keySet();
    }

    public void addUnitNotification(IPdfAttribute element, SuspiciousType suspicious) {
        addUnitNotification(element, suspicious, null, null);
    }

    public void addUnitNotification(IPdfAttribute element, SuspiciousType suspicious, String description) {
        addUnitNotification(element, suspicious, description, null);
    }

    public void addUnitNotification(IPdfAttribute element, SuspiciousType suspicious, String description, Throwable e) {
        addUnitNotification(element, suspicious, description, e, false);
    }

    public void addUnitNotification(IPdfAttribute element, SuspiciousType suspicious, String description, Throwable e,
            boolean dropSameLevel) {
        if(element.getParent() == null) {
            addUnitNotification(element, (PdfIndirectObj)element, suspicious, description, dropSameLevel);
        }
        else {
            addUnitNotification(element, element.getParent().getMainParent(), suspicious, description, dropSameLevel);
        }

    }

    private void addUnitNotification(IPdfAttribute element, PdfIndirectObj parent, SuspiciousType suspicious,
            String description, boolean dropSameLevel) {
        Map<IPdfAttribute, List<IUnitNotification>> anomaliesByChild = anomalies.get(parent);
        if(anomaliesByChild == null) {
            anomaliesByChild = new HashMap<>();
            anomalies.put(parent, anomaliesByChild);
        }
        List<IUnitNotification> anomaliesByElement = anomaliesByChild.get(element);
        if(anomaliesByElement == null) {
            anomaliesByElement = new ArrayList<>();
            anomaliesByChild.put(element, anomaliesByElement);
        }
        NotificationType notificationType = getNotificationType(suspicious);
        AbstractPdfParsableAttribute parentElement = element.getParent();
        List<IUnitNotification> duplicates = new ArrayList<>();
        for(Entry<IPdfAttribute, List<IUnitNotification>> entry: anomaliesByChild.entrySet()) {
            for(IUnitNotification notification: entry.getValue()) {
                if(notification.getType() == notificationType && notification.getDescription().equals(description)) {
                    IPdfAttribute notificationElement = ((PdfUnitNotification)notification).getElement();
                    if(notificationElement == element || notificationElement == parentElement
                            || notificationElement.getParent() == element
                            || notificationElement.getParent() == parentElement) {
                        duplicates.add(notification);
                    }
                }
            }
            if(dropSameLevel) {
                // remove same level notification if exists
                entry.getValue().removeAll(duplicates);
            }
            else if(!duplicates.isEmpty()) {
                // do not add if same level notification exists
                return;
            }
        }
        anomaliesByElement.add(getUnitNotification(element, suspicious, description));
    }

    private IUnitNotification getUnitNotification(IPdfAttribute element, SuspiciousType suspicious,
            String description) {
        if(suspicious == SuspiciousType.PotentialHarmfulToken) {
            addToken(element);
        }
        return new PdfUnitNotification(getNotificationType(suspicious),
                getDescription(element, suspicious, description), element, unit);
    }

    private NotificationType getNotificationType(SuspiciousType suspicious) {
        switch(suspicious) {
        case Malformed:
        case MalformedStream:
            return NotificationType.CORRUPTION;
        case StreamUnfiltered:
            return NotificationType.UNSUPPORTED_FEATURE;
        case StreamWithMultipleFilters:
            return NotificationType.AREA_OF_INTEREST;
        case PotentialHarmfulToken:
        case PotentialHarmfulFile:
            return NotificationType.POTENTIALLY_HARMFUL;
        default:
            return NotificationType.AREA_OF_INTEREST;
        }
    }

    private void addToken(IPdfAttribute element) {
        Integer t = tokens.get(element.toString());
        if(t == null) {
            t = Integer.valueOf(0);
        }
        tokens.put(element.toString(), ++t);
    }

    private String getDescription(IPdfAttribute element, SuspiciousType suspicious, String description) {
        if(description != null && !(suspicious == SuspiciousType.StreamUnfiltered && description.equals("Encrypted"))) {
            // no need to display in batch mode
            // multiple filter is not a problem
            if(!BATCH_MODE || (suspicious != SuspiciousType.PotentialHarmfulToken
                    && suspicious != SuspiciousType.StreamWithMultipleFilters
            /*&& !(suspicious == SuspiciousType.MalformedStream && description.contains("Length defined"))*/)) {
                logger.error("%s: %s in %s", suspicious, description, element.getId());
            }
            return description;
        }
        else {
            return suspicious.toString();
        }
    }

    public String toStringTokens() {
        StringBuilder stb = new StringBuilder();
        for(Entry<String, Integer> t: tokens.entrySet()) {
            stb.append("\nToken '").append(t.getKey()).append("' found ").append(t.getValue()).append(" time");
            if(t.getValue() > 1) {
                stb.append('s');
            }
        }
        return stb.toString();
    }

    public String toStringCorruptions() {
        StringBuilder stb = new StringBuilder();
        for(Entry<PdfIndirectObj, Map<IPdfAttribute, List<IUnitNotification>>> entry: anomalies.entrySet()) {
            for(Entry<IPdfAttribute, List<IUnitNotification>> entryAttr: entry.getValue().entrySet()) {
                String corruptions = toString(entryAttr.getValue(), NotificationType.CORRUPTION);
                if(!corruptions.isEmpty()) {
                    stb.append("\n--- in ").append(entry.getKey().getId()).append(":").append(corruptions);
                }
            }
        }
        if(stb.length() > 0) {
            stb.insert(0, "\n\nPdf File has corruptions:");
        }
        return stb.toString();
    }

    public String toStringAnomalies() {
        StringBuilder stb = new StringBuilder();
        for(Entry<PdfIndirectObj, Map<IPdfAttribute, List<IUnitNotification>>> entry: anomalies.entrySet()) {
            stb.append("\n- Notifications in ").append(entry.getKey().getId());
            for(Entry<IPdfAttribute, List<IUnitNotification>> entryAttr: entry.getValue().entrySet()) {
                stb.append("\n--- in ").append(entryAttr.getKey().toString()).append(":")
                        .append(toString(entryAttr.getValue(), null));
            }
            stb.append("\n");
        }
        return stb.toString();
    }

    public static String toString(List<IUnitNotification> notifications) {
        return toString(notifications, null);
    }

    public static String toString(List<IUnitNotification> notifications, NotificationType filter) {
        Set<String> notifStr = new TreeSet<>();
        for(IUnitNotification n: notifications) {
            if(filter != null) {
                if(n.getType() == filter) {
                    notifStr.add(n.getDescription());
                }
            }
            else {
                notifStr.add(n.getDescription());
            }
        }
        return Strings.join(", ", notifStr);
    }

    @Override
    public List<IUnitNotification> getNotifications() {
        Collection<Map<IPdfAttribute, List<IUnitNotification>>> anomaliesPerAttr = anomalies.values();
        List<IUnitNotification> notifications = new ArrayList<>();
        for(Map<IPdfAttribute, List<IUnitNotification>> ano: anomaliesPerAttr) {
            Collection<List<IUnitNotification>> anomaliesList = ano.values();
            for(List<IUnitNotification> a: anomaliesList) {
                notifications.addAll(a);
            }
        }
        return notifications;
    }

    @Override
    public int getNotificationCount() {
        return getNotifications().size();
    }

    @Override
    public void addNotification(IUnitNotification notification) {
        throw new UnsupportedOperationException("A notification must be bound to a PDF Indirect Object");
    }

    @Override
    public void insertNotification(int index, IUnitNotification notification) {
        throw new UnsupportedOperationException("A notification must be bound to a PDF Indirect Object");
    }

    @Override
    public void addAllNotifications(Collection<? extends IUnitNotification> c) {
        throw new UnsupportedOperationException("A notification must be bound to a PDF Indirect Object");
    }

    @Override
    public void removeNotification(int index) {
        throw new UnsupportedOperationException("A notification must be bound to a PDF Indirect Object");
    }

}
