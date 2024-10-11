package is.landsbokasafn.crawler.frontier;

import junit.framework.TestCase;

public class HostnameQueueAssignmentPolicyWithLimitsTest extends TestCase {

	public void testGetLimitedHostname(){
		HostnameQueueAssignmentPolicyWithLimits policy = new HostnameQueueAssignmentPolicyWithLimits();
		
		// Test no limit
		assertEquals("example.com", policy.getLimitedHostname("example.com", -1));
		assertEquals("a.b.c.d.e.f.g.example.com", policy.getLimitedHostname("a.b.c.d.e.f.g.example.com", -1));
		assertEquals("a.b.c.d.e.f.g.example.com#443", policy.getLimitedHostname("a.b.c.d.e.f.g.example.com#443", -1));

		// Test limit to two
		assertEquals("example.com", policy.getLimitedHostname("example.com", 2));
		assertEquals("example.com", policy.getLimitedHostname("www.example.com", 2));
		assertEquals("example.com", policy.getLimitedHostname("subdomain.example.com", 2));
		assertEquals("example.com", policy.getLimitedHostname("www.subdomain.example.com", 2));
		
		// Test limit to two on HTTPS port
		assertEquals("example.com#443", policy.getLimitedHostname("example.com#443", 2));
		assertEquals("example.com#443", policy.getLimitedHostname("www.example.com#443", 2));
		assertEquals("example.com#443", policy.getLimitedHostname("subdomain.example.com#443", 2));
		assertEquals("example.com#443", policy.getLimitedHostname("www.subdomain.example.com#443", 2));
		
		// Test limit two 3
		assertEquals("subdomain.example.com", policy.getLimitedHostname("subdomain.example.com", 3));
		assertEquals("subdomain.example.com", policy.getLimitedHostname("www.subdomain.example.com", 3));
		assertEquals("subdomain.example.com", policy.getLimitedHostname("another.www.subdomain.example.com", 3));
		
	}
}
