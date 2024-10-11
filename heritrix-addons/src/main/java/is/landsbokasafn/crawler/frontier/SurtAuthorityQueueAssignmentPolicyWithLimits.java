package is.landsbokasafn.crawler.frontier;

import org.archive.crawler.frontier.SurtAuthorityQueueAssignmentPolicy;
import org.archive.net.UURI;

/**
 * A variation on the Heritrix 3 supplied queue assignment policy <code>HostnameQueueAssignmentPolicy</code>.
 * <p>
 * This variation allows the operator (per sheet) to specify the maximum number of domains and
 * sub-domains to use for the queue name.
 * 
 * @deprecated in favor of {@link SurtAuthorityQueueAssignmentPolicyWithLimits}
 *
 */
@Deprecated
public class SurtAuthorityQueueAssignmentPolicyWithLimits extends SurtAuthorityQueueAssignmentPolicy {
    private static final long serialVersionUID = 3L;

    public static final String LIMIT = "limit";
    
    // Default limit to -1 (no limit enforced)
    {
    	setLimit(-1);
    }
    /**
     * Set the maximum number of surt segments to include in queue name.
     * <p>
     * E.g. if limit is set to <code>2</code> than the following assignments are made: <br/>
     * <code>com,example, -> com,example,</code> <br/>
     * <code>com,example,www, -> com,example,</code> <br/>
     * <code>com,example,subdomain, -> com,example,</code> <br/>
     * <code>com,example,subdomain,www, -> com,example,</code> <br/>
     * <code>com,otherdomain, -> com,otherdomain,</code> <br/>
     * <p>
     * The port part of the SURT is always preserved.
     *   
     * @param limit The limit on number of domains to set.
     */
    public void setLimit(int limit){
    	kp.put(LIMIT, limit);
    }
    public int getLimit(){
    	return (Integer)kp.get(LIMIT);
    }
    
    @Override
    protected String getCoreKey(UURI basis) {
    	int limit = (Integer)kp.get(LIMIT);
    	return getLimitedSurtAuthority(super.getCoreKey(basis), limit);
    }

    protected String getLimitedSurtAuthority(String surt, int limit){
    	if (limit <= 0) {
    		return surt;
    	}
    	String domainPart = surt;
    	String portPart = "";
    	int indexOfHash = surt.indexOf('#');
    	if (indexOfHash>-1) {
    		domainPart = surt.substring(0,indexOfHash);
    		portPart = surt.substring(indexOfHash);
    	}
		String[] segments = domainPart.split(",");
		if (limit>=segments.length) {
			return surt;
		}
		// More domains are present than allowed.
		StringBuffer limitedSurt = new StringBuffer();
		for (int i=0 ; i<limit ; i++){
			limitedSurt.append(segments[i]);
			limitedSurt.append(",");
		}
		limitedSurt.append(portPart);
		return limitedSurt.toString();
    }
}
