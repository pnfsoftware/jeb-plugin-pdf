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
import java.util.List;

import com.pnf.plugin.pdf.address.AddressImpl;
import com.pnf.plugin.pdf.address.IAddress;
import com.pnf.plugin.pdf.address.IAddressProvider;
import com.pnf.plugin.pdf.address.INodeCoordinatesProvider;
import com.pnf.plugin.pdf.obj.AbstractPdfParsableAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.pnfsoftware.jeb.core.output.tree.INodeCoordinates;
import com.pnfsoftware.jeb.core.output.tree.impl.NodeCoordinates;
import com.pnfsoftware.jeb.util.format.Strings;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * Define several Addressing system and allow to switch from them:<br>
 * - coordinates as Tree coordinates = list of integer (indexes in tree)<br>
 * - address: coordinates concatenated as string, separated by "/"<br>
 * - attribute: indicates an element in the tree<br>
 * - path: list of all attributes that bring to an attribute.<br>
 * <br>
 * Possible conversions<br>
 * attribute -> path<br>
 * path -> coordinates / attribute (latest element)<br>
 * coordinates -> path / address (easy) / attribute<br>
 * address (easy) &lt;-> coordinates (easy)
 * 
 * @author PNF Software
 * 
 */
public class AddressUtils implements IAddressProvider, INodeCoordinatesProvider {

    private static final ILogger logger = GlobalLog.getLogger(AddressUtils.class);

    private static final List<IPdfAttribute> EMPTY_PATH = new ArrayList<IPdfAttribute>();

    /**
     * Convert List of integer (position in tree) to Address (integers separated by /)
     */
    private static String coordinatesToAddress(List<Integer> coordinates) {
        return Strings.join("/", coordinates);
    }

    /**
     * Convert Address (integers separated by /) in List of integer (position in tree)
     */
    private static List<Integer> addressToCoords(String address) {
        String[] coordinatesStr = address.split("/");
        List<Integer> coordinates = new ArrayList<Integer>();
        for(String coor: coordinatesStr) {
            if(coor != null && coor.length() > 0) {
                coordinates.add(Integer.valueOf(coor));
            }
        }
        return coordinates;
    }

    private static List<IPdfAttribute> attributeToPath(IPdfAttribute element) {
        if(element == null) {
            return null;
        }
        IPdfAttribute currentNode = element;
        List<IPdfAttribute> path = new ArrayList<IPdfAttribute>();
        while(currentNode != null) {
            path.add(0, currentNode);
            currentNode = currentNode.getParent();
        }
        if(path.size() > 1) {
            // remove duplicated parent IndirectObj + its attribute represented as the same element in tree
            path.remove(1);
        }
        return path;
    }

    /**
     * Retrieve the position of an element
     */
    protected List<Integer> pathToCoordinates(List<IPdfAttribute> path) {
        logger.trace("path to coordinates: %s", toString(path, true));
        List<Integer> coordinates = new ArrayList<Integer>();
        IPdfAttribute attribute = path.remove(0);
        coordinates.add(objects.indexOf(attribute));
        if(!path.isEmpty()) {
            coordinates.addAll(pathToCoordinates(path, ((PdfIndirectObj)attribute).getAttribute()));
        }
        logger.trace("path to coordinates result: %s", Strings.joinList(coordinates));
        return coordinates;
    }

    private static List<Integer> pathToCoordinates(List<IPdfAttribute> path, IPdfAttribute attribute) {
        if(path.isEmpty()) {
            return new ArrayList<Integer>();
        }
        switch(attribute.getType()) {
        case Array:
            return pathToCoordinates(path, (PdfArray)attribute);
        case Dictionary:
            return pathToCoordinates(path, (PdfDictionary)attribute);
        case Stream:
            return pathToCoordinates(path, ((PdfStream)attribute).getDictionary());
        default:
            break;
        }
        return new ArrayList<Integer>();
    }

