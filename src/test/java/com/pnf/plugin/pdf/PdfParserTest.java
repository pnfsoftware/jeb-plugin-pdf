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
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;

import com.pnf.plugin.pdf.obj.PdfBoolean;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfNumber;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnf.plugin.pdf.parser.PdfFile;
import com.pnf.plugin.pdf.parser.PdfParser;
import com.pnfsoftware.jeb.core.input.BytesInput;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfParserTest extends TestCase {

    private static final String OBJ1 = "1 0 obj\n15\nendobj\n";
    private static final String OBJ2 = "2 0 obj\n<</mykey 16>>\nendobj\n";
    private static final String OBJ3 = "3 0 obj\n<</Attribute<</Key 17>>>>\nendobj\n";
    private static final String OBJ4 = "4 0 obj\ntrue\nendobj";

    private static final String ALL_OBJ = (OBJ1 + OBJ2 + OBJ3 + OBJ4);

    @Test
    public void testParseBoolean() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(ALL_OBJ, 92);
        assertEquals("true", ((PdfBoolean)o.getAttribute()).toString());
        assertEquals(4, o.getId().getObjectNumber());
        assertEquals(0, o.getId().getGenerationNumber());
    }

    @Test
    public void testParseNumber() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(ALL_OBJ);
        assertEquals("15", ((PdfNumber)o.getAttribute()).toString());
    }

    @Test
    public void testParseDictionaryNumber() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(ALL_OBJ, 22);
        PdfDictionary dictionary = (PdfDictionary)o.getAttribute();
        assertEquals(1, dictionary.getAttributes().size());
        assertEquals("/mykey", dictionary.getAttributes().get(0).getKey().toString());
        assertEquals("16", ((PdfNumber)dictionary.getAttributes().get(0).getValue()).toString());
    }

    @Test
    public void testParseDictionaryDictionary() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(ALL_OBJ, 51);
        PdfDictionary dictionary = (PdfDictionary)o.getAttribute();
        assertEquals(1, dictionary.getAttributes().size());
        assertEquals("/Attribute", dictionary.getAttributes().get(0).getKey().toString());
        PdfDictionary subdictionary = (PdfDictionary)dictionary.getAttributes().get(0).getValue();
        assertEquals("/Key", subdictionary.getAttributes().get(0).getKey().toString());
        assertEquals("17", ((PdfNumber)subdictionary.getAttributes().get(0).getValue()).toString());
    }

    @Test
    public void testParseBytes() throws IOException {
        PdfFileUnit fileUnit = Mockito.mock(PdfFileUnit.class);
        Map<PdfObjId, PdfFile> objects = new TreeMap<PdfObjId, PdfFile>();
        Mockito.when(fileUnit.getStatistics()).thenReturn(new PdfStatistics(fileUnit));
        Mockito.when(fileUnit.getObjects()).thenReturn(objects);
        new PdfParser(fileUnit).parse(new BytesInput(ALL_OBJ.getBytes()).getStream());
        assertEquals(4, objects.size());
    }

}
