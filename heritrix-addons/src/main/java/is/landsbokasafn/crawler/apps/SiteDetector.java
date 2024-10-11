package is.landsbokasafn.crawler.apps;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.archive.util.SURT;

public class SiteDetector {
	
	private static final RequestConfig requestConfig = RequestConfig
			.custom()
			.setConnectTimeout(200)
			.setSocketTimeout(200)
			.build();
	private static final HttpClient client = HttpClientBuilder
			.create()
			.disableAutomaticRetries()
			.setDefaultRequestConfig(requestConfig)
			.build();

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Expects seed file location as only argument");
		}
		
		Path seedlist = Path.of(args[0]);

		if (!Files.exists(seedlist)) {
			System.out.println("File not found: " + seedlist);
		}

		
		Path output = seedlist.getParent().resolve("surt-prefix-wix.txt");

		try (var o = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			int i = 0;
			var urls = Files.readAllLines(seedlist);
			for (String url : urls) {
				i++;
				System.out.print("\r" + i + "/" + urls.size() + " -- " + url + "                                      ");
				if (!url.startsWith("http://") && !url.startsWith("https://")) {
					url = "http://" + url;
				}

				if (isWix(url)) {
					String surt = SURT.prefixFromPlain(url);
					if (surt.endsWith("www,")) {
						surt = surt.substring(0, surt.length()-4);
					}
					o.write(surt + "\n");
					o.flush();
				}
			}
			System.out.println("Finished processing " + urls.size() + " seeds");
		}
	}

	public static boolean isWix(String url) {
		try {
			HttpHead head = new HttpHead(url);
			var response = client.execute(head);
			return response.getFirstHeader("X-Wix-Request-Id") != null;
		} catch (Exception e) {

		}
		return false;
	}
}