    private static List<Integer> pathToCoordinates(List<IPdfAttribute> path, PdfArray attribute) {
        List<Integer> coordinates = new ArrayList<Integer>();
        IPdfAttribute child = path.remove(0);
        coordinates.add(attribute.getAttributes().indexOf(child));
        coordinates.addAll(pathToCoordinates(path, child));
        return coordinates;
    }

    private static List<Integer> pathToCoordinates(List<IPdfAttribute> path, PdfDictionary attribute) {
        List<Integer> coordinates = new ArrayList<Integer>();
        List<PdfDictionaryAttribute> attributes = attribute.getAttributes();
        IPdfAttribute child = path.remove(0);
        for(int i = 0; i < attributes.size(); i++) {
            // check reference
            if(attributes.get(i).equals(child) || attributes.get(i).getKey().equals(child)
                    || attributes.get(i).getValue().equals(child)) {
                coordinates.add(i);
                coordinates.addAll(pathToCoordinates(path, child));
                return coordinates;
            }
        }
        logger.error("Can not retrieve attribute %s in dictionary %s", toString(child, false),
                toString(attributes, false));
        return coordinates;
    }

    protected List<IPdfAttribute> coordinateToPath(List<Integer> coordinates) {
        List<IPdfAttribute> path = new ArrayList<IPdfAttribute>();
        List<Integer> c = cloneList(coordinates);
        PdfIndirectObj o = objects.get(c.remove(0));
        path.add(o);

        path.addAll(coordinateToPath(c, o.getAttribute()));
        return path;
    }

    private static List<IPdfAttribute> coordinateToPath(List<Integer> coordinates, IPdfAttribute attribute) {
        if(coordinates.isEmpty()) {
            return EMPTY_PATH;
        }
        switch(attribute.getType()) {
        case Dictionary:
            // not displayed in address
            return coordinateToPath(coordinates, (PdfDictionary)attribute);
        case Stream:
            // not displayed in address
            return coordinateToPath(coordinates, ((PdfStream)attribute).getDictionary());
        case Array:
            return coordinateToPath(coordinates, (PdfArray)attribute);
        default:
            // no addressing system
            logger.debug("The element of type %s can not have a child: %s", attribute.getType(),
                    toString(attribute, false));
            return EMPTY_PATH;
        }
    }

    private static List<IPdfAttribute> coordinateToPath(List<Integer> c, PdfArray attribute) {
        List<IPdfAttribute> path = new ArrayList<IPdfAttribute>();
        int index = c.remove(0);
        IPdfAttribute child = attribute.getAttributes().get(index);
        path.add(child);
        path.addAll(coordinateToPath(c, child));
        return path;
    }

    private static List<IPdfAttribute> coordinateToPath(List<Integer> c, PdfDictionary attribute) {
        List<IPdfAttribute> path = new ArrayList<IPdfAttribute>();
        int index = c.remove(0);
        PdfDictionaryAttribute child = attribute.getAttributes().get(index);
        path.add(child);
        path.addAll(coordinateToPath(c, child.getValue()));
        return path;

    }

    private static <E> List<E> cloneList(List<E> list) {
        List<E> clone = new ArrayList<E>();
        for(E elementsFromList: list) {
            clone.add(elementsFromList);
        }
        return clone;
    }

    protected static String toString(List<? extends IPdfAttribute> attributes, boolean recursive) {
        if(attributes == null) {
            return "";
        }
        List<String> strList = new ArrayList<String>();
        for(IPdfAttribute attribute: attributes) {
            strList.add(toString(attribute, recursive));
        }
        return Strings.joinList(strList);
    }

    private static String toString(IPdfAttribute attribute, boolean recursive) {
        switch(attribute.getType()) {
        case Array:
            return ("Array<" + (recursive ? toString(((PdfArray)attribute).getAttributes(), false): "") + ">");
        case Boolean:
        case Name:
        case Null:
        case Number:
        case String:
        case Unknown:
        case IndirectReference:
            String str = attribute.toString();
            if(str.length() > 20) {
                str = str.substring(0, 17) + "...";
            }
            return str;
        case Stream:
            return ("Stream<>");
        case Dictionary:
            return ("Dictionary<" + (recursive ? toString(((PdfDictionary)attribute).getAttributes(), false): "")
                    + ">");
        case DictionaryAttribute:
            return ("DictionaryKey" + ((PdfDictionaryAttribute)attribute).getKey().toString());
        default:
            break;
        }
        return "";
    }

