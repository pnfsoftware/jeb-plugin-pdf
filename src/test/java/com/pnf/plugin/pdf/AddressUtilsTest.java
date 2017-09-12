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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.pnf.plugin.pdf.address.IAddress;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfName;
import com.pnf.plugin.pdf.obj.PdfNumber;
import com.pnf.plugin.pdf.obj.PdfObjBuilder;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnf.plugin.pdf.parser.PdfFile;

/**
 * Test complex algorithm only<br>
 * coordinates -> path<br>
 * attribute -> path -> coordinates<br>
 * 
 * @author PNF Software
 *
 */
public class AddressUtilsTest extends TestCase {

    private List<PdfIndirectObj> objects = new ArrayList<>();

    private Map<PdfObjId, PdfFile> mapObjects = new HashMap<>();

    private AddressUtils instance = new AddressUtils(objects);

    @Override
    @Before
    public void setUp() {
        PdfFileUnit unit = Mockito.mock(PdfFileUnit.class);
        Mockito.when(unit.getObjects()).thenReturn(mapObjects);
        Mockito.when(unit.getStatistics()).thenReturn(new PdfStatistics(unit));
        PdfFile file = new PdfFile(unit);
        // number
        PdfIndirectObj obj = new PdfIndirectObj(file, unit, 0);
        PdfObjId id = new PdfObjId(1, 0, 0);
        PdfObjBuilder.initIndirectObj(obj, id, new PdfNumber("3", obj, 0));
        mapObjects.put(id, file);
        objects.add(obj);

        buildDictionary(unit, file);

        // array
        obj = new PdfIndirectObj(file, unit, 0);
        id = new PdfObjId(2, 0, 0);
        PdfDictionary dict = new PdfDictionary(obj, 0);
        PdfObjBuilder.initIndirectObj(obj, id, dict);
        mapObjects.put(id, file);
        objects.add(obj);
    }

    private IPdfAttribute attLength;
    private PdfDictionary attData;
    private PdfDictionaryAttribute attDictMin;
    private PdfName attKeyMin;
    private IPdfAttribute attMin;
    private IPdfAttribute att282;
    private PdfArray attArray;

    private void buildDictionary(PdfFileUnit unit, PdfFile file) {
        // dict
        // |-- /Length 7
        // |-- /Data
        //       |-- /Min 9
        // |-- /DataArray
        //       |-- 282
        //       |-- 324
        PdfIndirectObj obj = new PdfIndirectObj(file, unit, 0);
        PdfObjId id = new PdfObjId(2, 0, 0);
        PdfDictionary dict = new PdfDictionary(obj, 0);
        PdfObjBuilder.initIndirectObj(obj, id, dict);

        attLength = new PdfNumber("7", dict, 0);
        dict.getAttributes().add(new PdfDictionaryAttribute(dict, new PdfName("Length", dict, 0), attLength, 0));

        attData = new PdfDictionary(dict, 0);
        attKeyMin = new PdfName("Min", attData, 0);
        attMin = new PdfNumber("9", attData, 0);
        attDictMin = new PdfDictionaryAttribute(attData, attKeyMin, attMin, 0);
        attData.getAttributes().add(attDictMin);
        dict.getAttributes().add(new PdfDictionaryAttribute(dict, new PdfName("Data", dict, 0), attData, 0));

        attArray = new PdfArray(dict, 0);
        att282 = new PdfNumber("282", attArray, 0);
        attArray.getAttributes().add(att282);
        attArray.getAttributes().add(new PdfNumber("324", attArray, 0));
        dict.getAttributes().add(new PdfDictionaryAttribute(dict, new PdfName("DataArray", dict, 0), attArray, 0));

        mapObjects.put(id, file);
        objects.add(obj);
    }

    @Test
    public void testCoordToPathRoot() {
        List<IPdfAttribute> attrs = null;
        attrs = instance.coordinateToPath(Arrays.asList(0));
        assertEquals(1, attrs.size());
        assertEquals(Type.IndirectObject, attrs.get(0).getType());
    }

    @Test
    public void testCoordToPathDictionaryElement() {
        List<IPdfAttribute> attrs = instance.coordinateToPath(Arrays.asList(1, 0));
        assertEquals(2, attrs.size());
        assertEquals(Type.DictionaryAttribute, attrs.get(1).getType());
        IPdfAttribute att = ((PdfDictionaryAttribute)attrs.get(1)).getValue();
        assertEquals("7", att.toString());
    }

    @Test
    public void testCoordToPathDictionaryInDictionary() {
        List<IPdfAttribute> attrs = instance.coordinateToPath(Arrays.asList(1, 1, 0));
        assertEquals(3, attrs.size());
        assertEquals(Type.DictionaryAttribute, attrs.get(2).getType());
        IPdfAttribute att = ((PdfDictionaryAttribute)attrs.get(2)).getValue();
        assertEquals("9", att.toString());
    }

    @Test
    public void testCoordToPathArrayInDictionary() {
        List<IPdfAttribute> attrs = instance.coordinateToPath(Arrays.asList(1, 2, 0));
        assertEquals("Attributes are: " + AddressUtils.toString(attrs, false), 3, attrs.size());
        assertEquals(Type.Number, attrs.get(2).getType());
        IPdfAttribute att = attrs.get(2);
        assertEquals("282", att.toString());
    }

    @Test
    public void testGetByAddressDict() {
        // coordinateToPath + coordinateToLabelAddress
        IAddress addr = null;
        addr = instance.getByAddress("1/0");
        assertEquals("2 0/<<Length>>", addr.getLabel());

        addr = instance.getByAddress("1/1/0");
        assertEquals("2 0/<<Data>>/<<Min>>", addr.getLabel());

        addr = instance.getByAddress("1/2/0");
        assertEquals("2 0/<<DataArray>>/[0]", addr.getLabel());
    }

    @Test
    public void testAttributeToAddress() {
        assertEquals("1/0", instance.attributeToAddress(attLength));

        assertEquals("1/1", instance.attributeToAddress(attData));
        assertEquals("1/1/0", instance.attributeToAddress(attDictMin));
        assertEquals("1/1/0", instance.attributeToAddress(attMin));
        assertEquals("1/1/0", instance.attributeToAddress(attKeyMin));

        assertEquals("1/2", instance.attributeToAddress(attArray));
        assertEquals("1/2/0", instance.attributeToAddress(att282));
    }
}
