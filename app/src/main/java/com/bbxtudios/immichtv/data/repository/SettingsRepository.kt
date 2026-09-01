package com.bbxtudios.immichtv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "immich_tv_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        const val DEFAULT_SERVER_URL = ""
        const val DEFAULT_API_KEY = ""
        const val DEFAULT_GROUP_ORDER = "desc"
        const val DEFAULT_ASSET_ORDER = "desc"
        const val DEFAULT_SLIDESHOW_INTERVAL = 3 // 3 segundos por defecto
        const val DEFAULT_LOOP_SLIDESHOW = true
        const val DEFAULT_LOOP_VIDEO = false
        const val DEFAULT_ZOOM_LEVEL = 1.75f
        const val DEFAULT_SHOW_METADATA = true
        const val DEFAULT_SHOW_CAMERA_INFO = true
        const val DEFAULT_SHOW_EXIF_DETAILS = true
        const val DEFAULT_SHOW_LOCATION_INFO = true
        const val DEFAULT_SHOW_DATE_INFO = true
        const val DEFAULT_SHOW_COUNTER = true
        const val DEFAULT_SHOW_VIDEO_SPECS = true
        const val DEFAULT_PRELOADING = true
        const val DEFAULT_ANIM_FOCUS = true
        const val DEFAULT_CARD_SHADOWS = true
        const val DEFAULT_GRID_COLUMNS = 5
        const val DEFAULT_SMOOTH_SCROLL = true
        const val DEFAULT_APP_LANGUAGE = "auto" // "auto", "es", "en"

        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_GROUP_ORDER = stringPreferencesKey("group_order")
        private val KEY_ASSET_ORDER = stringPreferencesKey("asset_order")
        private val KEY_SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval")
        private val KEY_LOOP_SLIDESHOW = booleanPreferencesKey("loop_slideshow")
        private val KEY_LOOP_VIDEO = booleanPreferencesKey("loop_video")
        private val KEY_ZOOM_LEVEL = floatPreferencesKey("zoom_level")
        private val KEY_SHOW_METADATA = booleanPreferencesKey("show_metadata")
        private val KEY_SHOW_CAMERA_INFO = booleanPreferencesKey("show_camera_info")
        private val KEY_SHOW_EXIF_DETAILS = booleanPreferencesKey("show_exif_details")
        private val KEY_SHOW_LOCATION_INFO = booleanPreferencesKey("show_location_info")
        private val KEY_SHOW_DATE_INFO = booleanPreferencesKey("show_date_info")
        private val KEY_SHOW_COUNTER = booleanPreferencesKey("show_counter")
        private val KEY_SHOW_VIDEO_SPECS = booleanPreferencesKey("show_video_specs")
        private val KEY_PRELOADING = booleanPreferencesKey("preloading")
        private val KEY_ANIM_FOCUS = booleanPreferencesKey("anim_focus")
        private val KEY_CARD_SHADOWS = booleanPreferencesKey("card_shadows")
        private val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        private val KEY_SMOOTH_SCROLL = booleanPreferencesKey("smooth_scroll")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: DEFAULT_APP_LANGUAGE
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: DEFAULT_API_KEY
    }

    val groupOrder: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GROUP_ORDER] ?: DEFAULT_GROUP_ORDER
    }

    val assetOrder: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ASSET_ORDER] ?: DEFAULT_ASSET_ORDER
    }

    val slideshowInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SLIDESHOW_INTERVAL] ?: DEFAULT_SLIDESHOW_INTERVAL
    }

    val loopSlideshow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOOP_SLIDESHOW] ?: DEFAULT_LOOP_SLIDESHOW
    }

    val loopVideo: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOOP_VIDEO] ?: DEFAULT_LOOP_VIDEO
    }

    val zoomLevel: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_ZOOM_LEVEL] ?: DEFAULT_ZOOM_LEVEL
    }

    val showMetadata: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_METADATA] ?: DEFAULT_SHOW_METADATA
    }

    val showCameraInfo: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_CAMERA_INFO] ?: DEFAULT_SHOW_CAMERA_INFO
    }

    val showExifDetails: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_EXIF_DETAILS] ?: DEFAULT_SHOW_EXIF_DETAILS
    }

    val showLocationInfo: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_LOCATION_INFO] ?: DEFAULT_SHOW_LOCATION_INFO
    }

    val showDateInfo: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_DATE_INFO] ?: DEFAULT_SHOW_DATE_INFO
    }

    val showCounter: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_COUNTER] ?: DEFAULT_SHOW_COUNTER
    }

    val showVideoSpecs: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_VIDEO_SPECS] ?: DEFAULT_SHOW_VIDEO_SPECS
    }

    val preloading: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRELOADING] ?: DEFAULT_PRELOADING
    }

    val animFocus: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ANIM_FOCUS] ?: DEFAULT_ANIM_FOCUS
    }

    val cardShadows: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CARD_SHADOWS] ?: DEFAULT_CARD_SHADOWS
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_GRID_COLUMNS] ?: DEFAULT_GRID_COLUMNS
    }

    val smoothScroll: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SMOOTH_SCROLL] ?: DEFAULT_SMOOTH_SCROLL
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url.trim() }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key.trim() }
    }

    suspend fun setGroupOrder(order: String) {
        context.dataStore.edit { it[KEY_GROUP_ORDER] = order }
    }

    suspend fun setAssetOrder(order: String) {
        context.dataStore.edit { it[KEY_ASSET_ORDER] = order }
    }

    suspend fun setSlideshowInterval(seconds: Int) {
        context.dataStore.edit { it[KEY_SLIDESHOW_INTERVAL] = seconds }
    }

    suspend fun setLoopSlideshow(loop: Boolean) {
        context.dataStore.edit { it[KEY_LOOP_SLIDESHOW] = loop }
    }

    suspend fun setLoopVideo(loop: Boolean) {
        context.dataStore.edit { it[KEY_LOOP_VIDEO] = loop }
    }

    suspend fun setZoomLevel(zoom: Float) {
        context.dataStore.edit { it[KEY_ZOOM_LEVEL] = zoom }
    }

    suspend fun setShowMetadata(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_METADATA] = show }
    }

    suspend fun setShowCameraInfo(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_CAMERA_INFO] = show }
    }

    suspend fun setShowExifDetails(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_EXIF_DETAILS] = show }
    }

    suspend fun setShowLocationInfo(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_LOCATION_INFO] = show }
    }

    suspend fun setShowDateInfo(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_DATE_INFO] = show }
    }

    suspend fun setShowCounter(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_COUNTER] = show }
    }

    suspend fun setShowVideoSpecs(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_VIDEO_SPECS] = show }
    }

    suspend fun setPreloading(preload: Boolean) {
        context.dataStore.edit { it[KEY_PRELOADING] = preload }
    }

    suspend fun setAnimFocus(anim: Boolean) {
        context.dataStore.edit { it[KEY_ANIM_FOCUS] = anim }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { it[KEY_APP_LANGUAGE] = lang }
    }

    suspend fun setCardShadows(shadows: Boolean) {
        context.dataStore.edit { it[KEY_CARD_SHADOWS] = shadows }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[KEY_GRID_COLUMNS] = columns.coerceIn(3, 8) }
    }

    suspend fun setSmoothScroll(smooth: Boolean) {
        context.dataStore.edit { it[KEY_SMOOTH_SCROLL] = smooth }
    }

    suspend fun setAllPerformance(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_ANIM_FOCUS] = enabled
            it[KEY_CARD_SHADOWS] = enabled
            it[KEY_SMOOTH_SCROLL] = enabled
            it[KEY_PRELOADING] = enabled
        }
    }

    suspend fun resetDefaults() {
        context.dataStore.edit {
            it[KEY_GROUP_ORDER] = DEFAULT_GROUP_ORDER
            it[KEY_ASSET_ORDER] = DEFAULT_ASSET_ORDER
            it[KEY_SLIDESHOW_INTERVAL] = DEFAULT_SLIDESHOW_INTERVAL
            it[KEY_LOOP_SLIDESHOW] = DEFAULT_LOOP_SLIDESHOW
            it[KEY_LOOP_VIDEO] = DEFAULT_LOOP_VIDEO
            it[KEY_ZOOM_LEVEL] = DEFAULT_ZOOM_LEVEL
            it[KEY_SHOW_METADATA] = DEFAULT_SHOW_METADATA
            it[KEY_SHOW_CAMERA_INFO] = DEFAULT_SHOW_CAMERA_INFO
            it[KEY_SHOW_LOCATION_INFO] = DEFAULT_SHOW_LOCATION_INFO
            it[KEY_SHOW_DATE_INFO] = DEFAULT_SHOW_DATE_INFO
            it[KEY_SHOW_COUNTER] = DEFAULT_SHOW_COUNTER
            it[KEY_SHOW_VIDEO_SPECS] = DEFAULT_SHOW_VIDEO_SPECS
            it[KEY_PRELOADING] = DEFAULT_PRELOADING
            it[KEY_ANIM_FOCUS] = DEFAULT_ANIM_FOCUS
            it[KEY_CARD_SHADOWS] = DEFAULT_CARD_SHADOWS
            it[KEY_GRID_COLUMNS] = DEFAULT_GRID_COLUMNS
            it[KEY_SMOOTH_SCROLL] = DEFAULT_SMOOTH_SCROLL
        }
    }
}
