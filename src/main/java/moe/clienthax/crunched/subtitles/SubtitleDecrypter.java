package moe.clienthax.crunched.subtitles;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Base64;

import static moe.clienthax.crunched.Utils.decompressZlib;
import static moe.clienthax.crunched.Utils.loadXMLFromString;

/**
 * Created by clienthax on 26/11/2017.
 */
@SuppressWarnings("SameParameterValue")
class SubtitleDecrypter {

    //TODO make this use bouncy castle because javas aes impl is limited by default for dumb reasons..
    static void decryptAndConvertSubtitle(File folder, SubtitleInfo subtitle) {
        //Here be dragons...
        try {
            int base = (int) (Math.floor(Math.sqrt(6.9) * Math.pow(2, 25)));
            int eq1 = (int) (Math.floor((Math.sqrt(6.9) * Math.pow(2, 25)))) ^ subtitle.id;
            long magic = ((subtitle.id ^ base) ^ (subtitle.id ^ base) >> 3 ^ eq1 * 32) & 0x00000000ffffffffL;
            String secret = subtitleSecret(20, 97, 1, 2) + "" + magic;

            byte[] decryptionKeyShort = MessageDigest.getInstance("SHA-1").digest(secret.getBytes());
            byte[] decryptionKey = new byte[32];
            System.arraycopy(decryptionKeyShort, 0, decryptionKey, 0, decryptionKeyShort.length);//Pad the key to 32 Bytes

            String subtitleXML = IOUtils.toString(new URL(subtitle.link));
            Document doc = loadXMLFromString(subtitleXML);
            doc.getDocumentElement().normalize();

            NodeList subtitleNode = doc.getElementsByTagName("subtitle");
            Element element = (Element) subtitleNode.item(0);
            Element iv = (Element) element.getElementsByTagName("iv").item(0);
            Element data = (Element) element.getElementsByTagName("data").item(0);

            String ivString = iv.getFirstChild().getNodeValue();
            String dataString = data.getFirstChild().getNodeValue();

            byte[] ivBytes = Base64.getDecoder().decode(ivString);
            byte[] dataBytes = Base64.getDecoder().decode(dataString);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptionKey, "AES"), new IvParameterSpec(ivBytes));

            byte[] zlibData = cipher.doFinal(dataBytes);
            byte[] decompressed = decompressZlib(zlibData);

            String xml = new String(decompressed);
            convertSubtitleToASS(folder, xml, subtitle);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to decrypt subtitle, bailing out");
            System.exit(-2);
        }

    }

    private static String subtitleSecret(int size, int mod, int firstSeed, int secondSeed) {
        int currentValue = firstSeed + secondSeed;
        int previousValue = secondSeed;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < size; i++) {
            int oldValue = currentValue;
            result.append((char) (currentValue % mod + 33));
            currentValue += previousValue;
            previousValue = oldValue;
        }

        return result.toString();

    }

    private static void convertSubtitleToASS(File folder, String xml, SubtitleInfo subtitle) {
        new ASSSubtitleConverter(folder, xml, subtitle);
    }

}
