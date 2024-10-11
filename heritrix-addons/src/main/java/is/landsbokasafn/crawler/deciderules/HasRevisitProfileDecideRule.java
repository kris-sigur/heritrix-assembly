package is.landsbokasafn.crawler.deciderules;

import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.PredicatedDecideRule;

@SuppressWarnings("serial")
public class HasRevisitProfileDecideRule extends PredicatedDecideRule {

	@Override
	protected boolean evaluate(CrawlURI curi) {
		return curi.isRevisit();
	}

}
