package is.landsbokasafn.crawler.processors;

import static is.landsbokasafn.crawler.prefetch.LbsFrontierPreparer.LBS_QUEUE_NAME_EXTRA_KEY;

import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;

import is.landsbokasafn.crawler.prefetch.LbsFrontierPreparer;

/**
 * The queue name extra info gets wiped out on retries. Re-add it if missing.
 * 
 * @see LbsFrontierPreparer
 */
public class LbsQueueNameLoggerProcessor extends Processor {

	@Override
	protected boolean shouldProcess(CrawlURI curi) {
		return !curi.getExtraInfo().has(LBS_QUEUE_NAME_EXTRA_KEY);
	}

	@Override
	protected void innerProcess(CrawlURI curi) throws InterruptedException {
		curi.getExtraInfo().putOnce(LBS_QUEUE_NAME_EXTRA_KEY, curi.getClassKey());

	}

}
