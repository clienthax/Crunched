package moe.clienthax.crunched;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import moe.clienthax.crunched.data.EpisodeInfo;
import moe.clienthax.crunched.subtitles.SubtitleInfo;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.apache.commons.cli.*;
import org.apache.commons.exec.*;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static moe.clienthax.crunched.subtitles.SubtitleUtils.getSubtitleCodeFromTitle;
import static moe.clienthax.crunched.subtitles.SubtitleUtils.getSubtitles;

/**
 * Created by clienthax on 25/11/2017.
 */
@SuppressWarnings("unchecked")
public class Crunched {

    private String session;
    private String email = "";
    private String password = "";
    private String locale = "enUS";//jaJP / enUS
    private String seriesPage = "";//http://www.crunchyroll.com/the-ancient-magus-bride";
    private String animeName = "";
    private final JSONParser parser = new JSONParser();

    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private String mkvmergePath = "mkvmerge";

    private org.apache.commons.cli.CommandLine cmd = null;


    public static void main(String[] args) {
        new Crunched(args);
    }

    //TODO config file
    //TODO allow cred saving
    //TODO check for missing exes
    //TODO Proxy support for getting the session id
    //TODO mkvmerge, set audio lang to match locale

    //TODO retry a few times if ffmpeg dies die to a url error or something dumb ?

    private void parseArgs(String[] args) {
        HelpFormatter helpFormatter = new HelpFormatter();

        Options options = new Options();
        options.addRequiredOption("email", "email", true, "email");
        options.addRequiredOption("pass", "pass", true, "password");
        options.addRequiredOption("page", "page", true, "Crunchyroll series page");
        options.addOption("ffmpeg", true, "Path to ffmpeg");
        options.addOption("ffprobe", true, "Path to ffprobe");
        options.addOption("mkvmerge", true, "Path to mkvmerge");

        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Missing required arguments");
            helpFormatter.printHelp("Crunched", options);
            System.exit(-2);
        }

