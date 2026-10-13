package com.volume_plus_plus.app.overlay

import android.media.AudioManager

/**
 * Whether a call is in progress right now.
 *
 * Covers both kinds, because both put the device on the call stream and both are what the panel has
 * to react to: a cellular call (`MODE_IN_CALL`) and an app's own voice or video chat
 * (`MODE_IN_COMMUNICATION`) — Discord, a messenger call, an in-game voice channel — which is the
 * commoner one by far, and the one issue 19 asked for.
 *
 * The audio mode is what the system's own volume panel keys off, so this flips at exactly the moment
 * the stock panel changes shape. Reading it can throw on a stubbed-out AudioManager (some emulated
 * builds), and "no call" is the right answer when it does.
 */
internal fun AudioManager.inCall(): Boolean = runCatching {
    mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
}.getOrDefault(false)
