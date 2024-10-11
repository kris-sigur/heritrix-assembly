package is.landsbokasafn.crawler.deciderules;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.PredicatedDecideRule;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.ServerCache;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Variant on the {@link IpAddressSetDecideRule} that accepts address ranges in
 * CIDR notation instead of individual IP addresses.
 * {@link IpAddressSetDecideRule} will perform better than this class in terms
 * of lookup time per URI evaluated. However, for large ranges this variant will
 * use a lot less memory as it does not expand the CIDR into individual IPs to
 * match against.
 * </p>
 * 
 * <p>
 * IpAddressCidrSetDecideRule must be used with
 * org.archive.crawler.prefetch.Preselector#setRecheckScope(boolean) set to true
 * because it relies on Heritrix' dns lookup to establish the ip address for a
 * URI before it can run.
 * </p>
 * 
 * <pre>
 * &lt;bean class="org.archive.modules.deciderules.IpAddressCidrSetDecideRule"&gt;
 *  &lt;property name="ipAddresseCidr"&gt;
 *   &lt;set&gt;
 *    &lt;value&gt;127.0.0.1/32&lt;/value&gt;
 *    &lt;value&gt;69.89.27.0/24&lt;/value&gt;
 *   &lt;/set&gt;
 *  &lt;/property&gt;
 *  &lt;property name='decision' value='REJECT' /&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * @author Travis Wellman &lt;travis@archive.org&gt;
 * @author Kristinn Sigur&eth;sson
 */

public class IpAddressCidrSetDecideRule extends PredicatedDecideRule {

	private static final Logger logger = Logger.getLogger(IpAddressCidrSetDecideRule.class.getName());

	private static final long serialVersionUID = -3670434739183271441L;
	private Set<String> ipAddressCidr;

	List<SubnetInfo> subnetInfos = null;

	/**
	 * @return the addresses being matched
	 */
	public Set<String> getIpAddressCidrs() {
		return Collections.unmodifiableSet(ipAddressCidr);
	}

	/**
	 * @param ipAddresses the addresses to match
	 */
	public void setIpAddressCidrs(Set<String> ipAddressCidr) {
		subnetInfos = null;
		this.ipAddressCidr = ipAddressCidr;
	}

	private List<SubnetInfo> getSubnetInfos() {
		if (subnetInfos == null) {
			subnetInfos = new LinkedList<>();
			for (String cidr : getIpAddressCidrs()) {
				subnetInfos.add((new SubnetUtils(cidr)).getInfo());
			}
		}
		return subnetInfos;
	}

	@Override
	protected boolean evaluate(CrawlURI curi) {
		String hostAddress = getHostAddress(curi);

		if (hostAddress == null) {
			return true;
		}

		for (SubnetInfo si : getSubnetInfos()) {
			if (si.isInRange(hostAddress)) {
				return true;
			}
		}

		return false;
	}

	transient protected ServerCache serverCache;

	public ServerCache getServerCache() {
		return this.serverCache;
	}

	@Autowired
	public void setServerCache(ServerCache serverCache) {
		this.serverCache = serverCache;
	}

	/**
	 * from WriterPoolProcessor
	 * 
	 * @param curi CrawlURI
	 * @return String of IP address or null if unable to determine IP address
	 */
	protected String getHostAddress(CrawlURI curi) {
        // if possible use the exact IP the fetcher stashed in curi
        if (curi.getServerIP() != null) {
            return curi.getServerIP();
        }
        // otherwise, consult the cache
		String addr = null;
		try {
			CrawlHost crlh = getServerCache().getHostFor(curi.getUURI());
			if (crlh == null) {
				return null;
			}
			InetAddress inetadd = crlh.getIP();
			if (inetadd == null) {
				return null;
			}
			addr = inetadd.getHostAddress();
		} catch (Exception e) {
			// Log error and continue (return null)
			logger.log(Level.WARNING, "Error looking up IP for URI " + curi.getURI(), e);
		}
		return addr;
	}
}
