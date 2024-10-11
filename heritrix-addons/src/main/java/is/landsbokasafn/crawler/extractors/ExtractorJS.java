/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package is.landsbokasafn.crawler.extractors;

import static org.archive.modules.extractor.Hop.SPECULATIVE;
import static org.archive.modules.extractor.LinkContext.JS_MISC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.NestableRuntimeException;
import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.ConfigurableExtractorJS;
import org.archive.modules.extractor.Extractor;
import org.archive.util.UriUtils;

/**
 * Subclasses the standard ExtractorJS to add configurable blacklist -- both as literal and 
 * regex -- for extracted relative URLs in JS and enable strict mode. 
 * 
 * @deprecated in favor of {@link ConfigurableExtractorJS}
 */
@Deprecated
public class ExtractorJS extends org.archive.modules.extractor.ExtractorJS {
    private static Logger LOGGER = 
            Logger.getLogger(ExtractorJS.class.getName());
	
    long foundFalsePositives = 0;

    /** If true, then only extract absolute paths */
    protected boolean strict = false; 
    public boolean getStrict() {
        return strict;
    }
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    
    /**
     * The list of regular expressions to evalute potential relative url against, rejecting any that match
     */
    public List<String> getRejectRelativeMatchingRegexList() {
        return rejectRelativeMatchingRegexList;
    }
    public void setRejectRelativeMatchingRegexList(List<String> patterns) {
    	rejectRelativeMatchingRegexList = patterns;
    	rejectRelativeMatchingRegexListPatterns = new ArrayList<>();
    	for (String p : patterns) {
    		rejectRelativeMatchingRegexListPatterns.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE));
    	}
    }
    public void addRejectRelativeMatchingRegex(String pattern) {
		rejectRelativeMatchingRegexListPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
    }
    
    private List<String> rejectRelativeMatchingRegexList = new ArrayList<>();
    private List<Pattern> rejectRelativeMatchingRegexListPatterns;


    /**
     * The list of literal strings that should never be extracted as a potential relative url 
     */
    public List<String> getRejectRelativeIgnoreList() {
        return rejectRelativeIgnoreSet.stream().collect(Collectors.toList());
    }
    public void setRejectRelativeIgnoreList(Set<String> ignoreStrings) {
    	rejectRelativeIgnoreSet.clear();
    	rejectRelativeIgnoreSet.addAll(ignoreStrings);
    }
    public void addRejectRelativeIgnoreList(String ignoreString) {
		rejectRelativeIgnoreSet.add(ignoreString);
    }
    
    private Set<String> rejectRelativeIgnoreSet = new HashSet<>();
    

    @Override
    protected boolean considerString(Extractor ext, CrawlURI curi,
            boolean handlingJSFile, String candidate) {
        try {
            candidate = StringEscapeUtils.unescapeJavaScript(candidate);
        } catch (NestableRuntimeException e) {
            LOGGER.log(Level.WARNING, "problem unescaping some javascript", e);
        }
        
        candidate = UriUtils.speculativeFixup(candidate, curi.getUURI());

        if ( isStrictMatch(candidate) && 
        		UriUtils.isVeryLikelyUri(candidate) && 
        		!shouldIgnorePossibleRelativeLink(candidate)) {
            try {
                int max = ext.getExtractorParameters().getMaxOutlinks();
                if (handlingJSFile) {
                    addRelativeToVia(curi, max, candidate, JS_MISC, 
                            SPECULATIVE);
                    return true;
                } else {
                    addRelativeToBase(curi, max, candidate, JS_MISC, 
                            SPECULATIVE);
                    return true;
                }
            } catch (URIException e) {
                ext.logUriError(e, curi.getUURI(), candidate);
            }
        }
        
        return false;
    }
    
    private boolean isStrictMatch(String candidate) {
    	if (!getStrict()) {
    		return true;
    	}
    	return candidate.startsWith("http://") || candidate.startsWith("https://");
    }
    
	private boolean shouldIgnorePossibleRelativeLink(String string) {
		if (string.startsWith("http://") || string.startsWith("https://")) {
			// Absolute path. Assume it is ok.
			return false;
		}

		if (string.contains("/.") 
				|| string.contains("@") 
				|| string.length() > 150 
				// If the string is in the set of literal strings to ignore, 
				// then treat as false positive
				|| rejectRelativeIgnoreSet.contains(string)) {
			// While legal in URIs, these are rare and usually an indication of a false
			// positive in the speculative extraction.
			foundFalsePositives++;
			return true;
		}

		for (Pattern p : rejectRelativeMatchingRegexListPatterns) {
			boolean matches = p.matcher(string).matches();
			if (matches) {
				foundFalsePositives++;
				return true;
			}
		}

		return false;
	}
    


	@Override
	public String report() {
        StringBuilder report = new StringBuilder();
        report.append(super.report());
        report.append("  False positives eliminated: " + foundFalsePositives + "\n"); 
		return report.toString();
	}
    
    
}
