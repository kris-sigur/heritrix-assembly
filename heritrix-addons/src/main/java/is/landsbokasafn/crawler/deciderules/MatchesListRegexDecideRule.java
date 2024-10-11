package is.landsbokasafn.crawler.deciderules;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.archive.crawler.event.CrawlStateEvent;
import org.archive.crawler.framework.CrawlController.State;
import org.archive.io.ReadSource;
import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.MatchesRegexDecideRule;
import org.archive.modules.deciderules.PredicatedDecideRule;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Rule applies configured decision to any CrawlURIs whose String URI
 * matches the regexs supplied via a ReadSource or added explicitly.
 * <p>
 * The list of regular expressions can be considered logically AND or OR.
 *
 * @author Kristinn Sigurdsson
 * 
 * @see MatchesRegexDecideRule
 */
public class MatchesListRegexDecideRule extends PredicatedDecideRule 
		implements ApplicationListener<ApplicationEvent> {
    private static final long serialVersionUID = 3L;
    private static final Logger logger = Logger.getLogger(MatchesListRegexDecideRule.class.getName());

    ConcurrentHashMap<Pattern, AtomicInteger> triggeredPatterns = new ConcurrentHashMap<>();  
    
    protected ReadSource regexSource = null;
    public ReadSource getRegexSource() {
        return regexSource;
    }
    public void setRegexSource(ReadSource regexSource) {
        this.regexSource = regexSource;
    }
    
    /**
     * The list of regular expressions to evaluate against the URI.
     */
    {
        setRegexList(new CopyOnWriteArrayList<>());
    }
    @SuppressWarnings("unchecked")
    private List<Pattern> getRegexList() {
        return (List<Pattern>) kp.get("regexList");
    }
    private void setRegexList(List<Pattern> patterns) {
        kp.put("regexList", patterns);
    }

    /**
     * True if the list of regular expression should be considered as logically
     * OR when matching. False if the list of regular expressions should be
     * considered as logically AND when matching.
     */
    {
        setListLogicalOr(true);
    }
    public boolean getListLogicalOr() {
        return (Boolean) kp.get("listLogicalOr");
    }
    public void setListLogicalOr(boolean listLogicalOr) {
        kp.put("listLogicalOr",listLogicalOr);
    }

    /**
     * Usual constructor. 
     */
    public MatchesListRegexDecideRule() {
    }

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof CrawlStateEvent) {
			CrawlStateEvent cse = (CrawlStateEvent)event;
			if (cse.getState().equals(State.PREPARING)) {
				importRegexes();
			}
		}
	}
    
	public void add(String regex) {
		for (Pattern p : getRegexList()) {
			if (p.toString().equals(regex)) {
				// Already added
				return;
			}
		}
		
		getRegexList().add(Pattern.compile(regex));
	}
	
	public void remove(String regex) {
		for (Pattern p : getRegexList()) {
			if (p.toString().equals(regex)) {
				getRegexList().remove(p);
			}
		}
	}
	
	public String getList() {
		StringBuilder list = new StringBuilder();
		for (Pattern p : getRegexList()) {
			list.append(p.toString());
			list.append("\n");
		}
		return list.toString();
	}
	
    protected void importRegexes() {
    	if (getRegexSource()==null) {
    		throw new IllegalStateException("Missing source for regular expressions");
    	}
		BufferedReader br = new BufferedReader(getRegexSource().obtainReader());
		String line;
		try {
			while ((line = br.readLine()) != null) {
				try {
					if (!line.startsWith("#") && !line.isBlank()) {
						// Lines starting with # are comments and are ignored. Empty lines are also ignored
						add(line);
					}
				} catch (PatternSyntaxException pse) {
					logger.log(Level.WARNING, "Failed to compile regular expression: " + line,  pse);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}		
	}
    
	/**
     * Evaluate whether given object's string version
     * matches configured regexes
     */
    @Override
    protected boolean evaluate(CrawlURI uri) {
        List<Pattern> regexes = getRegexList();
        if(regexes.isEmpty()){
            return false;
        }

        String str = uri.toString();
        boolean listLogicOR = getListLogicalOr();

        for (Pattern p: regexes) {
            boolean matches = p.matcher(str).matches();

            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Tested '" + str + "' match with regex '" +
                    p.pattern() + " and result was " + matches);
            }
            
			if (matches) {
				if (listLogicOR) {
                    // OR based and we just got a match, done!
                    count(p);
                    return true;
                }
            } else {
				if (!listLogicOR) {
                    // AND based and we just found a non-match, done!
                    count(p);
                    return false;
                }
            }
        }
        
        if (listLogicOR) {
            return false;
        } else {
            return true;
        }
    }
    
    private void count(Pattern p) {
    	if (triggeredPatterns.containsKey(p)) {
    		triggeredPatterns.get(p).incrementAndGet();
    	} else {
    		triggeredPatterns.put(p, new AtomicInteger(1));
    	}
    }
    
    public String getReport() {
    	StringBuilder report = new StringBuilder();
    	for (Pattern p : getRegexList()) {
    		int hits = 0;
    		if (triggeredPatterns.containsKey(p)) {
    			hits = triggeredPatterns.get(p).get();
    		}
			report.append(p.toString() + "\t" + hits + "\n");
    		
    	}
    	
    	return report.toString();
    }
    
}