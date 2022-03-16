package wifip2p.wifi.com.wifip2p.activity;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import wifip2p.wifi.com.wifip2p.R;

public class VideoTestActivity extends Activity {


//    http://rbv01.ku6.com/omtSn0z_PTREtneb3GRtGg.mp4
//    http://rbv01.ku6.com/7lut5JlEO-v6a8K3X9xBNg.mp4
//    https://key003.ku6.com/movie/1af61f05352547bc8468a40ba2d29a1d.mp4
//    https://key002.ku6.com/xy/d7b3278e106341908664638ac5e92802.mp4
    String path = "https://www.koi2000.top/api/video/286198582171779073.mp4";
    String uri2 = "http://rbv01.ku6.com/omtSn0z_PTREtneb3GRtGg.mp4";
    public final String videoUrl = "http://rbv01.ku6.com/omtSn0z_PTREtneb3GRtGg.mp4";

    VideoView videoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);

        init();
    }

    private void init() {
        videoView = (VideoView) findViewById(R.id.video);
        videoView.setVideoPath(path);
        videoView.setMediaController(new MediaController(this));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.start();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
            }
        });

    }


}