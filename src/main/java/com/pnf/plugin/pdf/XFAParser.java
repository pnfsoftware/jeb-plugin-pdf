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

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * Parses XFA to detect malicious content (script tags). The parser should ignore errors that can
 * happen (missing tag end, invalid chars). The implementation is done using SAX which process all
 * file.
 * 
 * @author PNF Software
 *
 */
public class XFAParser {
    private static final ILogger logger = GlobalLog.getLogger(XFAParser.class);

    private XFAHandler xfa = new XFAHandler();

    public void parse(byte[] xmlContent) throws ParserConfigurationException, SAXException, IOException {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharBuffer parsed = decoder.decode(ByteBuffer.wrap(xmlContent));

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        try(@SuppressWarnings("deprecation")
        InputStream is = new ReaderInputStream(new CharArrayReader(parsed.array()))) {
            parser.parse(is, xfa);
        }
        catch(Exception e) {
            logger.catching(e);
            logger.error("Error while parsing XFA content");
        }
    }

    public List<byte[]> getJavaScripts() {
        return xfa.javascripts;
    }

    public List<byte[]> getScripts() {
        return xfa.scripts;
    }

    private class XFAHandler extends DefaultHandler {
        private class Script {
            private StringBuilder stb = new StringBuilder();;
            private boolean isJs = false;

        }

        private List<byte[]> javascripts = new ArrayList<>();
        private List<byte[]> scripts = new ArrayList<>();

        private Script script;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if(script != null) {
                logger.error("Previous script node was not closed before %s", qName);
                endScript();
            }
            if(qName.equals("script")) {
                startScript();
                String sc = attributes.getValue("contentType");
                if("application/x-javascript".equals(sc)) {
                    script.isJs = true;
                }
            }
        }

        private void startScript() {
            script = new Script();
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(script != null) {
                if(!qName.equals("script")) {
                    logger.error("Unexpected end tag %s, expected script end node", qName);
                }
            }
            endScript();
        }

        private void endScript() {
            if(script != null) {
                if(script.isJs) {
                    javascripts.add(script.stb.toString().getBytes());
                }
                else {
                    scripts.add(script.stb.toString().getBytes());
                }
                script = null;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if(script != null) {
                script.stb.append(ch, start, length);
            }
        }
    }
}
