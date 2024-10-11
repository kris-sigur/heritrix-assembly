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
 
package is.landsbokasafn.crawler.spring;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import org.archive.crawler.spring.SurtPrefixesSheetAssociation;
import org.archive.io.ReadSource;

/**
 * SheetAssociation applied on the basis of matching SURT prefixes loaded from an external source.
 * This still allows a list be specified in the CXML and any surts in the external source will be added to 
 * that list. 
 * 
 */
public class ExtSourceSurtPrefixesSheetAssociation extends SurtPrefixesSheetAssociation {
    private static final Logger logger = Logger.getLogger(ExtSourceSurtPrefixesSheetAssociation.class.getName());
	
    protected ReadSource prefixSource = null;
    public ReadSource getPrefixSource() {
        return prefixSource;
    }

    public void setPrefixSource(ReadSource prefixSource) {
        this.prefixSource = prefixSource;
        
    }

	@Override
	public List<String> getSurtPrefixes() {
		List<String> surtPrefixes = new ArrayList<>();
		BufferedReader br = new BufferedReader(prefixSource.obtainReader());
		String line;
		try {
			while ((line = br.readLine()) != null) {
				try {
					if (!line.startsWith("#") && !line.isEmpty()) {
						// Lines starting with # are comments and are ignored. Empty lines are also ignored
						surtPrefixes.add(line);
					}
				} catch (PatternSyntaxException pse) {
					logger.log(Level.WARNING, "Failed to compile regular expression: " + line,  pse);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}		

		if (this.surtPrefixes != null) {
			surtPrefixes.addAll(this.surtPrefixes);
		}
		
		return surtPrefixes;
		
	}

	@Override
	public void setSurtPrefixes(List<String> surtPrefixes) {
		super.setSurtPrefixes(surtPrefixes);
	}

    
    
	
}
