package is.landsbokasafn.crawler.deciderules;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.DecideResult;
import org.archive.modules.deciderules.DecideRule;

@SuppressWarnings("serial")
public class PathSegmentsDecideRule extends DecideRule {
    private static final Logger logger = Logger.getLogger(PathSegmentsDecideRule.class.getName());
    /**
     * The maximum number of times any two identical path segments may appear in an URL, regardless of their
     * relative position.
     * A setting of 0 or lower disables this check.
     */
    {
        setMaximumIdenticalSegments(3);
    }
    public int getMaximumIdenticalSegments() {
        return (Integer) kp.get("maximumIdenticalSegments");
    }
    public void setMaximumIdenticalSegments(int maximumIdenticalSegments) {
        kp.put("maximumIdenticalSegments",maximumIdenticalSegments);
    }

    /**
     * Maximum number of times that an identical path segment can be repeated without other path segments coming 
     * between them. Setting this value to be equal or higher than "maximumIdenticalSegment" effectively disables it.
     * A value of 0 or lower explicitly disables it.
     */
    {
        setMaximumIdenticalConsecutiveSegments(2);
    }
    public int getMaximumIdenticalConsecutiveSegments() {
        return (Integer) kp.get("maximumIdenticalConsecutiveSegments");
    }
    public void setMaximumIdenticalConsecutiveSegments(int maximumIdenticalConsecutiveSegments) {
        kp.put("maximumIdenticalConsecutiveSegments",maximumIdenticalConsecutiveSegments);
    }

    
    @Override
    protected DecideResult innerDecide(CrawlURI uri) {
        try {
            int maxSeg = getMaximumIdenticalSegments();
            int maxConSeg = getMaximumIdenticalConsecutiveSegments();
            
            if (maxSeg <= 0 && maxConSeg <= 0) {
                // Rule is effectively disabled
                return DecideResult.NONE;
            }
            
            String[] segments = getSegments(uri.getURI());
            
            return evaluate(maxSeg, maxConSeg, segments);
        } catch (Exception e) {
            // An exception occurred. Log it and return the default verdict
            logger.log(Level.WARNING, "Error on: " + uri.getURI(), e);
        }
        return DecideResult.NONE;
    }
    
    protected DecideResult evaluate(int maxSeg, int maxConSeg, String[] segments) {
        int consecutive = 0;
        String lastSeg = "";
        Map<String, Integer> segMap = new HashMap<String, Integer>();
        DecideResult result = DecideResult.NONE; // Default proposition
        for (String seg : segments) {
            if (seg.equals(lastSeg)) {
                consecutive++;
            } else {
                consecutive = 1;
            }
            if (consecutive>maxConSeg && maxConSeg>0 ) {
                // Threshold for maximum identical consecutive segments exceeded.
                result = DecideResult.REJECT;
                break;
            }
            Integer count = segMap.get(seg);
            if (count==null) {
                count=1;
            } else {
                count++;
            }
            if (count > maxSeg && maxSeg > 0) {
                // Threshold for maximum identical segments exceeded.
                result = DecideResult.REJECT;
                break;
            }
            segMap.put(seg, count);
            lastSeg = seg;
        }
        
        return result;
    }
    
    /**
     * Returns path segments from an URL. Path segments are always separated by forward slashes. The protocol and
     * the domain are excluded. In the case of a series of consecutive forward slashes they should be treated as a 
     * single separator. The first instance of ? should mark the termination of the relevant portion of the 
     * URL and should be included in entirety in the last segment, regardless of the presence of forward slashes
     * after the question mark character.
     * @param uri
     * @return
     */
    protected String[] getSegments(String uri){
        String url = uri;
        // The third occurrence of a forwards slash should mark the beginning of the path segments.
        for (int i=0 ; i<3 ; i++) {
            int index = url.indexOf('/');
            if (index<0) {
                break;
            }
            url = url.substring(index+1);
        }
        // Remove leading /
        while (url.startsWith("/")) {
            url = url.substring(1);
        }
        int paramsBegin = url.indexOf('?');
        String params = "";
        if (paramsBegin!=-1) {
            params = url.substring(paramsBegin+1);
            url = url.substring(0,paramsBegin+1);
        }
        url = url.replaceAll("//+", "/");
        String[] result = url.split("/");
        result[result.length-1]+=params;
        return result;
    }
    

}
