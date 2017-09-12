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

import junit.framework.TestCase;

import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfArrayTest extends TestCase {
    private static final String OBJ8 = "8 0 obj\n[true 16]\nendobj";
    private static final String OBJ9 = "9 0 obj\n[ 549 [ 3.14 false] ( Ralph ) ]\nendobj";
    private static final String OBJ10 = "10 0 obj\n[<</Attribute (mystr)>> %toto titi\n<<%get\n/Attribute2 %try\n(mys tr)>>]\nendobj\n";

    private PdfIndirectObj o;

    public void testSimpleArray() {
        o = PdfIndirectObjectHelper.parseIndirectObject(OBJ8);
        assertEquals(Type.Array, o.getAttribute().getType());
        PdfArray array = (PdfArray)o.getAttribute();
        assertEquals(2, array.getAttributes().size());
        assertEquals("true", array.getAttributes().get(0).toString());
        assertEquals(Type.Boolean, array.getAttributes().get(0).getType());
        assertEquals("16", array.getAttributes().get(1).toString());
        assertEquals(Type.Number, array.getAttributes().get(1).getType());
    }

    public void testStringArray() {
        o = PdfIndirectObjectHelper.parseIndirectObject(OBJ9);
        PdfArray array = (PdfArray)o.getAttribute();
        assertEquals(3, array.getAttributes().size());
        assertEquals("549", array.getAttributes().get(0).toString());
        PdfArray subarray = (PdfArray)array.getAttributes().get(1);
        assertEquals("3.14", subarray.getAttributes().get(0).toString());
        assertEquals(Type.Number, array.getAttributes().get(0).getType());
        assertEquals("false", subarray.getAttributes().get(1).toString());
        assertEquals(" Ralph ", array.getAttributes().get(2).toString());
        assertEquals(Type.String, array.getAttributes().get(2).getType());
    }

    public void testDictionaryArray() {
        o = PdfIndirectObjectHelper.parseIndirectObject(OBJ10, 5);
        PdfArray array = (PdfArray)o.getAttribute();
        assertEquals(2, array.getAttributes().size());
        assertEquals(Type.Dictionary, array.getAttributes().get(0).getType());
        PdfDictionary dict1 = (PdfDictionary)array.getAttributes().get(0);
        assertEquals("/Attribute", dict1.getAttributes().get(0).getKey().toString());
        assertEquals("mystr", dict1.getAttributes().get(0).getValue().toString());
        PdfDictionary dict2 = (PdfDictionary)array.getAttributes().get(1);
        assertEquals("/Attribute2", dict2.getAttributes().get(0).getKey().toString());
        assertEquals("mys tr", dict2.getAttributes().get(0).getValue().toString());
    }
}
