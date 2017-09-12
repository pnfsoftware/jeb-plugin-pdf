/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.filter;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.pnf.plugin.pdf.filter.PDFObject;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * Modeled on the JBIG2Decode filter.
 *
 * thanks to Timo Boehme <timo.boehme@ontochem.com>
 */

public class JBIG2Filter {

    /** Log instance. */
    private static final ILogger LOG = GlobalLog.getLogger(JBIG2Filter.class);

    /**
     * Decode JBIG2 data using Java ImageIO library.
     *
     * {@inheritDoc}
     *
     */
    public void decode(InputStream compressedData, OutputStream result, PDFObject dictionary, PDFObject params)
        throws IOException
    {
        /**
         *  A working JBIG2 ImageIO plugin is needed to decode JBIG2 encoded streams.
         *  The following is known to be working. It can't be bundled with PDFBox because of an incompatible license.
         *  http://code.google.com/p/jbig2-imageio/ 
         */
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JBIG2");
        if (!readers.hasNext())
        {
            LOG.error( "Can't find an ImageIO plugin to decode the JBIG2 encoded datastream.");
            return;
        }
        ImageReader reader = readers.next();

        Integer bits = dictionary.getDictRef("BitsPerComponent").getIntValue();
        PDFObject st = null;
        if (params != null)
        {
            st = params.getDictRef("JBIG2Globals");
        }
        if (st != null)
        {
            compressedData = new SequenceInputStream(st.getUnfilteredStream(), compressedData);
        }
        ImageInputStream iis = ImageIO.createImageInputStream(compressedData);
        reader.setInput(iis);
        BufferedImage bi = reader.read(0, reader.getDefaultReadParam());
        iis.close();
        reader.dispose();
        if ( bi != null )
        {
            // I am assuming since JBIG2 is always black and white 
            // depending on your renderer this might or might be needed
            if(bi.getColorModel().getPixelSize() != bits.intValue()) 
            {
                if(bits.intValue() != 1)
                {
                    LOG.error("Do not know how to deal with JBIG2 with more than 1 bit");
                    return;
                }
                BufferedImage packedImage = 
                        new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics graphics = packedImage.getGraphics();  
                graphics.drawImage(bi, 0, 0, null);
                graphics.dispose();
                bi = packedImage;
            }
            DataBuffer dBuf = bi.getData().getDataBuffer();
            if ( dBuf.getDataType() == DataBuffer.TYPE_BYTE )
            {
                result.write( ( ( DataBufferByte ) dBuf ).getData() );
            }
            else
            {
                LOG.error( "Image data buffer not of type byte but type " + dBuf.getDataType() );
            }
        }
        else
        {
            LOG.error( "Something went wrong when decoding the JBIG2 encoded datastream.");
        }
    }

}
