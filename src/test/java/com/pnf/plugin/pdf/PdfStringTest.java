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

import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfString;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfStringTest extends TestCase {
    private static final String OBJ5 = "5 0 obj\n(my string)\nendobj";
    private static final String OBJ6 = "6 0 obj\n(mystring \\n\53oh\\053 (my))\nendobj";
    private static final String OBJ7 = "7 0 obj\n<</Attribute (mystr\\()>>\nendobj\n";
    private static final String OBJ8 = "6 0 obj\n< 4E6F762073686D6F7A206B6120706F702E 2 >\nendobj";

    public void testSimpleString() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(OBJ5);
        assertEquals("my string", ((PdfString)o.getAttribute()).getValue());
    }

    public void testStringParenthesis() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(OBJ6);
        assertEquals("mystring \n+oh+ my", ((PdfString)o.getAttribute()).getValue());
    }

    public void testStringInDictionary() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(OBJ7);
        PdfDictionary dictionary = (PdfDictionary)o.getAttribute();
        assertEquals(1, dictionary.getAttributes().size());
        assertEquals("/Attribute", dictionary.getAttributes().get(0).getKey().toString());
        assertEquals("mystr(", ((PdfString)dictionary.getAttributes().get(0).getValue()).getValue());
    }

    public void testHexaString() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(OBJ8);
        assertEquals("Nov shmoz ka pop. ", ((PdfString)o.getAttribute()).getValue());
    }

}
