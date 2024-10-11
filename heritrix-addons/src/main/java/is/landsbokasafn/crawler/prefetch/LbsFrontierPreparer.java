package is.landsbokasafn.crawler.prefetch;

import org.archive.crawler.prefetch.FrontierPreparer;
import org.archive.modules.CrawlURI;

/**
 * Lbs custom frontier preparer adds the name of the frontier queue (class key) that the CrawlURI is assigned to
 * to the JSON extra info that is written to the end of each line of the crawl log (if extra info is enabled in 
 * config).
 * 
 * This is merely to facilitate crawl-time analysis of the crawl log by making it easy to associate lines in the log
 * with specific queues in the frontier report.
 *
 */
public class LbsFrontierPreparer extends FrontierPreparer {
	public static final String LBS_QUEUE_NAME_EXTRA_KEY = "queueName";

	@Override
	public void prepare(CrawlURI curi) {
		super.prepare(curi);
		curi.getExtraInfo().putOnce(LBS_QUEUE_NAME_EXTRA_KEY, curi.getClassKey());
	}


}
