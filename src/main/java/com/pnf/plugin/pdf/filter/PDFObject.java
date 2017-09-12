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

package com.pnf.plugin.pdf.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.IPdfAttribute.Type;
import com.pnf.plugin.pdf.obj.PdfArray;
import com.pnf.plugin.pdf.obj.PdfDictionary;
import com.pnf.plugin.pdf.obj.PdfDictionaryAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectReference;
import com.pnf.plugin.pdf.obj.PdfStream;
import com.sun.pdfview.PDFStringUtil;

/**
 * 
 * 
 * @author PNF Software
 *
 */
public class PDFObject {

    private static final Type ARRAY = Type.Array;
    private static final Type INDIRECT = Type.IndirectReference;
    private static final Type DICTIONARY = Type.Dictionary;
    private static final Type STREAM = Type.Stream;
    private static final Type NUMBER = Type.Number;
    private static final Type BOOLEAN = Type.Boolean;
    private static final Type NAME = Type.Name;
    private static final Type STRING = Type.String;

    private IPdfAttribute dict;
    private Type type;

    private SoftReference<Object> cache;

    private PDFObject(IPdfAttribute dict) {
        this.dict = dict;
        this.type = dict.getType();
    }

    public static PDFObject getInstance(IPdfAttribute dict) {
        if(dict == null) {
            return null;
        }
        return new PDFObject(dict);
    }

    public int getObjNum() {
        return dict.getId().getObjectNumber();
    }

    public int getObjGen() {
        return dict.getId().getGenerationNumber();
    }

    public Type getType() {
        return dict.getType();
    }

    public PDFObject getDictRef(String key) throws IOException {
        if(type == INDIRECT) {
            return dereference().getDictRef(key);
        }
        else if(type == DICTIONARY) {
            return getInstance(((PdfDictionary)dict).getAttribute("/" + key));
        }
        else if(type == STREAM) {
            return getInstance(((PdfStream)dict).getAttribute("/" + key));
        }

        // wrong type
        return null;
    }

    public int getIntValue() throws IOException {
        if(type == INDIRECT) {
            return dereference().getIntValue();
        }
        else if(type == NUMBER) {
            try {
                return Integer.valueOf(dict.toString());
            }
            catch(Exception e) {
                try {
                    return Long.valueOf(dict.toString()).intValue();
                }
                catch(Exception e2) {
                    e.printStackTrace();
                }
            }
        }

        // wrong type
        return 0;
    }

    public boolean getBooleanValue() throws IOException {
        if(type == INDIRECT) {
            return dereference().getBooleanValue();
        }
        else if(type == BOOLEAN) {
            return Boolean.valueOf(dict.toString());
        }

        // wrong type
        return false;
    }

    public PDFObject getAt(int i) throws IOException {
        if(type == INDIRECT) {
            return dereference().getAt(i);
        }
        else if(type == ARRAY) {
            return getInstance(((PdfArray)dict).getAttributes().get(i));
        }

        // wrong type
        return null;
    }

    public float getFloatValue() throws IOException {
        if(type == INDIRECT) {
            return dereference().getFloatValue();
        }
        else if(type == NUMBER) {
            return Double.valueOf(dict.toString()).floatValue();
        }

        // wrong type
        return 0;
    }

    public String getStringValue() throws IOException {
        if(type == INDIRECT) {
            return dereference().getStringValue();
        }
        else if(type == STRING) {
            return dict.toString();
        }
        else if(type == NAME) {
            return dict.toString().substring(1);
        }

        // wrong type
        return null;
    }

    public HashMap<String, PDFObject> getDictionary() throws IOException {
        List<PdfDictionaryAttribute> dictAttributes = null;
        if(type == INDIRECT) {
            return dereference().getDictionary();
        }
        else if(type == DICTIONARY) {
            dictAttributes = ((PdfDictionary)dict).getAttributes();
        }
        else if(type == STREAM) {
            dictAttributes = ((PdfStream)dict).getDictionary().getAttributes();
        }
        else {
            // wrong type
            return new HashMap<String, PDFObject>();
        }

        HashMap<String, PDFObject> dictionary = new HashMap<String, PDFObject>();
        for(PdfDictionaryAttribute att: dictAttributes) {
            dictionary.put(att.getKey().toString().substring(1), getInstance(att.getValue()));
        }
        return dictionary;
    }

    public InputStream getUnfilteredStream() throws IOException {
        if(type == INDIRECT) {
            return dereference().getUnfilteredStream();
        }
        else if(type == STREAM) {
            return new ByteArrayInputStream(((PdfStream)dict).getDecodedData());
        }
        return null;
    }

    @Override
    public String toString() {
        return dict.toString();
    }

    public byte[] getStream() throws IOException {
        if(type == INDIRECT) {
            return dereference().getStream();
        }
        else if(type == STREAM) {
            PdfStream stream = (PdfStream)dict;
            if(stream == null) {
                return null;
            }
            return stream.getDecodedData();

        }
        else if(type == STRING) {
            return PDFStringUtil.asBytes(getStringValue());
        }
        else {
            // wrong type
            return null;
        }
    }

    public Iterator<String> getDictKeys() throws IOException {
        if(type == INDIRECT) {
            return dereference().getDictKeys();
        }
        else if(type == DICTIONARY) {
            return ((PdfDictionary)dict).getAttributeKeys().iterator();
        }
        else if(type == STREAM) {
            return ((PdfStream)dict).getDictionary().getAttributeKeys().iterator();
        }

        // wrong type
        return new ArrayList<String>().iterator();
    }

    public PDFObject[] getArray() throws IOException {
        if(type == INDIRECT) {
            return dereference().getArray();
        }
        else if(type == ARRAY) {
            List<IPdfAttribute> atts = ((PdfArray)dict).getAttributes();
            PDFObject[] result = new PDFObject[atts.size()];
            for(int i = 0; i < result.length; i++) {
                result[i] = getInstance(atts.get(i));
            }
            return result;
        }
        else {
            PDFObject[] ary = new PDFObject[1];
            ary[0] = this;
            return ary;
        }
    }

    public ByteBuffer getStreamBuffer() throws IOException {
        if(type == INDIRECT) {
            return dereference().getStreamBuffer();
        }
        else if(type == STREAM) {
            PdfStream stream = (PdfStream)dict;
            if(stream == null) {
                return null;
            }
            return ByteBufferUtils.getByteBuffer(stream.getDecodedData(), 0, stream.getDecodedData().length);
        }
        else if(type == STRING) {
            String src = getStringValue();
            return ByteBuffer.wrap(src.getBytes());
        }

        // wrong type
        return null;
    }

    /**
     * Make sure that this object is dereferenced. Use the cache of an indirect object to cache the
     * dereferenced value, if possible.
     */
    public PDFObject dereference() {
        if(type == INDIRECT) {
            IPdfAttribute ref = PdfIndirectReference.retrieveDirectObject(dict);
            if(ref == null) {
                return null;
            }
            return getInstance(ref);
        }
        else {
            // not indirect, no need to dereference
            return this;
        }
    }

    public Object getCache() throws IOException {
        if(type == INDIRECT) {
            return dereference().getCache();
        }
        else if(cache != null) {
            return cache.get();
        }
        else {
            return null;
        }
    }

    /**
     * set the cached value. The object may be garbage collected if no other reference exists to it.
     * 
     * @param obj the object to be cached
     */
    public void setCache(Object obj) throws IOException {
        if(type == INDIRECT) {
            dereference().setCache(obj);
            return;
        }
        else {
            cache = new SoftReference<Object>(obj);
        }
    }

}
