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

import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.archive.crawler.spring.DecideRuledSheetAssociation;
import org.archive.crawler.spring.SurtPrefixesSheetAssociation;
import org.archive.spring.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Variant to allow {@link ExtSourceSurtPrefixesSheetAssociation} to load correctly 
 * by delaying the reading of the associations until spring context is fully constructed
 * 
 */
public class SheetOverlaysManager extends org.archive.crawler.spring.SheetOverlaysManager {
    private static final Logger logger = Logger.getLogger(SheetOverlaysManager.class.getName());

    private List<SurtPrefixesSheetAssociation> associations;
    
    /**
     * Collect all SURT-based SheetAssociations. Typically autowired 
     * from the set of all SurtPrefixesSheetAssociation instances
     * declared in the initial configuration. 
     */
    @Override
    @Autowired(required=false)
    public void addSurtAssociations(List<SurtPrefixesSheetAssociation> associations) {
    	this.associations = associations;
    }
    
    /**

    /** 
     * Ensure all sheets are 'primed' after the entire ApplicatiotnContext
     * is assembled. This ensures target HasKeyedProperties beans know
     * any long paths by which their properties are addressed, and 
     * handles (by either PropertyEditor-conversion or a fast-failure)
     * any type-mismatches between overlay values and their target
     * properties.
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextRefreshedEvent) {
            for(SurtPrefixesSheetAssociation association : associations) {
                addSurtsAssociation(association);
            }
        	
            for(Sheet s: sheetsByName.values()) {
                s.prime(); // exception if Sheet can't target overridable properties
            }
            // log warning for any sheets named but not present
            HashSet<String> allSheetNames = new HashSet<String>();
            for(DecideRuledSheetAssociation assoc : ruleAssociations) {
                allSheetNames.addAll(assoc.getTargetSheetNames());
            }
            for(List<String> names : sheetNamesBySurt.values()) {
                allSheetNames.addAll(names);
            }
            for(String name : allSheetNames) {
                if(!sheetsByName.containsKey(name)) {
                    logger.warning("sheet '"+name+"' referenced but absent");
                }
            }
        }
    }
}