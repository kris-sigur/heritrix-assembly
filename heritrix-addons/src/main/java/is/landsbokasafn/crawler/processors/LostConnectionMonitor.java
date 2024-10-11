package is.landsbokasafn.crawler.processors;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.event.CrawlURIDispositionEvent;
import org.archive.crawler.framework.CrawlController;
import org.archive.modules.CrawlURI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class LostConnectionMonitor implements ApplicationListener<ApplicationEvent> {
    private long lastSuccess = -1;

    protected CrawlController controller;
    public CrawlController getCrawlController() {
        return this.controller;
    }
    @Autowired
    public void setCrawlController(CrawlController controller) {
        this.controller = controller;
    }
    
    private static final Logger logger = Logger.getLogger(LostConnectionMonitor.class.getName());
    
    long maxMinWithoutSuccess = 20;  
    public long getMaxMinWithoutSuccess() {
        return this.maxMinWithoutSuccess;
    }
    public void setMaxMinWithoutSuccess(long maxMinWithoutSuccess) {
        this.maxMinWithoutSuccess = maxMinWithoutSuccess;
    }
    
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof CrawlURIDispositionEvent) {
            CrawlURI curi = ((CrawlURIDispositionEvent)event).getCrawlURI();
            if (!curi.isHttpTransaction()) {
            	return; // Ignore DNS lookups that may succeed without 
                        // Internet connection if using local DNS cache
            }
	        if (lastSuccess==-1 || curi.getFetchStatus() > 0) {
	        	lastSuccess = System.currentTimeMillis();
	        }
	        if (lastSuccess + (maxMinWithoutSuccess*60*1000) < System.currentTimeMillis()) {
	            logger.log(Level.SEVERE, "Pausing due to likely connection failure. No success in - " + 
	            		maxMinWithoutSuccess + " minutes.");
	        	controller.requestCrawlPause();
	        }
		}
	}
}
