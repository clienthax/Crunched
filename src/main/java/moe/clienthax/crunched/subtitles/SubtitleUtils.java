package moe.clienthax.crunched.subtitles;

import org.apache.commons.io.IOUtils;
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

    public static List<SubtitleInfo> getSubtitles(File folder, int mediaid) {
        try {
            String subtitleListingUrl = "http://www.crunchyroll.com/xml/?req=RpcApiSubtitle_GetListing&media_id=" + mediaid;
            String subtitleListXML = IOUtils.toString(new URL(subtitleListingUrl), Charset.defaultCharset());
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
        switch (title) {
            case "[English (US)] English (US)":
                return "eng";

            case "[Español (España)] Español (España)":
            case "[Español] Español":
                return "es";

            case "[Português (Brasil)] Português (Brasil)":
                return "por";

            case "[العربية] العربية":
                return "ara";

            case "[Italiano] Italiano":
                return "ita";

            case "[Deutsch] Deutsch":
                return "ger";

            case "[Français (France)] Français (France)":
                return "fra";

            default:
                System.out.println("Unknown subtitle code "+title);
                break;

        }
        return "";
    }


}
