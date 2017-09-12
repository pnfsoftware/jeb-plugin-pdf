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

package com.pnf.plugin.pdf.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.pnf.plugin.pdf.PdfFormatter;
import com.pnf.plugin.pdf.obj.AbstractPdfParsableAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnfsoftware.jeb.core.output.ItemClassIdentifiers;
import com.pnfsoftware.jeb.core.output.tree.INode;
import com.pnfsoftware.jeb.core.output.tree.IVisualNode;
import com.pnfsoftware.jeb.core.units.IUnitNotification;
import com.pnfsoftware.jeb.core.units.NotificationType;

/**
 * Simple node that represent a PDF Node without interaction.
 * 
 * @author PNF Software
 * 
 */
public class PdfNode implements IVisualNode {
    public static final List<String> COLUMNS = Arrays.asList("Node/Type", "Key/Id", "Value");

    private String label;
    private ItemClassIdentifiers classId;
    protected String[] additionalLabels = new String[COLUMNS.size() - 1];
    private List<INode> children = new ArrayList<>();

    public PdfNode(String label, ItemClassIdentifiers classId) {
        this.label = label;
        this.classId = classId;
    }

    public PdfNode(IPdfAttribute attribute, Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        this(getAttributeLabel(attribute), getClassId(attribute, anomalies));
        processElement(attribute, anomalies);
    }

    public PdfNode(PdfDictionaryAttribute dictionaryAttribute, Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        this(getAttributeLabel(dictionaryAttribute.getValue()), getClassId(dictionaryAttribute, anomalies));
        processElement(dictionaryAttribute.getValue(), anomalies);
        this.additionalLabels[0] = dictionaryAttribute.getKey().toString();
    }

    private PdfNode buildNode(IPdfAttribute attribute, Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        if(attribute.getType() == Type.IndirectReference) {
            return new PdfObjectNode(attribute, anomalies, false);
        }
        return new PdfNode(attribute, anomalies);
    }

    private PdfNode buildNode(PdfDictionaryAttribute dictionaryAttribute,
            Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        if(dictionaryAttribute.getValue().getType() == Type.IndirectReference) {
            return new PdfObjectNode(dictionaryAttribute, anomalies);
        }
        return new PdfNode(dictionaryAttribute, anomalies);
    }

    private static String getAttributeLabel(IPdfAttribute attribute) {
        return attribute.getType().toString();
    }

    private void processElement(IPdfAttribute attribute, Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        List<IUnitNotification> notifications = getChildNotifications(anomalies, attribute);
        switch(attribute.getType()) {
        case Array:
            addArrayChildren(((PdfArray)attribute).getAttributes(), anomalies);
            additionalLabels[1] = "";
            break;
        case Boolean:
        case Name:
        case Null:
        case Number:
        case String:
        case Unknown:
        case IndirectReference:
            additionalLabels[1] = PdfFormatter.displayValue(attribute, notifications);
            break;
        case Stream:
            // additionalLabels[0] = ((PdfStream) attribute).toString();
            additionalLabels[1] = PdfFormatter.displayValue(attribute, notifications);
            addDictionaryChildren(((PdfStream)attribute).getDictionary().getAttributes(), anomalies);
            break;
        case Dictionary:
            additionalLabels[1] = PdfFormatter.displayValue(attribute, notifications);
            addDictionaryChildren(((PdfDictionary)attribute).getAttributes(), anomalies);
            break;
        default:
            additionalLabels[1] = "";
            break;
        }
    }

    private void addArrayChildren(List<IPdfAttribute> attributes,
            Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        int i = 0;
        for(IPdfAttribute attribute: attributes) {
            PdfNode childNode = buildNode(attribute, anomalies);
            this.addChild(childNode);
            if(childNode.additionalLabels[0] == null) {
                childNode.additionalLabels[0] = String.format("[%d]", i);
            }
            else {
                childNode.additionalLabels[0] = String.format("[%d] ", i) + childNode.additionalLabels[0];
            }
            i++;
        }
    }