        email = cmd.getOptionValue("email");
        password = cmd.getOptionValue("pass");
        seriesPage = cmd.getOptionValue("page");

    }


    private Crunched(String[] args) {
        parseArgs(args);
        checkForPrograms();

        String device_id = "de";
        String device_type = "com.crunchyroll.iphone";//IPhone type allows us to get 1080 quality
        String access_token = "QWjz212GspMHH9h";
        String sessionUrl = "http://api.crunchyroll.com/start_session.0.json?device_id="+device_id+"&device_type="+device_type+"&access_token="+access_token;

        try {
            //Get session key TODO allow for override?
            String sessionJson = IOUtils.toString(new URL(sessionUrl), Charset.defaultCharset());
            JSONObject sessionJsonObj = (JSONObject) parser.parse(sessionJson);
            session = (String) ((JSONObject) sessionJsonObj.get("data")).get("session_id");
            System.out.println("session = " + session);

            //Do login.
            String loginUrl = "https://api.crunchyroll.com/login.0.json?session_id=" + session + "&account=" + email + "&password=" + password + "&fields=user.username,user.premium,user.email&locale=" + locale;
            String loginJson = IOUtils.toString(new URL(loginUrl), Charset.defaultCharset());
            JSONObject loginJsonObj = (JSONObject) parser.parse(loginJson);

            //Check for login problems
            boolean errorCheck = (boolean) loginJsonObj.get("error");
            if (errorCheck) {
                System.out.println("Error logging in");
                System.out.println(loginJsonObj.get("message"));
                System.exit(-2);
            }

            String premiumTypes = (String) ((JSONObject) ((JSONObject) loginJsonObj.get("data")).get("user")).get("premium");
            if (premiumTypes.isEmpty() || !premiumTypes.contains("anime")) {
                System.out.println("Specified account is not premium, please use a premium account");
                System.exit(-2);
            }

            //premium check
            //data.user.premium	"anime|drama|manga" vs blank ""

            String authKey = (String) ((JSONObject) loginJsonObj.get("data")).get("auth");
            System.out.println("auth key = " + authKey);

            //Get mediaid (series_id)
            animeName = seriesPage.substring(seriesPage.lastIndexOf("/") + 1).replace("-", " ");
            animeName = WordUtils.capitalize(animeName);
            String seriesId = getMediaIdFromSeriesPage(seriesPage);

            LinkedHashMap<Integer, EpisodeInfo> episodes = buildEpisodeMap(seriesId);
            downloadEpisodes(episodes);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-2);
        }

    }

    private void checkForPrograms() {
        if(Utils.isWindows()) {
            ffmpegPath += ".exe";
            ffprobePath += ".exe";
            mkvmergePath += ".exe";
        }
        if(cmd.hasOption("ffmpeg")) {
            ffmpegPath = cmd.getOptionValue("ffmpeg");
        }
        if(cmd.hasOption("ffprobe")) {
            ffprobePath = cmd.getOptionValue("ffprobe");
        }
        if(cmd.hasOption("mkvmerge")) {
            mkvmergePath = cmd.getOptionValue("mkvmerge");
        }

        boolean ffmpegFound = new File(ffmpegPath).exists();
        boolean ffprobeFound = new File(ffprobePath).exists();
        boolean mkvmergeFound = new File(mkvmergePath).exists();

        if(!ffmpegFound || !ffprobeFound || !mkvmergeFound) {
            System.out.println("Required program not found!");
            System.out.println("Please specify location with -ffmpeg -ffprobe -mkvmerge, or place in same folder as program");
            System.out.println("ffmpeg "+ (ffmpegFound ? "Found" : "Not found"));
            System.out.println("ffprobe "+ (ffprobeFound ? "Found" : "Not found"));
            System.out.println("mkvmerge "+ (mkvmergeFound ? "Found" : "Not found"));
            System.exit(-2);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void downloadEpisodes(LinkedHashMap<Integer, EpisodeInfo> episodes) {


        for (Map.Entry<Integer, EpisodeInfo> episode : episodes.entrySet()) {
            EpisodeInfo episodeInfo = episode.getValue();
            File folder = new File(".", animeName);
            folder.mkdirs();

            System.out.println("Downloading "+animeName+" S"+episodeInfo.season+" ep"+episodeInfo.episode_number);

            String fileNameWithoutSuffix;
            //Stupid name corner case 4.5
            if(episodeInfo.episode_number.contains(".")) {
                int endIndex = episodeInfo.episode_number.indexOf(".");
                String part1 = episodeInfo.episode_number.substring(0, endIndex);
                String part2 = episodeInfo.episode_number.substring(endIndex+1);
                fileNameWithoutSuffix = animeName + " - s" + String.format("%02d", episodeInfo.season) + "e" + String.format("%02d", Integer.parseInt(part1))+"."+part2 + " - [CrunchyRoll]";
            } else {
                fileNameWithoutSuffix = animeName + " - s" + String.format("%02d", episodeInfo.season) + "e" + String.format("%02d", Integer.parseInt(episodeInfo.episode_number)) + " - [CrunchyRoll]";
            }
            String streamUrl = getHighestQualityVideoStream(episodeInfo.media_id);
            boolean skipped = !downloadStreamToMkv(folder, fileNameWithoutSuffix, streamUrl);
            if(skipped) {
                System.out.println("skipping as already exists");
                continue;
            }

            List<SubtitleInfo> subtitles = getSubtitles(folder, episodeInfo.media_id);

            //merge subtitles into mkv

            try {
                CommandLine cmd = new CommandLine(mkvmergePath);
                cmd.addArgument("-o");
                cmd.addArgument(new File(folder, fileNameWithoutSuffix + ".mkv").getAbsolutePath());
                cmd.addArgument(new File(folder, fileNameWithoutSuffix + "_nosub.mkv").getAbsolutePath());

                for (SubtitleInfo subtitle : subtitles) {
                    cmd.addArgument("--language");
                    cmd.addArgument("0:" + getSubtitleCodeFromTitle(subtitle.title));
                    cmd.addArgument(new File(folder, subtitle.id + ".ass").getAbsolutePath());
                }

                DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

                Executor executor = new DefaultExecutor();
                ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
                executor.setStreamHandler(new PumpStreamHandler(os));
                executor.setWorkingDirectory(new File("."));
                executor.execute(cmd, resultHandler);

                while (!resultHandler.hasResult()) {
                    resultHandler.waitFor();
                }
                //0 is good for exit value

                //cleanup
                for (SubtitleInfo subtitle : subtitles) {
                    new File(folder, subtitle.id + ".ass").delete();
                }
                new File(folder, fileNameWithoutSuffix + "_nosub.mkv").delete();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("An error occured while running mkvmerge");
                System.exit(-2);
            }


        }
    }

    /*
    Need to build up a map of seasons(from collection id), episodes, and media ids
     */
    private LinkedHashMap<Integer, EpisodeInfo> buildEpisodeMap(String seriesId) {
        String seriesJsonUrl = "http://api.crunchyroll.com/list_media.0.json?session_id="+session+"&media_type=anime&series_id="+seriesId+"&limit=1000";
        //System.out.println(seriesJsonUrl);
        //has a list of data for each ep, 0 indexed
        //get season from collection ids listed to figure out mappings

        //media_id, epinfo
        LinkedHashMap<Integer, EpisodeInfo> episodes = new LinkedHashMap<>();
        HashMap<Integer, Integer> collectionToSeason = new HashMap<>();

        try {
            String seriesJson = IOUtils.toString(new URL(seriesJsonUrl), Charset.defaultCharset());
            JSONObject seriesJsonObj = (JSONObject) parser.parse(seriesJson);
            JSONArray episodeListObj = (JSONArray) seriesJsonObj.get("data");
            for (JSONObject epObj : (ArrayList<JSONObject>) episodeListObj) {
                try {
                    int media_id = Integer.parseInt((String) epObj.get("media_id"));
                    int series_id = Integer.parseInt((String) epObj.get("series_id"));
                    String episode_number = (String) epObj.get("episode_number");
                    int collection_id = Integer.parseInt((String) epObj.get("collection_id"));

                    //Skip trailers
                    if(episode_number.isEmpty())
                        continue;

                    EpisodeInfo episodeInfo = new EpisodeInfo(media_id, series_id, collection_id, episode_number);
                    if (!collectionToSeason.containsKey(collection_id)) {
                        //Get season id from api
                        String collectionJsonUrl = "http://api.crunchyroll.com/info.0.json?session_id=" + session + "&media_type=anime&collection_id=" + collection_id;
                        String collectionJson = IOUtils.toString(new URL(collectionJsonUrl), Charset.defaultCharset());
                        JSONObject collectionJsonObj = (JSONObject) parser.parse(collectionJson);

                        int season = Integer.parseInt((String) (((JSONObject) collectionJsonObj.get("data")).get("season")));
                        collectionToSeason.put(collection_id, season);
                    }
                    episodeInfo.season = collectionToSeason.get(collection_id);
                    episodes.put(media_id, episodeInfo);
                } catch (Exception e) {
                    //Some trailers etc have a 0 as episode number...
                    //System.out.println("Skipping media item");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to build episode map");
        }

        return episodes;
    }

    private boolean downloadStreamToMkv(File folder, String fileNameWithoutSuffix, String streamUrl) {

        try {
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);

            //Check if completed file exists first
            File mkvFile = new File(folder, fileNameWithoutSuffix + ".mkv");
            if (mkvFile.exists())
                return false;

            mkvFile = new File(folder, fileNameWithoutSuffix + "_nosub.mkv");

            FFmpegBuilder fFmpegBuilder = new FFmpegBuilder()
                    .setInput(streamUrl)
                    .addOutput(mkvFile.getAbsolutePath())
                    .setVideoCodec("copy")
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            FFmpegProbeResult in = ffprobe.probe(streamUrl);

            FFmpegJob job = executor.createJob(fFmpegBuilder, new ProgressListener() {

                // Using the FFmpegProbeResult determine the duration of the input
                final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);
                final ProgressBar progressBar = new ProgressBar("Downloading", 100, ProgressBarStyle.ASCII).start();

                @Override
                public void progress(Progress progress) {
                    double percentage = progress.out_time_ns / duration_ns;
                    int sanepercentage = (int) (percentage * 100);
                    progressBar.stepTo(sanepercentage);//Works in console, not soo much in ide
                    if (sanepercentage == 100)
                        progressBar.stop();
                }
            });
            job.run();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("An error occured while downloading the stream");
            System.exit(-2);
        }
        return true;
    }


    private String getHighestQualityVideoStream(int mediaid) {
        String mediaInfoUrl = "https://api.crunchyroll.com/info.0.json?session_id=" + session + "&media_id=" + mediaid + "&locale=" + locale + "&fields=media.media_id,media.stream_data,media.premium_available,media.free_available,media.series_id,media.collection_id,media.media_type,%20media.series_name,media.name,media.duration,media.name,media.description,media.episode_number,media.playhead,media.fwide_url,media.screenshot_image,media.url,media.bif_url,media.collection_name,series.genres\n";

        try {
            //Need to match the locale to the audio lang to get a non hardsub stream.

            String mediaInfoJson = IOUtils.toString(new URL(mediaInfoUrl), Charset.defaultCharset());
            JSONObject mediaInfoJsonObj = (JSONObject) parser.parse(mediaInfoJson);

            String audioLang = (String) ((JSONObject) ((JSONObject) mediaInfoJsonObj.get("data")).get("stream_data")).get("audio_lang");
            if (!audioLang.equalsIgnoreCase(locale)) {
                //Request again to get clean video
                mediaInfoUrl = "https://api.crunchyroll.com/info.0.json?session_id=" + session + "&media_id=" + mediaid + "&locale=" + audioLang + "&fields=media.media_id,media.stream_data,media.premium_available,media.free_available,media.series_id,media.collection_id,media.media_type,%20media.series_name,media.name,media.duration,media.name,media.description,media.episode_number,media.playhead,media.fwide_url,media.screenshot_image,media.url,media.bif_url,media.collection_name,series.genres\n";
                mediaInfoJson = IOUtils.toString(new URL(mediaInfoUrl), Charset.defaultCharset());
                mediaInfoJsonObj = (JSONObject) parser.parse(mediaInfoJson);
            }

            //System.out.println(mediaInfoUrl);

            JSONArray streamList = (JSONArray) ((JSONObject) ((JSONObject) mediaInfoJsonObj.get("data")).get("stream_data")).get("streams");
            //Best stream is normally the one right at the end of the list.
            //However if no hardsubs are present (or atleast listed like that in the api!) then the media list is wrong and includes all hardsubs , but the first ultra will be the clean video
            JSONObject bestStream = null;
            for (JSONObject stream : (List<JSONObject>) streamList) {
                bestStream = stream;
                //TODO check for mid/low in order etc before ultra
                String quality = (String) stream.get("quality");
                //Api sometimes has null... ?
                if (quality != null && quality.equalsIgnoreCase("ultra"))
                    break;
            }
            return (String) bestStream.get("url");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("An error occured while attempting to get the video stream "+mediaInfoUrl);
            System.exit(-2);
        }
        return "";
    }

    /**
     * This method is going to get very hacky... very quickly.
     * TODO use the api search functionality
     * @param seriesUrl
     * @return
     */
    private String getMediaIdFromSeriesPage(String seriesUrl) {
        try {
            String seriesPageString = IOUtils.toString(new URL(seriesUrl), Charset.defaultCharset());
            String toHunt = "mediaId\":\"";
            int mediaIdIndex = seriesPageString.indexOf(toHunt) + toHunt.length();
            String substring = seriesPageString.substring(mediaIdIndex);
            String attempt = substring.substring(0, substring.indexOf("\"")).replace("SRZ.", "");
            try {
                Integer.parseInt(attempt);
            } catch (NumberFormatException e) {
                seriesPageString = IOUtils.toString(new URL(seriesUrl), Charset.defaultCharset());
                toHunt = "<div class=\"show-actions\" group_id=\"";
                mediaIdIndex = seriesPageString.indexOf(toHunt) + toHunt.length();
                substring = seriesPageString.substring(mediaIdIndex);
                attempt = substring.substring(0, substring.indexOf("\"")).replace("SRZ.", "");
            }
            return attempt;
        } catch (Exception e) {
            System.out.println("Couldn't find media ID on series page "+seriesUrl);
            System.out.println("Is this page in the correct format? eg, http://www.crunchyroll.com/rwby");
        }
        System.exit(-2);
        return "";
    }


}
