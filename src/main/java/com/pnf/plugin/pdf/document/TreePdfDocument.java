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
import java.util.List;

import com.pnf.plugin.pdf.AddressUtils;
import com.pnf.plugin.pdf.PdfStatistics;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.unit.IPdfUnit;
import com.pnfsoftware.jeb.core.events.JebEventSource;
import com.pnfsoftware.jeb.core.output.tree.INode;
import com.pnfsoftware.jeb.core.output.tree.INodeCoordinates;
import com.pnfsoftware.jeb.core.output.tree.ITreeDocument;
import com.pnfsoftware.jeb.core.properties.IPropertyManager;
import com.pnfsoftware.jeb.util.serialization.annotations.SerDisabled;

/**
 * 
 * 
 * @author PNF Software
 *
 */
@SerDisabled
public class TreePdfDocument extends JebEventSource implements ITreeDocument {
    private List<INode> roots;
    private List<PdfIndirectObj> objects;
    private List<IPdfAttribute> attributes;
    private PdfStatistics statistics;

    public TreePdfDocument(List<PdfIndirectObj> objects, IPdfUnit unit) {
        this.objects = objects;
        this.statistics = unit.getStatistics();
    }

    public TreePdfDocument(PdfDictionary dictionary) {
        attributes = new ArrayList<>();
        attributes.add(dictionary);
        statistics = dictionary.getMainParent().getPdfStatictics();
    }

    @Override
    public List<? extends INode> getRoots() {
        if(roots == null) {
            List<INode> nodes = new ArrayList<>();
            if(objects != null) {
                for(PdfIndirectObj obj: objects) {
                    INode newNode = new PdfObjectNode(obj.getAttribute(), obj.getId(), statistics.getAnomalies(obj),
                            true);
                    nodes.add(newNode);
                }
            }
            if(attributes != null) {
                for(IPdfAttribute attribute: attributes) {
                    PdfIndirectObj obj = attribute.getParent().getMainParent();
                    INode newNode = new PdfObjectNode(attribute, statistics.getAnomalies(obj), true);
                    nodes.add(newNode);
                }
            }
            roots = nodes;
        }
        return roots;
    }

    @Override
    public List<String> getColumnLabels() {
        return PdfNode.COLUMNS;
    }

    @Override
    public int getInitialExpansionLevel() {
        return objects != null ? 0: -1;
    }

    @Override
    public INodeCoordinates addressToCoordinates(String address) {
        return new AddressUtils(objects).addressToCoordinates(address);
    }

    @Override
    public String coordinatesToAddress(INodeCoordinates coordinates) {
        return new AddressUtils(objects).coordinatesToAddress(coordinates);
    }

    @Override
    public void dispose() {

    }

    @Override
    public IPropertyManager getPropertyManager() {
        return null;
    }

}
