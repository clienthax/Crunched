package moe.clienthax.crunched.subtitles;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static moe.clienthax.crunched.Utils.loadXMLFromString;

/**
 * Created by clienthax on 26/11/2017.
 */
class ASSSubtitleConverter {

    private final ArrayList<String> assLines = new ArrayList<>();

    ASSSubtitleConverter(File folder, String xmlString, SubtitleInfo subtitle) {
        Document doc = loadXMLFromString(xmlString);
        doc.getDocumentElement().normalize();

        handleScriptBlock(doc.getDocumentElement());

        NodeList childNodes = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            String nodeName = item.getNodeName();
            switch (nodeName) {
                case "styles":
                    handleStylesBlock((Element)item);
                    break;
                case "events":
                    handleEventsBlock((Element)item);
                    break;
                default:
                    System.out.println("unknown subtitle block "+nodeName);
                    break;
            }
            assLines.add("");
        }

        try {
            FileUtils.writeLines(new File(folder, subtitle.id+".ass"), assLines);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occured while attempting to write a subtitle file");
            System.exit(-2);
        }

    }

    private void handleScriptBlock(Element scriptBlock) {
        String assScriptBlock =
                "[Script Info]\n" +
                        "Title: "+scriptBlock.getAttribute("title")+"\n" +
                        "ScriptType: v4.00+\n" +
                        "WrapStyle: "+scriptBlock.getAttribute("wrap_style")+"\n" +
                        "PlayResX: "+scriptBlock.getAttribute("play_res_x")+"\n" +
                        "PlayResY: "+scriptBlock.getAttribute("play_res_y")+"\n" +
                        "SubtitleInfo ID: "+scriptBlock.getAttribute("id")+"\n" +
                        "Language: "+scriptBlock.getAttribute("lang_string")+"\n" +
                        "Created: "+scriptBlock.getAttribute("created")+"\n";
        assLines.add(assScriptBlock);

    }

    private void handleStylesBlock(Element stylesBlock) {
        assLines.add("[V4+ Styles]");
        String format = "Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding";
        assLines.add(format);

        NodeList styles = stylesBlock.getChildNodes();
        for (int i = 0; i < styles.getLength(); i++) {
            Element style = (Element) styles.item(i);
            String line = "Style: "+ style.getAttribute("name") + "," +
                    style.getAttribute("font_name") + "," +
                    style.getAttribute("font_size") + "," +
                    style.getAttribute("primary_colour") + "," +
                    style.getAttribute("secondary_colour") + "," +
                    style.getAttribute("outline_colour") + "," +
                    style.getAttribute("back_colour") + "," +
                    style.getAttribute("bold") + "," +
                    style.getAttribute("italic") + "," +
                    style.getAttribute("underline") + "," +
                    style.getAttribute("strikeout") + "," +
                    style.getAttribute("scale_x") + "," +
                    style.getAttribute("scale_y") + "," +
                    style.getAttribute("spacing") + "," +
                    style.getAttribute("angle") + "," +
                    style.getAttribute("border_style") + "," +
                    style.getAttribute("outline") + "," +
                    style.getAttribute("shadow") + "," +
                    style.getAttribute("alignment") + "," +
                    style.getAttribute("margin_l") + "," +
                    style.getAttribute("margin_r") + "," +
                    style.getAttribute("margin_v") + "," +
                    style.getAttribute("encoding");
            assLines.add(line);
        }
    }

    private void handleEventsBlock(Element eventsBlock) {
        assLines.add("[Events]");
        assLines.add("Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text");

        NodeList events = eventsBlock.getChildNodes();
        for (int i = 0; i < events.getLength(); i++) {
            Element event = (Element) events.item(i);
            String line = "Dialogue: 0," +
                    event.getAttribute("start") + "," +
                    event.getAttribute("end") + "," +
                    event.getAttribute("style") + "," +
                    event.getAttribute("name") + "," +
                    event.getAttribute("margin_l") + "," +
                    event.getAttribute("margin_r") + "," +
                    event.getAttribute("margin_v") + "," +
                    event.getAttribute("effect") + "," +
                    event.getAttribute("text");
            assLines.add(line);
        }
    }


}
