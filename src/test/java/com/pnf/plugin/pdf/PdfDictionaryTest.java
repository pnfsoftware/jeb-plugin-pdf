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
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfObjId;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfDictionaryTest extends TestCase {

    public static final String sample1 = "7 0 obj <</Linearized 1/L 7945/O 9 0 R/E 3524/N 1/T 7656/H [ 451 137]>> endobj";

    public void testWithIndirectReference() {
        PdfIndirectObj o = PdfIndirectObjectHelper.parseIndirectObject(sample1);
        PdfDictionary array = (PdfDictionary)o.getAttribute();
        assertEquals(7, array.getAttributes().size());
        PdfIndirectReference attribute3 = (PdfIndirectReference)array.getAttributes().get(2).getValue(); // indirect
                                                                                                        // reference
        assertEquals(new PdfObjId(9, 0, 0), attribute3.getId());
    }

}
