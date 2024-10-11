package is.landsbokasafn.crawler.deciderules;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.net.util.SubnetUtils;
import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.PredicatedDecideRule;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.ServerCache;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Variant on the {@link org.archive.modules.deciderules.IpAddressSetDecideRule}
 * that also accepts address ranges in CIDR notation instead of just individual
 * IP addresses. Note that the CIDR addresses are expanded and stored as
 * individual addresses. This is suitable for small-ish subnets, but for larger
 * ranges use {@link IpAddressCidrSetDecideRule}
 * </p>
 * 
 * <p>
 * IpAddressSetDecideRule must be used with
 * org.archive.crawler.prefetch.Preselector#setRecheckScope(boolean) set to true
 * because it relies on Heritrix' dns lookup to establish the ip address for a
 * URI before it can run.
 * </p>
 * 
 * <pre>
 * &lt;bean class="org.archive.modules.deciderules.IpAddressCidrSetDecideRule"&gt;
 *  &lt;property name="ipAddresseCidr"&gt;
 *   &lt;set&gt;
 *    &lt;value&gt;127.0.0.1&lt;/value&gt;
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

public class IpAddressSetDecideRule extends PredicatedDecideRule {

	private static final Logger logger = Logger.getLogger(IpAddressSetDecideRule.class.getName());

	private static final long serialVersionUID = -3670434739183271441L;
	private Set<String> ipAddresses;

	/**
	 * @return the addresses being matched
	 */
	public Set<String> getIpAddresses() {
		return Collections.unmodifiableSet(ipAddresses);
	}

	/**
	 * @param ipAddresses the addresses to match
	 */
	public void setIpAddresses(Set<String> ipAddresses) {
		this.ipAddresses = new HashSet<>();
		for (String address : ipAddresses) {
			addIpAddress(address);
		}
	}

	public void addIpAddress(String address) {
		if (address.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}/[0-9]{1,2}")) {
			// Address in CIDR notation
			try {
				for (String cidrAdd : (new SubnetUtils(address)).getInfo().getAllAddresses()) {
					this.ipAddresses.add(cidrAdd);
				}
			} catch (Exception e) {
				logger.severe("Invalid CIDR address specified: " + address);
			}
		} else {
			this.ipAddresses.add(address);
		}
	}

	@Override
	protected boolean evaluate(CrawlURI curi) {
		String hostAddress = getHostAddress(curi);
		return hostAddress != null && ipAddresses.contains(hostAddress.intern());
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
