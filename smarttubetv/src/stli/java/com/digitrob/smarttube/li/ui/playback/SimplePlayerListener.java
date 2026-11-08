package com.digitrob.smarttube.li.ui.playback;

import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.listener.PlayerEventListener;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;

/**
 * No-op adapter for {@link PlayerEventListener}. The touch player overrides only what it needs.
 */
public class SimplePlayerListener implements PlayerEventListener {
    @Override
    public void onNewVideo(Video item) {
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
    }

    @Override
    public void onInit() {
    }

    @Override
    public void onFinish() {
    }

    @Override
    public void onSuggestionItemClicked(Video item) {
    }

    @Override
    public void onSuggestionItemLongClicked(Video item) {
    }

    @Override
    public void onScrollEnd(Video item) {
    }

    @Override
    public boolean onPreviousClicked() {
        return false;
    }

    @Override
    public boolean onNextClicked() {
        return false;
    }

    @Override
    public void onPlayClicked() {
    }

    @Override
    public void onPauseClicked() {
    }

    @Override
    public boolean onKeyDown(int keyCode) {
        return false;
    }

    @Override
    public void onButtonClicked(int buttonId, int buttonState) {
    }

    @Override
    public void onButtonLongClicked(int buttonId, int buttonState) {
    }

    @Override
    public void onControlsShown(boolean shown) {
    }

    @Override
    public void onPlay() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onPlayEnd() {
    }

    @Override
    public void onBuffering() {
    }

    @Override
    public void onSeekEnd() {
    }

    @Override
    public void onSeekPositionChanged(long positionMs) {
    }

    @Override
    public void onSpeedChanged(float speed) {
    }

    @Override
    public void onSourceChanged(Video item) {
    }

    @Override
    public void onVideoLoaded(Video item) {
    }

    @Override
    public void onEngineInitialized() {
    }

    @Override
    public void onEngineReleased() {
    }

    @Override
    public void onEngineError(int type, int rendererIndex, Throwable error) {
    }

    @Override
    public void onTrackChanged(FormatItem track) {
    }

    @Override
    public void onTrackSelected(FormatItem track) {
    }

    @Override
    public void onViewCreated() {
    }

    @Override
    public void onViewDestroyed() {
    }

    @Override
    public void onViewPaused() {
    }

    @Override
    public void onViewResumed() {
    }

    @Override
    public void onTickle() {
    }
}
