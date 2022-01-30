/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.datahandler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Checks input for XML-escapes, e.g. {@code &#xABCD} or {@code &#12345}. If an escape refers to an illegal Unicode codepoint,
 * it is replaced with '?' (or a custom String specified in config).
 * If it is not illegal, the escape is left as-is.
 */
public class XMLEscapeSanitiser extends CallbackReplacer {
    private static final Logger log = LoggerFactory.getLogger(XMLEscapeSanitiser.class);
    private static final Pattern ESCAPE = Pattern.compile("&#x?[a-fA-F0-9]+;");

    private final String replacement;

    /**
     * Constructs an XML unicode escape validator, where illegal Unicode codepoint are replaced with {@code ?}.
     */
    public XMLEscapeSanitiser() {
        this("?");
    }

    /**
     * Constructs an XML unicode escape validator.
     * @param replacement returned if an illegal Unicode codepoint is encountered.
     */
    public XMLEscapeSanitiser(String replacement) {
        super(ESCAPE, getEscapeSanitizer(replacement));
        this.replacement = replacement;
    }

    /**
     * Isolates the unicode part of an XML escape (either hex or decimal) and parses that to a long, then checks if the
     * alledged Unicode codepoint is valid. If it is balid, the full original escape is returned, else the replacement
     * character (fefault {@code ?} is returned.
     * @param replacement the replacement character for illegal codepoints.
     * @return the original input if valid Unicode escape, else replacement.
     */
    public static Function<String, String> getEscapeSanitizer(String replacement) {
        return escape -> { // &#xABCD; or &#12345; (or &#xAB etc.)
            try {
                long unicode;
                if (escape.charAt(2) == 'x') { // &#xABCD;
                    unicode = Long.parseLong(escape.substring(3, escape.length()-1), 16); // &#xABCD; -> ABCD
                } else { // &#12345;
                    unicode = Long.parseLong(escape.substring(2, escape.length()-1)); // &#12345; -> 12345
                }
                // List taken from http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html
                // TODO: Verify the list from an authoritative Unicode source
                if ((unicode == 0x9) ||
                    (unicode == 0xA) ||
                    (unicode == 0xD) ||
                    ((unicode >= 0x20) && (unicode <= 0xD7FF)) ||
                    ((unicode >= 0xE000) && (unicode <= 0xFFFD)) ||
                    ((unicode >= 0x10000) && (unicode <= 0x10FFFF))) {
                    return escape; // All OK
                }
                log.trace("Illegal XML escape character '" + escape + "'");
                return replacement;
            } catch (Exception e) {
                log.warn("Exception processing '" + escape + "'", e);
            }
            return null;
        };
    }

    @Override
    public String toString() {
        return "XMLEscapeSanitiser(" + super.toString() + ")";
    }
}
