package com.minimaldev.android.exoplayerapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.webkit.DownloadListener;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.*;
import com.google.android.exoplayer2.upstream.cache.CacheUtil;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    Uri uri;
    private final DefaultBandwidthMeter defaultBandwidthMeter=new DefaultBandwidthMeter();

    SharedPreferences sharedPreferences;
    private SimpleExoPlayer player;
    private SimpleExoPlayerView playerView;
    private ComponentListener componentListener;
    String s="";
    private long playbackPosition;
    private int currentWindow;
    long medialength;
    private boolean playwhenReady=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            SharedPreferences preferences = this.getSharedPreferences("download", MODE_PRIVATE);
            s=preferences.getString("down","no");
        }
        catch (Exception e)
        {
            Log.e("Error",e.getMessage());
        }

        componentListener=new ComponentListener();
        playerView=(SimpleExoPlayerView) findViewById(R.id.exoplayer);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        initializeplayer();

    }

    @Override
    public void onResume()
    {
        super.onResume();
        hidesystemui();
        if(player != null) {

            player.seekTo(medialength);
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(player!=null)
        {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
            medialength=player.getCurrentPosition();
        }

    }

    @Override
    public void onStop()
    {
        super.onStop();
        if(Util.SDK_INT>23)
            releaseplayer();

    }

    private void initializeplayer()
    {
        if(player== null)
        {
            TrackSelection.Factory  factory=new AdaptiveTrackSelection.Factory(defaultBandwidthMeter);

            player= ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), new DefaultTrackSelector(factory), new DefaultLoadControl());
            player.addListener(componentListener);
            player.setVideoDebugListener(componentListener);
            player.setAudioDebugListener(componentListener);
            playerView.setPlayer(player);
            player.seekTo(currentWindow, playbackPosition);

            uri=Uri.parse(getString(R.string.media_url_mp4));
            DataSource.Factory data=new DefaultDataSourceFactory(this,Util.getUserAgent(this,"ExoPlayerApp"),defaultBandwidthMeter);
            if(s.equals("yes"))
            {
                File c=null;
                if(!this.getCacheDir().exists()) {
                    c.mkdir();
                    System.out.println("Created");
                }
                Cache cache = new SimpleCache(this.getCacheDir(), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 10));
                CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(cache, data, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

                DefaultExtractorsFactory allocator=new DefaultExtractorsFactory();
                MediaSource media=new ExtractorMediaSource(uri,cacheDataSourceFactory,allocator,null,null);
                player.prepare(media,true,false);
            }
            else
            {
                DefaultExtractorsFactory allocator=new DefaultExtractorsFactory();
                MediaSource media=new ExtractorMediaSource(uri,data,allocator,null,null);

                player.prepare(media,true,false);

            }

        }
    }

    private void releaseplayer()
    {
        if(player!=null)
        {
            playbackPosition=player.getCurrentPosition();
            currentWindow=player.getCurrentWindowIndex();
            playwhenReady=player.getPlayWhenReady();
            player.removeListener(componentListener);
            player.setVideoListener(null);
            player.setVideoDebugListener(null);
            player.setAudioDebugListener(null);
            player=null;
        }
    }

    private void hidesystemui()
    {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private MediaSource buildMediaSource(Uri uri)
    {
        DataSource.Factory datasourcefac=new DefaultHttpDataSourceFactory("ua",defaultBandwidthMeter);
        DashChunkSource.Factory dash=new DefaultDashChunkSource.Factory(datasourcefac);
        return new DashMediaSource(uri,datasourcefac,dash,null,null);
    }



    private class ComponentListener implements ExoPlayer.EventListener, VideoRendererEventListener,AudioRendererEventListener, DownloadListener
    {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest)
        {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playwhenready, int playbackState)
        {
            ProgressBar progressBar=(ProgressBar) findViewById(R.id.prog);
            //progressBar.setBackgroundColor(Color.parseColor("#FFFFFF"));
            String stateString;
            switch (playbackState)
            {
                case ExoPlayer.STATE_IDLE:
                    progressBar.setVisibility(View.GONE);
                    stateString="ExoPlayer.STATE_IDLE";
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    progressBar.setVisibility(View.VISIBLE);
                    stateString="ExoPlayer.STATE_BUFFERING";

                    break;
                case ExoPlayer.STATE_READY:
                    progressBar.setVisibility(View.GONE);
                    stateString="ExoPlayer.STATE_READY";
                    break;
                case ExoPlayer.STATE_ENDED:
                    progressBar.setVisibility(View.GONE);
                    stateString="ExoPlayer.STATE_ENDED";
                    break;
                default:
                    progressBar.setVisibility(View.GONE);
                    stateString="UNKNOWN_STATE";
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity() {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onAudioEnabled(DecoderCounters counters) {

        }

        @Override
        public void onAudioSessionId(int audioSessionId) {

        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onAudioInputFormatChanged(Format format) {

        }

        @Override
        public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoEnabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onVideoInputFormatChanged(Format format) {

        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {

        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

        }
    }

    public void download(View view)
    {
        sharedPreferences=this.getSharedPreferences("download",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.clear();
        editor.putString("down","yes");
        editor.apply();

        DataSource.Factory data=new DefaultDataSourceFactory(this,Util.getUserAgent(this,"ExoPlayerApp"),defaultBandwidthMeter);

        if(Build.VERSION.SDK_INT>=23)
        {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
            {

                File c=null;
                if(!this.getCacheDir().exists()) {
                    c.mkdir();
                    System.out.println("Created");
                }
                Cache cache = new SimpleCache(this.getCacheDir(), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 10));
                CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(cache, data, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

                DefaultExtractorsFactory allocator=new DefaultExtractorsFactory();
                MediaSource media=new ExtractorMediaSource(uri,cacheDataSourceFactory,allocator,null,null);
                player.prepare(media,true,false);

            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

            }
        }
        else
        {
            File c=null;
            if(!this.getCacheDir().exists()) {
                c.mkdir();
                System.out.println("Created");
            }
            Cache cache = new SimpleCache(this.getCacheDir(), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 10));
            CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(cache, data, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

            DefaultExtractorsFactory allocator=new DefaultExtractorsFactory();
            MediaSource media=new ExtractorMediaSource(uri,cacheDataSourceFactory,allocator,null,null);
            player.prepare(media,true,false);

        }

    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        player.release();
    }

    public void removedownload(View view)
    {
        Cache cache = new SimpleCache(this.getCacheDir(), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 10));
        DataSpec dataSpec=new DataSpec(uri);
        com.google.android.exoplayer2.upstream.cache.CacheUtil.remove(cache, CacheUtil.getKey(dataSpec));

    }

}
