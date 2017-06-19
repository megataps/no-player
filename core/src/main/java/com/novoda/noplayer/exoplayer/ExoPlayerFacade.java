package com.novoda.noplayer.exoplayer;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.text.TextRenderer;
import com.novoda.noplayer.ContentType;
import com.novoda.noplayer.PlayerAudioTrack;
import com.novoda.noplayer.PlayerSubtitleTrack;
import com.novoda.noplayer.VideoDuration;
import com.novoda.noplayer.VideoPosition;
import com.novoda.noplayer.exoplayer.forwarder.ExoPlayerForwarder;
import com.novoda.noplayer.exoplayer.mediasource.ExoPlayerAudioTrackSelector;
import com.novoda.noplayer.exoplayer.mediasource.ExoPlayerSubtitleTrackSelector;
import com.novoda.noplayer.exoplayer.mediasource.MediaSourceFactory;

import java.util.List;

class ExoPlayerFacade {

    private static final boolean RESET_POSITION = true;
    private static final boolean DO_NOT_RESET_STATE = false;

    private final MediaSourceFactory mediaSourceFactory;
    private final ExoPlayerAudioTrackSelector audioTrackSelector;
    private final ExoPlayerSubtitleTrackSelector subtitleTrackSelector;
    private final ExoPlayerCreator exoPlayerCreator;

    @Nullable
    private SimpleExoPlayer exoPlayer;
    @Nullable
    private TextRenderer.Output output;

    ExoPlayerFacade(MediaSourceFactory mediaSourceFactory,
                    ExoPlayerAudioTrackSelector audioTrackSelector,
                    ExoPlayerSubtitleTrackSelector subtitleTrackSelector,
                    ExoPlayerCreator exoPlayerCreator) {
        this.mediaSourceFactory = mediaSourceFactory;
        this.audioTrackSelector = audioTrackSelector;
        this.subtitleTrackSelector = subtitleTrackSelector;
        this.exoPlayerCreator = exoPlayerCreator;
    }

    boolean isPlaying() {
        return exoPlayer != null && exoPlayer.getPlayWhenReady();
    }

    VideoPosition getPlayheadPosition() {
        assertVideoLoaded();
        return VideoPosition.fromMillis(exoPlayer.getCurrentPosition());
    }

    VideoDuration getMediaDuration() {
        assertVideoLoaded();
        return VideoDuration.fromMillis(exoPlayer.getDuration());
    }

    int getBufferPercentage() {
        assertVideoLoaded();
        return exoPlayer.getBufferedPercentage();
    }

    void play(SurfaceHolder surfaceHolder, VideoPosition position) {
        seekTo(position);
        play(surfaceHolder);
    }

    void play(SurfaceHolder surfaceHolder) {
        assertVideoLoaded();
        exoPlayer.clearVideoSurfaceHolder(surfaceHolder);
        exoPlayer.setVideoSurfaceHolder(surfaceHolder);
        exoPlayer.setPlayWhenReady(true);
    }

    void pause() {
        assertVideoLoaded();
        exoPlayer.setPlayWhenReady(false);
    }

    void seekTo(VideoPosition position) {
        assertVideoLoaded();
        exoPlayer.seekTo(position.inMillis());
    }

    void release() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    void stop() {
        assertVideoLoaded();
        exoPlayer.stop();
    }

    void loadVideo(Uri uri, ContentType contentType, ExoPlayerForwarder forwarder) {
        exoPlayer = exoPlayerCreator.create();
        exoPlayer.addListener(forwarder.exoPlayerEventListener());
        exoPlayer.setVideoDebugListener(forwarder.videoRendererEventListener());
        MediaSource mediaSource = mediaSourceFactory.create(
                contentType,
                uri,
                forwarder.extractorMediaSourceListener(),
                forwarder.mediaSourceEventListener()
        );
        exoPlayer.prepare(mediaSource, RESET_POSITION, DO_NOT_RESET_STATE);
        setExoPlayerTextOutput(output);
    }

    void selectAudioTrack(PlayerAudioTrack audioTrack) {
        audioTrackSelector.selectAudioTrack(audioTrack, rendererTypeRequester);
    }

    List<PlayerAudioTrack> getAudioTracks() {
        return audioTrackSelector.getAudioTracks(rendererTypeRequester);
    }

    void setSubtitleRendererOutput(TextRenderer.Output output) {
        this.output = output;
        setExoPlayerTextOutput(output);
    }

    private void setExoPlayerTextOutput(TextRenderer.Output output) {
        assertVideoLoaded();
        exoPlayer.setTextOutput(output);
    }

    void selectSubtitleTrack(PlayerSubtitleTrack subtitleTrack) {
        subtitleTrackSelector.selectTextTrack(subtitleTrack, rendererTypeRequester);
    }

    List<PlayerSubtitleTrack> getSubtitleTracks() {
        return subtitleTrackSelector.getSubtitleTracks(rendererTypeRequester);
    }

    boolean hasPlayedContent() {
        return exoPlayer != null;
    }

    void clearSubtitleTrack() {
        subtitleTrackSelector.clearSubtitleTrack(rendererTypeRequester);
    }

    void selectFirstAvailableSubtitlesTrack() {
        subtitleTrackSelector.selectFirstTextTrack(rendererTypeRequester);
    }

    @VisibleForTesting
    final RendererTypeRequester rendererTypeRequester = new RendererTypeRequester() {
        @Override
        public int getRendererTypeFor(int index) {
            assertVideoLoaded();
            return exoPlayer.getRendererType(index);
        }
    };

    private void assertVideoLoaded() {
        if (exoPlayer == null) {
            throw new IllegalStateException("Video must be loaded before trying to interact with the player");
        }
    }
}
