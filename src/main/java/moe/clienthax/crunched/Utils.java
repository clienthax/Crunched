package moe.clienthax.crunched;

import org.apache.commons.lang3.SystemUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * Created by clienthax on 27/11/2017.
 */
public class Utils {

    public static Document loadXMLFromString(String xml)
    {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            return builder.parse(is);
        } catch (Exception e) {
            System.out.println("An error occured while parsing the xml "+xml);
            System.exit(-2);
        }
        return null;
    }

    public static byte[] decompressZlib(byte[] input) {
        try {
            Inflater decompress = new Inflater();
            decompress.setInput(input);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(byteArrayOutputStream, decompress);
            inflaterOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            System.out.println("An error occured while decompressing data");
            System.exit(-2);
        }
        return null;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static String sanitizeName( String name ) {
        if( null == name ) {
            return "";
        }

        if( SystemUtils.IS_OS_LINUX ) {
            return name.replaceAll( "/+", "" ).trim();
        }

        return name.replaceAll( "[\u0001-\u001f<>:\"/\\\\|?*\u007f]+", "" ).trim();
    }

}
