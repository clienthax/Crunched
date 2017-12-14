package moe.clienthax.crunched.subtitles;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static moe.clienthax.crunched.Utils.loadXMLFromString;
import static moe.clienthax.crunched.subtitles.SubtitleDecrypter.decryptAndConvertSubtitle;

/**
 * Created by clienthax on 27/11/2017.
 */
public class SubtitleUtils {

    public static List<SubtitleInfo> getSubtitles(HttpClient httpClient, File folder, int mediaid) {
        try {
            String subtitleListingUrl = "http://www.crunchyroll.com/xml/?req=RpcApiSubtitle_GetListing&media_id=" + mediaid;

            HttpPost request = new HttpPost(subtitleListingUrl);
            HttpResponse response = httpClient.execute(request);
            String subtitleListXML = EntityUtils.toString(response.getEntity(), "UTF-8");

//            String subtitleListXML = IOUtils.toString(new URL(subtitleListingUrl), Charset.defaultCharset());
            Document doc = loadXMLFromString(subtitleListXML);
            doc.getDocumentElement().normalize();

            ArrayList<SubtitleInfo> subtitles = new ArrayList<>();

            NodeList subtitleNode = doc.getElementsByTagName("subtitle");
            for (int i = 0; i < subtitleNode.getLength(); i++) {
                Element item = (Element) subtitleNode.item(i);
                int id = Integer.parseInt(item.getAttribute("id"));
                String subtitleUrl = item.getAttribute("link");
                String lang = item.getAttribute("title");
                SubtitleInfo sub = new SubtitleInfo(id, subtitleUrl, lang);
                decryptAndConvertSubtitle(folder, sub);
                subtitles.add(sub);
            }

            return subtitles;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error while getting subtitle list");
            System.exit(-2);
        }
        return null;
    }

    /***
     * Used for converting crunchyrolls title strings to valid lang codes
     */
   public static String getSubtitleCodeFromTitle(String title) {

       switch (title.hashCode()) {
           case -611717596://"[English (US)] English (US)":
               return "eng";

           case 76347396://"[Español (España)] Español (España)":
           case -923126152://"[Español] Español":
               return "es";

           case 80306080://"[Português (Brasil)] Português (Brasil)":
               return "por";

           case -1552869928://[العربية] العربية
               return "ara";

           case -2105392548://"[Italiano] Italiano":
               return "ita";

           case 950834328://"[Deutsch] Deutsch":
               return "ger";

           case 777410360://"[Français (France)] Français (France)":
               return "fra";

           case 1243937944://"[Русский] Русский":
               return "rus";

           default:
               System.out.println("Unknown subtitle code " + title);
               System.out.println(title.hashCode()+":"+title);
               break;

       }

       return "";

    }


}
