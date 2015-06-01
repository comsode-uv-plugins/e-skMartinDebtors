package eu.comsode.unifiedviews.plugins.extractor.skmartindebtors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dataunit.resource.Resource;
import eu.unifiedviews.helpers.dataunit.resource.ResourceHelpers;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.helpers.dpu.exec.AbstractDpu;

/**
 * Main data processing unit class.
 */
@DPU.AsExtractor
public class SkMartinDebtors extends AbstractDpu<SkMartinDebtorsConfig_V1> {

    private static final Logger LOG = LoggerFactory.getLogger(SkMartinDebtors.class);

    private static final String INPUT_URL = "http://egov.martin.sk/Default.aspx?NavigationState=806:0:";

    private static final String OUTPUT_FILE_NAME = "dlznici.csv";

    private static final String CSV_HEADER = "\"Dlžník\";\"Adresa dlžníka\";\"Mesto\";\"Typ dane\";\"Suma daòových nedoplatkov\";\"Mena\";\"Variabilný symbol\";\"Špecifický symbol\"";

    @DataUnit.AsOutput(name = "filesOutput")
    public WritableFilesDataUnit filesOutput;

    private static Element nhs = null;

    private static Element lf = null;

    private static Element pps = null;

    private static Element vs = null;

    private static Element vsg = null;

    private static Element ev = null;

    private static Element wmsca = null;

    private static Element ppgts = null;

    private String wmSavedSa = null;

    private Map<String, String> prsdrsp = null;

    private static String nextPageSM1 = "Portal1$part2117$up";

    private static String nextPageSM2 = "Portal1$part2117$grid$pageTbr";

    public SkMartinDebtors() {
        super(SkMartinDebtorsVaadinDialog.class, ConfigHistory.noHistory(SkMartinDebtorsConfig_V1.class));
    }

