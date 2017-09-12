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

package com.pnf.plugin.pdf.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.pnf.plugin.pdf.PdfFileUnit;
import com.pnf.plugin.pdf.filter.PDFObject;
import com.pnf.plugin.pdf.obj.IPdfAttribute;
import com.pnf.plugin.pdf.obj.PdfIndirectObj;
import com.pnf.plugin.pdf.obj.PdfObjId;
import com.pnf.plugin.pdf.obj.PdfTrailer;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.jeb.util.serialization.annotations.Ser;
import com.pnfsoftware.jeb.util.serialization.annotations.SerId;
import com.pnfsoftware.jeb.util.serialization.annotations.SerTransient;
import com.sun.pdfview.decrypt.EncryptionUnsupportedByPlatformException;
import com.sun.pdfview.decrypt.EncryptionUnsupportedByProductException;
import com.sun.pdfview.decrypt.PDFDecrypter;
import com.sun.pdfview.decrypt.PDFDecrypterFactory;

/**
 * Represent a version of the file or the addition in the version of a file
 * 
 * @author PNF Software
 * 
 */
@Ser
public class PdfFile {

    private static final ILogger logger = GlobalLog.getLogger(PdfFile.class);

    @SerId(1)
    private PdfFileUnit unit;

    @SerId(2)
    private Map<PdfObjId, PdfIndirectObj> objects = new TreeMap<PdfObjId, PdfIndirectObj>();

    @SerId(3)
    private PdfTrailer trailer;

    // Do not save it to rebuild all other fields
    @SerTransient
    private boolean decryptionInitialized;

    @SerTransient
    private boolean isEncrypted;

    @SerTransient
    private PDFDecrypter decrypter;

    @SerTransient
    private PdfStringDecrypter stringDecrypt;

    @SerTransient
    private IPdfAttribute encryptDictionary;

    public PdfFile(PdfFileUnit unit) {
        this.unit = unit;
    }

    public List<PdfIndirectObj> getObjectList() {
        ArrayList<PdfIndirectObj> objectList = new ArrayList<PdfIndirectObj>(objects.values());
        Collections.sort(objectList);
        return objectList;
    }

    public void putObject(PdfObjId id, PdfIndirectObj t) {
        objects.put(id, t);
    }

    public int getObjectNumber() {
        return objects.size();
    }

    public Map<PdfObjId, PdfIndirectObj> getObjects() {
        return objects;
    }

    private void initEncryption() {
        decryptionInitialized = true;
        if(trailer == null) {
            logger.warn("No Xref found");
            return;
        }
        isEncrypted = trailer.isEncrypted();
        decrypter = null;
        if(isEncrypted) {
            unit.getStatistics().setEncrypted(true);
            try {
                encryptDictionary = trailer.getEncrypt();
                decrypter = PDFDecrypterFactory.createDecryptor(PDFObject.getInstance(encryptDictionary),
                        PDFObject.getInstance(trailer.getID()), null);
                stringDecrypt = new PdfStringDecrypter(decrypter);
            }
            catch(EncryptionUnsupportedByPlatformException | EncryptionUnsupportedByProductException | IOException e) {
                logger.error("Can not decrypt PDF file: PDF probably requires user password");
                // TODO display pop up, prompt with password
                unit.getStatistics().setEncrypted(true, true);
            }
        }
    }

    public void setTrailer(PdfTrailer trailer) {
        if(this.trailer == null) {
            this.trailer = trailer;
        }
    }

    public boolean isEncrypted() {
        if(!decryptionInitialized) {
            initEncryption();
        }
        return isEncrypted;
    }

    public PDFDecrypter getDecrypter() {
        if(!decryptionInitialized) {
            initEncryption();
        }
        return decrypter;
    }

    public PdfStringDecrypter getStringDecrypt() {
        if(!decryptionInitialized) {
            initEncryption();
        }
        return stringDecrypt;
    }

    public IPdfAttribute getEncryptDictionary() {
        if(!decryptionInitialized) {
            initEncryption();
        }
        return encryptDictionary;
    }

    public PdfIndirectObj getObject(PdfObjId key) {
        return objects.get(key);
    }

    public void setStartXref(PdfTrailer pdfTrailer) {
        // override with correct trailer
        if(pdfTrailer != null) {
            this.trailer = pdfTrailer;
        }
    }

}
