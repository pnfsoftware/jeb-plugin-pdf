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

import java.util.List;

import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnf.plugin.pdf.obj.PdfTrailer;
import com.pnfsoftware.jeb.core.units.IUnitNotification;
import com.pnfsoftware.jeb.util.format.Strings;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfFormatter {

    public static String displayValue(IPdfAttribute attribute, List<IUnitNotification> notifications) {
        if(attribute == null) {
            return "";
        }
        switch(attribute.getType()) {
        case Array:
            return toStringWithSuspiciousType("", attribute, notifications);
        case IndirectReference:
            return toStringWithSuspiciousType(((PdfIndirectReference)attribute).getId().toString(), attribute,
                    notifications);
        case Stream:
            return toStringWithSuspiciousType(displayStream((PdfStream)attribute), attribute, notifications);
        case Dictionary:
            String dictionaryType = ((PdfDictionary)attribute).getDictionaryFullType();
            if(attribute.getParent() != null && attribute.getParent().getType() == Type.Trailer) {
                dictionaryType = "@" + ((PdfTrailer)attribute.getParent()).getStartIndex();
            }
            return toStringWithSuspiciousType(dictionaryType == null ? "": dictionaryType, attribute, notifications);
        case IndirectObject:
        case IndirectObjectStream:
            IPdfAttribute subattribute = ((PdfIndirectObj)attribute).getAttribute();
            if(subattribute.getType() == Type.Stream) {
                return toStringWithSuspiciousType(displayStream((PdfStream)subattribute), attribute, notifications);
            }
            else {
                return toStringWithSuspiciousType("", attribute, notifications);
            }
        case Boolean:
        case Name:
        case Null:
        case Number:
        case String:
        case Unknown:
        default:
            return toStringWithSuspiciousType(attribute.toString(), attribute, notifications);
        }
    }

    private static String displayStream(PdfStream attribute) {
        if(attribute.isObjStm()) {
            return String.format("%s (%s)", attribute.getStreamType(), Strings.join(";", attribute.getObjStmId()));
        }
        return attribute.getStreamType();
    }

    private static String toStringWithSuspiciousType(String prefix, IPdfAttribute attribute,
            List<IUnitNotification> notifications) {
        return String.format("%s %s", prefix, toString(notifications, attribute));
    }

    private static String toString(List<IUnitNotification> notifications, IPdfAttribute attribute) {
        if(notifications == null || notifications.isEmpty()) {
            return "";
        }
        return String.format("[%s]", PdfStatistics.toString(notifications));
    }

}
