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
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfStream;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfStreamTest extends TestCase {

    public static final String sample1 = "1 0 obj <</Filter/FlateDecode/First 94/Length 773/N 13/Type/ObjStm>>stream\nanyContent/<<>>ddsqdcqs\nendstream endobj ";

    public static final String sampleNotAStream = "1 0 obj [ <</Filter/FlateDecode/First 94/Length 773/N 13/Type/ObjStm>> (stream) (anyContentddsqdcqs) (endstream)] endobj.";

    public void testNotAStream() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(sampleNotAStream);
        assertEquals(Type.Array, o.getAttribute().getType());
        PdfArray array = (PdfArray)o.getAttribute();
        assertEquals(4, array.getAttributes().size());
        assertEquals("stream", array.getAttributes().get(1).toString());
    }

    public void testStream() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(sample1);
        assertEquals(Type.Stream, o.getAttribute().getType());
        PdfStream stream = (PdfStream)o.getAttribute();
        assertEquals(5, stream.getDictionary().getAttributes().size());
        assertEquals("Stream <<  /Filter /FlateDecode    /First 94    /Length 773    /N 13    /Type /ObjStm  >>",
                stream.toString());
    }

}
