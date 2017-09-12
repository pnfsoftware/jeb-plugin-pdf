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

import com.pnf.plugin.pdf.obj.PdfIndirectObj;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PdfIndirectObjectHelper {

    public static PdfIndirectObj parseIndirectObject(String data) {
        return parseIndirectObject(data, 4);
    }

    public static PdfIndirectObj parseIndirectObject(String data, int fromByte) {
        PdfIndirectObj o = new PdfIndirectObj(null, null, fromByte);
        o.parse(data.getBytes(), fromByte);
        return o;
    }
}