    private void addDictionaryChildren(List<PdfDictionaryAttribute> attributes,
            Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        for(PdfDictionaryAttribute attribute: attributes) {
            PdfNode childNode = buildNode(attribute, anomalies);
            this.addChild(childNode);
        }
    }

    private static List<IUnitNotification> getChildNotifications(Map<IPdfAttribute, List<IUnitNotification>> anomalies,
            IPdfAttribute attribute) {
        if(anomalies == null) {
            return null;
        }
        List<IUnitNotification> notifications = new ArrayList<IUnitNotification>();
        for(Entry<IPdfAttribute, List<IUnitNotification>> n: anomalies.entrySet()) {
            if(n.getKey().equals(attribute)) {
                notifications.addAll(n.getValue());
            }
            else {
                AbstractPdfParsableAttribute parent = n.getKey().getParent();
                while(parent != null) {
                    if(parent.equals(attribute)) {
                        // the anomaly is on a child
                        notifications.addAll(n.getValue());
                        break;
                    }
                    parent = parent.getParent();
                }

            }
        }
        return notifications;
    }

    private static ItemClassIdentifiers getClassId(IPdfAttribute attribute,
            Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        List<IUnitNotification> notifications = getChildNotifications(anomalies, attribute);
        return getClassId(attribute, notifications);
    }

    private static NotificationType getNotificationType(int maxLevel) {
        for(NotificationType n: NotificationType.values()) {
            if(n.getLevel() == maxLevel) {
                return n;
            }
        }
        return null;
    }

    private static ItemClassIdentifiers getClassId(IPdfAttribute attribute, List<IUnitNotification> notifications) {
        if(notifications != null && !notifications.isEmpty()) {
            int maxLevel = 0;
            for(IUnitNotification n: notifications) {
                maxLevel = Math.max(maxLevel, n.getType().getLevel());
            }
            switch(getNotificationType(maxLevel)) {
            case UNSUPPORTED_FEATURE:
                return ItemClassIdentifiers.INFO_WARNING;
            case DEPRECATED_FEATURE:
                return ItemClassIdentifiers.INFO_DEPRECATED;
            case CORRUPTION:
                return ItemClassIdentifiers.INFO_CORRUPT;
            case AREA_OF_INTEREST:
                return ItemClassIdentifiers.INFO_WARNING;
            case POTENTIALLY_HARMFUL:
                return ItemClassIdentifiers.INFO_DANGEROUS;
            default:
                return ItemClassIdentifiers.INFO_MALFORMED;
            }
        }
        if(attribute.getType() == Type.IndirectReference) {
            return ItemClassIdentifiers.ADDRESS;
        }
        return null;
    }

    private static ItemClassIdentifiers getClassId(PdfDictionaryAttribute dictionaryAttribute,
            Map<IPdfAttribute, List<IUnitNotification>> anomalies) {
        List<IUnitNotification> notifications = new ArrayList<>();
        List<IUnitNotification> valueNotifications = getChildNotifications(anomalies, dictionaryAttribute.getValue());
        List<IUnitNotification> keyNotifications = getChildNotifications(anomalies, dictionaryAttribute.getKey());
        if(valueNotifications != null) {
            notifications.addAll(valueNotifications);
        }
        if(keyNotifications != null) {
            notifications.addAll(keyNotifications);
        }
        return getClassId(dictionaryAttribute.getValue(), notifications);
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public ItemClassIdentifiers getClassId() {
        return classId;
    }

    @Override
    public String[] getAdditionalLabels() {
        return additionalLabels;
    }

    private void addChild(PdfNode childNode) {
        children.add(childNode);
    }

    @Override
    public List<INode> getChildren() {
        return children;
    }

    @Override
    public int getInitialExpansion() {
        return 0;
    }
}
