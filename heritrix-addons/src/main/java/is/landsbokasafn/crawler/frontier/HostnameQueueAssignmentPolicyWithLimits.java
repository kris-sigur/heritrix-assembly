package is.landsbokasafn.crawler.frontier;

import org.archive.crawler.frontier.HostnameQueueAssignmentPolicy;
import org.archive.net.UURI;

/**
 * A variation on the Heritrix 3 supplied queue assignment policy <code>HostnameQueueAssignmentPolicy</code>.
 * <p>
 * This variation allows the operator (per sheet) to specify the maximum number of domains and
 * sub-domains to use for the queue name.
 * 
 * @deprecated in favor of {@link HostnameQueueAssignmentPolicyWithLimits}
 */
@Deprecated
public class HostnameQueueAssignmentPolicyWithLimits extends HostnameQueueAssignmentPolicy {
    private static final long serialVersionUID = 3L;

    public static final String LIMIT = "limit";
    
    // Default limit to -1 (no limit enforced)
    {
    	setLimit(-1);
    }
    /**
     * Set the maximum number of domains and sub-domains to include in the queue name.
     * <p>
     * E.g. if limit is set to <code>2</code> than the following assignments are made: <br/>
     * <code>example.com -> example.com</code> <br/>
     * <code>www.example.com -> example.com</code> <br/>
     * <code>subdomain.example.com -> example.com</code> <br/>
     * <code>www.subdomain.example.com -> example.com</code> <br/>
     * <code>otherdomain.com -> otherdomain.com</code> <br/>
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
    	return getLimitedHostname(super.getCoreKey(basis), limit);
    }

    protected String getLimitedHostname(String hostname, int limit){
    	if (limit <= 0) {
    		return hostname;
    	}
    	
		String[] domains = hostname.split("\\.");
		if (limit>=domains.length) {
			return hostname;
		}
		// More domains are present than allowed.
		StringBuffer limitedHostname = new StringBuffer();
		for (int i=domains.length-limit ; i<domains.length-1 ; i++){
			limitedHostname.append(domains[i]);
			limitedHostname.append(".");
		}
		limitedHostname.append(domains[domains.length-1]);
		return limitedHostname.toString();
    }
}