    public String attributeToAddress(IPdfAttribute element) {
        return coordinatesToAddress(pathToCoordinates(attributeToPath(element)));
    }

    List<PdfIndirectObj> objects;

    public AddressUtils(List<PdfIndirectObj> objects) {
        this.objects = objects;
    }

    @Override
    public IAddress getByOffset(long offset) {
        IPdfAttribute attribute = offsetToAttribute(offset);
        return getAddress(attribute);
    }

    /**
     * Return the {@link IPdfAttribute} that is selected at offset parameter. This method does not
     * recurse: it simply returns the high level IndirectObj
     */
    private PdfIndirectObj offsetToAttribute(long offset) {
        for(PdfIndirectObj obj: objects) {
            if(offset >= obj.getStartIndex() && offset <= obj.getEndIndex()) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public IAddress getByAddress(String address) {
        IPdfAttribute attribute = addressToAttribute(address);
        return getAddress(attribute, addressToCoords(address));
    }

    protected IPdfAttribute addressToAttribute(String address) {
        List<Integer> coordinates = addressToCoords(address);
        List<IPdfAttribute> attributes = coordinateToPath(coordinates);
        return attributes.get(attributes.size() - 1);
    }

    private IAddress getAddress(IPdfAttribute attribute) {
        List<Integer> coordinates = pathToCoordinates(attributeToPath(attribute));
        if(attribute == null) {
            return AddressImpl.nullObject();
        }
        return getAddress(attribute, coordinates);
    }

    private IAddress getAddress(IPdfAttribute attribute, List<Integer> coordinates) {
        return new AddressImpl(attributeToRange(attribute), coordinatesToAddress(coordinates),
                coordinateToLabelAddress(coordinates), attributeToComment(attribute));
    }

    private static long[] attributeToRange(IPdfAttribute attribute) {
        long startIndex = 0L;
        long endIndex = 0L;
        if(attribute instanceof AbstractPdfParsableAttribute) {
            startIndex = ((AbstractPdfParsableAttribute)attribute).getStartIndex();
            endIndex = ((AbstractPdfParsableAttribute)attribute).getEndIndex();
        }
        else {
            startIndex = attribute.getParent().getStartIndex();
            endIndex = attribute.getParent().getEndIndex();
        }
        return new long[]{startIndex, endIndex - startIndex};
    }

    /**
     * Display address label
     */
    protected String coordinateToLabelAddress(List<Integer> coordinates) {
        List<IPdfAttribute> path = coordinateToPath(coordinates);
        StringBuilder address = new StringBuilder();
        address.append(((PdfIndirectObj)path.get(0)).getId().toString());
        for(int i = 1; i < path.size(); i++) {
            IPdfAttribute attribute = path.get(i);
            switch(attribute.getType()) {
            case IndirectObject:
            case IndirectObjectStream:
                logger.error("Indirect objects are not displayed");
                break;
            case DictionaryAttribute:
                address.append(String.format("/<<%s>>", ((PdfDictionaryAttribute)attribute).getKey().toString()));
                break;
            default:
                // if not dictionary attribute, this is an array element
                address.append(String.format("/[%d]", coordinates.get(i)));
                break;
            }
        }
        return address.toString();
    }

    private String attributeToComment(IPdfAttribute attribute) {
        AbstractPdfParsableAttribute parent = attribute.getParent();
        if(parent == null) {
            return PdfFormatter.displayValue(attribute, null);
        }
        return PdfFormatter.displayValue(parent.getMainParent(), null);
    }

    @Override
    public INodeCoordinates addressToCoordinates(String address) {
        return new NodeCoordinates(addressToCoords(address));
    }

    @Override
    public String coordinatesToAddress(INodeCoordinates coordinates) {
        return coordinatesToAddress(coordinates.getPath());
    }
}
