package moe.clienthax.crunched.data;

/**
 * Created by clienthax on 26/11/2017.
 */
public class EpisodeInfo {
    public final int media_id;
    private final int series_id;
    private final int collection_id;
    public final int episode_number;

    public int season = 0;

    public EpisodeInfo(int media_id, int series_id, int collection_id, int episode_number) {
        this.media_id = media_id;
        this.series_id = series_id;
        this.collection_id = collection_id;
        this.episode_number = episode_number;
    }



}
