package eu.comsode.unifiedviews.plugins.extractor.skmartindebtors;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;

import cz.cuni.mff.xrg.odcs.dpu.test.TestEnvironment;
import eu.unifiedviews.dataunit.files.FilesDataUnit;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.helpers.dataunit.files.FilesHelper;
import eu.unifiedviews.helpers.dpu.test.config.ConfigurationBuilder;

public class SkMartinDebtorsTest {
    @Test
    public void testDebtorsCount() throws Exception {
        SkMartinDebtorsConfig_V1 config = new SkMartinDebtorsConfig_V1();

        // Prepare DPU.
        SkMartinDebtors dpu = new SkMartinDebtors();
        dpu.configure((new ConfigurationBuilder()).setDpuConfiguration(config).toString());

        // Prepare test environment.
        TestEnvironment environment = new TestEnvironment();

        // Prepare data unit.
        WritableFilesDataUnit filesOutput = environment.createFilesOutput("filesOutput");
        try {
            // Run.
            environment.run(dpu);

            // Get file iterator.
            Map<String, FilesDataUnit.Entry> outputFiles = FilesHelper.getFilesMap(filesOutput);

            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(SkMartinDebtors.INPUT_URL);
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            httpGet.setHeader("Accept-Encoding", "gzip, deflate");
            httpGet.setHeader("Accept-Language", "en-US,cs;q=0.7,en;q=0.3");
            httpGet.setHeader("Connection", "keep-alive");
            httpGet.setHeader("Host", (new URL(SkMartinDebtors.INPUT_URL)).getHost());
            httpGet.setHeader("Referer", SkMartinDebtors.INPUT_URL);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0");
            CloseableHttpResponse response1 = httpclient.execute(httpGet);
            HttpEntity entity = null;
            String response = null;
            try {
                entity = response1.getEntity();
                response = EntityUtils.toString(entity);
            } finally {
                EntityUtils.consumeQuietly(entity);
                response1.close();
                httpclient.close();
            }
            Document doc = Jsoup.parse(response);
            Element span = doc.select("span.PagingControl").first();
            Integer debtorsCount = Integer.decode(span.attr("countRec"));

            FilesDataUnit.Entry entry = outputFiles.get("dlznici.csv");
            URI uri = new URI(entry.getFileURIString());
            File f = new File(uri);
            Path path = f.toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Set<String> dlznici = new HashSet<String>();
            for (String line : lines) {
                dlznici.add(line.substring(0, line.indexOf(';')));
            }
            Assert.assertEquals(debtorsCount, new Integer(dlznici.size() - 1));//decrement csv header
        } finally {
            // Release resources.
            environment.release();
        }
    }
}