    @Override
    protected void innerExecute() throws DPUException {
        File outputFile = null;
        try {
            outputFile = File.createTempFile("____", "csv", new File(URI.create(filesOutput.getBaseFileURIString())));
        } catch (IOException | DataUnitException ex) {
            throw ContextUtils.dpuException(ctx, ex, "SkMartinDebtors.execute.exception");
        }

        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPORT);
        encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try (PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), encoder))); CloseableHttpClient httpclient = HttpClients.createDefault()) {
            outputWriter.println(CSV_HEADER);

            HttpGet httpGet = new HttpGet(INPUT_URL);
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            httpGet.setHeader("Accept-Encoding", "gzip, deflate");
            httpGet.setHeader("Accept-Language", "en-US,cs;q=0.7,en;q=0.3");
            httpGet.setHeader("Connection", "keep-alive");
            httpGet.setHeader("Host", (new URL(INPUT_URL)).getHost());
            httpGet.setHeader("Referer", INPUT_URL);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0");
            CloseableHttpResponse response1 = httpclient.execute(httpGet);

            LOG.debug(String.format("GET response status line: %s", response1.getStatusLine()));
            int responseCode = response1.getStatusLine().getStatusCode();
            StringBuilder headerSb = new StringBuilder();
            for (Header h : response1.getAllHeaders()) {
                headerSb.append("Key : " + h.getName() + " ,Value : " + h.getValue());
            }
            LOG.debug(headerSb.toString());

            Header[] cookies = response1.getHeaders("Set-Cookie");
            String[] cookieParts = cookies[0].getValue().split("; ");
            String sessionId = cookieParts[0];
            String response = null;
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.error("GET request not worked");
                throw new Exception("GET request not worked");
            }
            HttpEntity entity = null;
            try {
                entity = response1.getEntity();
                response = EntityUtils.toString(entity);
            } finally {
                EntityUtils.consumeQuietly(entity);
                response1.close();
            }
            do {
                LOG.debug(String.format("Server response:\n%s", response));
                Document doc = Jsoup.parse(response);

                Element content = doc.select("table#Portal1_part2117_grid_grdTable").first();
                Element body = content.select("tbody").first();
                Elements links = body.select("tr");
                for (Element link : links) {
                    Elements tds = link.select("td");
                    boolean detailTd = true;
                    List<String> mainInfo = new ArrayList<String>();
                    for (Element td : tds) {
                        if (detailTd) {
                            detailTd = false;
                            continue;
                        }
                        if (td != null && td.text() != null) {
                            mainInfo.add(td.text());
                        } else {
                            mainInfo.add("");
                        }
                    }
                    Debtor debtor = fillDebtorMainInfo(mainInfo);
                    Element onClick = link.select("div.FrmPodIcon").first();
                    String postBack = onClick.attr("onclick");
                    Pattern pattern = Pattern.compile("\\'.*?\\'");
                    Matcher matcher = pattern.matcher(postBack);

                    List<String> params = new ArrayList<String>();
                    while (matcher.find()) {
                        params.add(postBack.substring(matcher.start() + 1, matcher.end() - 1));
                    }
                    Map<String, String> detailPostParamsMap = prepareHttpPostParamsMap(doc);
                    detailPostParamsMap.put("ScriptManager1", params.get(0) + "|" + params.get(0));
                    detailPostParamsMap.put("__EVENTTARGET", params.get(0));
                    detailPostParamsMap.put("__EVENTARGUMENT", params.get(1));
                    String debtDetail = getDetailInfo(httpclient, sessionId, detailPostParamsMap);

                    prsdrsp = parseDetailListResponse(debtDetail);

                    Element debtDetailContent = Jsoup.parse(debtDetail);
                    wmSavedSa = debtDetailContent.select("input#WM_savedSA").first().attr("value");

                    Element veryDetailTable = debtDetailContent.select("table.detTbl").first();

                    debtor.addDetail(getDetails(veryDetailTable));
                    Element detailTable = debtDetailContent.select("table.grdTbl").first();

                    for (Element row : getListOfDebtDetailRows(detailTable)) {
                        Element detailHead1 = detailTable.select("th").first();
                        String detailPostBack = detailHead1.attr("sortHandler");
                        Matcher detailMatcher = pattern.matcher(detailPostBack);

                        List<String> detailPBParams = new ArrayList<String>();
                        while (detailMatcher.find()) {
                            detailPBParams.add(detailPostBack.substring(detailMatcher.start() + 1, detailMatcher.end() - 1));
                        }
                        Map<String, String> veryDetailPostParamsMap = new HashMap<String, String>();
                        veryDetailPostParamsMap.put("ScriptManager1", detailPBParams.get(0) + "|" + detailPBParams.get(0));
                        if (nhs != null) {
                            veryDetailPostParamsMap.put("NavigationHistoryState", nhs.attr("value"));
                        }
                        veryDetailPostParamsMap.put("WM$savedSA", wmSavedSa);
                        veryDetailPostParamsMap.put("__EVENTTARGET", detailPBParams.get(0));
                        veryDetailPostParamsMap.put("__EVENTARGUMENT", "1:1:" + row.attr("rId"));
                        veryDetailPostParamsMap.put("__VIEWSTATE", prsdrsp.get("__VIEWSTATE"));
                        veryDetailPostParamsMap.put("__VIEWSTATEGENERATOR", prsdrsp.get("__VIEWSTATEGENERATOR"));
                        veryDetailPostParamsMap.put("__EVENTVALIDATION", prsdrsp.get("__EVENTVALIDATION"));
                        veryDetailPostParamsMap.put("__PortalPageState", prsdrsp.get("__PortalPageState"));
                        veryDetailPostParamsMap.put("__ASYNCPOST", "true");
                        String debtVeryDetail = getDetailInfo(httpclient, sessionId, veryDetailPostParamsMap);
                        Element nextDebtDetailContent = Jsoup.parse(debtVeryDetail);
                        Element nextVeryDetailTable = nextDebtDetailContent.select("table.detTbl").first();
                        debtor.addDetail(getDetails(nextVeryDetailTable));
                    }
                    for (DebtDetail dd : debtor.getDetails()) {
                        outputWriter.println("\"" + debtor.getName() + "\";\"" + debtor.getAddress() + "\";\"" + debtor.getCity() + "\";\"" + dd.getDebtType() + "\";\"" + dd.getDebtSumForType().toString() + "\";\"" + dd.getCurrency() + "\";\"" + dd.getVariableSymbol() + "\";\""
                                + dd.getSpecificSymbol() + "\"");
                    }
                }
                Element pagingControl = doc.select("span.PagingControl").first();
                int activePage = Integer.parseInt(pagingControl.attr("actPage"));
                int maxPage = Integer.parseInt(pagingControl.attr("maxPage"));
                LOG.debug("Processed " + activePage + " of " + maxPage + " pages.");
                if (activePage == maxPage) {
                    break;
                }
                Map<String, String> nextPagePostParamsMap = new HashMap<String, String>();
                nextPagePostParamsMap.put("ScriptManager1", nextPageSM1 + "|" + nextPageSM2);
                if (nhs != null) {
                    nextPagePostParamsMap.put("NavigationHistoryState", nhs.attr("value"));
                }
                nextPagePostParamsMap.put("WM$savedSA", wmSavedSa);
                nextPagePostParamsMap.put("__EVENTTARGET", nextPageSM2);
                nextPagePostParamsMap.put("__EVENTARGUMENT", Integer.toString(activePage + 1));
                nextPagePostParamsMap.put("__VIEWSTATE", prsdrsp.get("__VIEWSTATE"));
                nextPagePostParamsMap.put("__VIEWSTATEGENERATOR", prsdrsp.get("__VIEWSTATEGENERATOR"));
                nextPagePostParamsMap.put("__EVENTVALIDATION", prsdrsp.get("__EVENTVALIDATION"));
                nextPagePostParamsMap.put("__PortalPageState", prsdrsp.get("__PortalPageState"));
                nextPagePostParamsMap.put("__ASYNCPOST", "true");
                response = getDetailInfo(httpclient, sessionId, nextPagePostParamsMap);

            } while (true);
            Resource resource = ResourceHelpers.getResource(filesOutput, OUTPUT_FILE_NAME);
            resource.setLast_modified(new Date());
            resource.setMimetype("text/csv");
            resource.setSize(outputFile.length());
            ResourceHelpers.setResource(filesOutput, OUTPUT_FILE_NAME, resource);
            filesOutput.updateExistingFileURI(OUTPUT_FILE_NAME, outputFile.toURI().toASCIIString());
        } catch (Exception ex) {
            throw ContextUtils.dpuException(ctx, ex, "SkMartinDebtors.execute.exception");
        }

    }

    private Debtor fillDebtorMainInfo(List<String> mainInfo) {
        Debtor debtor = new Debtor();
        debtor.setName(mainInfo.get(0));
        debtor.setAddress(mainInfo.get(1));
        debtor.setCity(mainInfo.get(2));
        debtor.setDebtsSum(normalizeSum(mainInfo.get(3)));
        debtor.setCurrency(mainInfo.get(4));
        return debtor;
    }

    private static Double normalizeSum(String sum) {
        String sumToConvert = sum.replaceAll(" ", "").replaceAll(",", ".").trim();
        Double result = null;
        try {
            result = Double.parseDouble(sumToConvert);
        } catch (NumberFormatException ex) {
            LOG.error(String.format("Problem converting string %s to Double.", sumToConvert), ex);
        }
        return result;
    }

    private Map<String, String> prepareHttpPostParamsMap(Element doc) {
        Map<String, String> postParams = new HashMap<String, String>();
        if (doc.select("input#NavigationHistoryState").first() != null) {
            nhs = doc.select("input#NavigationHistoryState").first();
        }
        if (nhs != null) {
            postParams.put("NavigationHistoryState", nhs.attr("value"));
        }
        if (doc.select("input#__LASTFOCUS").first() != null) {
            lf = doc.select("input#__LASTFOCUS").first();
        }
        if (lf != null) {
            postParams.put("__LASTFOCUS", lf.attr("value"));
        }
        if (doc.select("input#__PortalPageState").first() != null) {
            pps = doc.select("input#__PortalPageState").first();
        }
        if (pps != null) {
            postParams.put("__PortalPageState", pps.attr("value"));
        }
        if (doc.select("input#__VIEWSTATE").first() != null) {
            vs = doc.select("input#__VIEWSTATE").first();
        }
        if (vs != null) {
            postParams.put("__VIEWSTATE", vs.attr("value"));
        }
        if (doc.select("input#__VIEWSTATEGENERATOR").first() != null) {
            vsg = doc.select("input#__VIEWSTATEGENERATOR").first();
        }
        if (vsg != null) {
            postParams.put("__VIEWSTATEGENERATOR", vsg.attr("value"));
        }
        if (doc.select("input#__EVENTVALIDATION").first() != null) {
            ev = doc.select("input#__EVENTVALIDATION").first();
        }
        if (ev != null) {
            postParams.put("__EVENTVALIDATION", ev.attr("value"));
        }
        if (doc.select("input#WM_savedSA").first() != null) {
            wmsca = doc.select("input#WM_savedSA").first();
        }
        if (wmsca != null) {
            postParams.put("WM$savedSA", wmsca.attr("value"));
        }
        if (doc.select("input#Portal1_part2117_grid_txtSearch").first() != null) {
            ppgts = doc.select("input#Portal1_part2117_grid_txtSearch").first();
        }
        if (ppgts != null) {
            postParams.put("Portal1_part2117_grid_txtSearch", ppgts.attr("value"));
        }
        postParams.put("__ASYNCPOST", "true");
        return postParams;
    }

    private String getDetailInfo(CloseableHttpClient client, String sessionId, Map<String, String> postParams) throws IOException {
        HttpPost httpPost = new HttpPost(INPUT_URL);
        httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        httpPost.setHeader("Accept-Encoding", "gzip, deflate");
        httpPost.setHeader("Accept-Language", "en-US,cs;q=0.7,en;q=0.3");
        httpPost.setHeader("Cache-Control", "no-cache");
        httpPost.setHeader("Connection", "keep-alive");
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        httpPost.setHeader("Cookie", sessionId + "; ys-browserCheck=b%3A1");
        httpPost.setHeader("Host", (new URL(INPUT_URL)).getHost());
        httpPost.setHeader("Pragma", "no-cache");
        httpPost.setHeader("Referer", INPUT_URL);
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0");
        httpPost.setHeader("X-MicrosoftAjax", "Delta=true");
        httpPost.setHeader("X-Requested-With", "XMLHttpRequest");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> postParam : postParams.entrySet()) {
            nvps.add(new BasicNameValuePair(postParam.getKey(), postParam.getValue()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        String responseDoc = null;
        try (CloseableHttpResponse response2 = client.execute(httpPost)) {

            LOG.debug("POST Response Code :: " + response2.getStatusLine().getStatusCode());

            LOG.debug("Printing Response Header...\n");
            StringBuilder headerSb = new StringBuilder();
            for (Header h : response2.getAllHeaders()) {
                headerSb.append("Key : " + h.getName() + " ,Value : " + h.getValue());
            }
            LOG.debug(headerSb.toString());
            HttpEntity entity = null;
            try {
                entity = response2.getEntity();
                if (response2.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) { //success
                    responseDoc = EntityUtils.toString(entity);
                } else {
                    LOG.error("POST request not worked");
                }
            } finally {
                EntityUtils.consumeQuietly(entity);
            }
        }
        return responseDoc;
    }

    private Map<String, String> parseDetailListResponse(String resp) {
        Pattern pattern = Pattern.compile("[0-9]+\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|");
        Matcher matcher = pattern.matcher(resp);

        Map<String, String> params = new HashMap<String, String>();
        while (matcher.find()) {
            String line = resp.substring(matcher.start(), matcher.end());
            List<String> values = new ArrayList<String>();
            while (line.contains("|")) {
                values.add(line.substring(0, line.indexOf("|")));
                line = line.substring(line.indexOf("|") + 1);
            }
            if (values.size() == 4 && values.get(1).equals("hiddenField")) {
                params.put(values.get(2), values.get(3));
            }
        }
        return params;
    }

    private DebtDetail getDetails(Element detailTable) {
        DebtDetail dd = new DebtDetail();
        List<String> details = new ArrayList<String>();
        Elements trs = detailTable.select("tr");
        for (Element tr : trs) {
            Element spanValue = tr.select("span.detText").first();
            if (spanValue != null) {
                details.add(spanValue.text());
            } else {
                details.add("");
            }
        }
        dd.setDebtType(details.get(1));
        dd.setDebtSumForType(normalizeSum(details.get(2)));
        dd.setCurrency(details.get(3));
        dd.setVariableSymbol(details.get(4));
        dd.setSpecificSymbol(details.get(5));
        return dd;
    }

    private Elements getListOfDebtDetailRows(Element debtDetailListTble) {
        Element detailTableBody = debtDetailListTble.select("tbody").first();
        Elements detailRows = detailTableBody.select("tr");
        detailRows.remove(0);
        return detailRows;
    }
}
