package rs.highlande.app.tatatu.connection.http

import rs.highlande.app.tatatu.BuildConfig

/**
 * File holding reference to all constant values related to http protocol.
 * @author mbaldrighi on 2019-07-12.
 */


//region == COMMON ==

const val HTTP_TIMEOUT_DEV = 60L
const val HTTP_TIMEOUT_PROD = 30L

val HTTP_TIMEOUT = if (BuildConfig.USE_PROD_CONNECTION) HTTP_TIMEOUT_PROD else HTTP_TIMEOUT_DEV

//endregion


//region == BRIGHTCOVE ==

/**
 * The base URL pointing to Brightcove servers.
 */
const val BASE_URL = "https://edge-elb.api.brightcove.com/"

/**
 * The Brightcove policy key to authenticate into Brightcove servers.
 */
val POLICY_KEY =
    if (BuildConfig.USE_PROD_CONNECTION) "BCpkADawqM26WkFn5LVpfhJw3_rErby9YCVzy1iK3lsc7OXMhTm6biUWYmwVUi6AvZmNXllC2pw2IpHDg1nUo7BC47J8VOUU9xzpk-Pq5KhnUXh89ROyr16-ZZHw_rZnYiamiUieWJrAZPNb"
    else "BCpkADawqM3ALh3QrKtE1VxA88PO3j1ypGNNsnzNfCATzV6Ib1bapVWX06VLjUjapmjbQxaYroLAB93UcazY0aLdTg6QfRmaDZyJMc0PpJciKVgPBwpCp5ngmL-NhBVkEQqA4CgXlQ5W_cge"

const val POLICY_KEY_SEARCH = "BCpkADawqM3ALh3QrKtE1VxA88PO3j1ypGNNsnzNfCATzV6Ib1bapVWX06VLjUjapmjbQxaYroLAB93UcazY0aLdTg6QfRmaDZyJMc0PpJciKVgPBwpCp5ngmL-NhBVkEQqA4CgXlQ5W_cge"

/**
 * "pk=" prefix + the Brightcove policy key to authenticate into Brightcove servers.
 */
val POLICY_KEY_HEADER = "pk=${POLICY_KEY}"

/**
 * The value needed to send JSON body to server.
 */
const val APPL_JSON = "application/json"

/**
 * The Brightcove account ID for TTU.
 */
const val ACCOUNT_ID = "5972928262001"

/**
 * The Brightcove account ID for TTU.
 */
const val AD_CONFIG_ID = "373fd7a0-eb69-4f5e-8bcc-fd204b5c0a14"

/**
 * The header holding the [POLICY_KEY_HEADER].
 */
const val HEADER_ACCEPT = "Accept"

/**
 * Query param referring to the element to be skipped during pagination process.
 */
const val PARAM_OFFSET = "offset"

/**
 * Query param referring to the upper relative limit of the elements to be queried during pagination process.
 */
const val PARAM_LIMIT = "limit"

/**
 * Query param referring to the search query string.
 */
const val PARAM_QUERY = "q"

/**
 * Query param referring to the search results sort order for more information see
 * https://support.brightcove.com/overview-playback-api#Search_videos.
 */
const val PARAM_SORT = "sort"

/**
 * Query param referring to the element to be skipped during pagination process.
 */
const val PARAM_AD_CONFIG_ID = "ad_config_id"

const val PARAM_ACCOUNT_ID = "account_id"
const val PARAM_PLAYLIST_ID = "playlist_id"
const val PARAM_VIDEO_ID = "video_id"

const val PARAM_X_ID = "x-id"
const val PARAM_X_SESSION_ID= "x-session-id"
const val PARAM_X_UPLOAD_TYPE= "x-upload-type"
const val PARAM_X_MEDIA_TYPE= "x-media-type"



//region = URLs =

const val URL_PLAYLIST = "playback/v1/accounts/{$PARAM_ACCOUNT_ID}/playlists/{$PARAM_PLAYLIST_ID}"
const val URL_PLAYBACK = "playback/v1/accounts/{$PARAM_ACCOUNT_ID}/videos/{$PARAM_VIDEO_ID}"
const val URL_PLAYBACK_AD = "playback/v1/accounts/{$PARAM_ACCOUNT_ID}/videos/{video_id}?$PARAM_AD_CONFIG_ID={ad_config_id}"
const val URL_PLAYBACK_SEARCH = "playback/v1/accounts/{$PARAM_ACCOUNT_ID}/videos"

//endregion


//endregion



//region == TTU ==

const val BASE_URL_TTU_HTTP = "http://ttudev.highlanders.app:5000/api/"
const val BASE_URL_TTU_HTTPS = "https://uploadttu.highlanders.app/api/"
const val BASE_URL_TTU_HTTP_DEBUG = "http://accessocasa.ddns.net:5000/api/"

val BASE_URL_TTU = if (BuildConfig.USE_PROD_CONNECTION) BASE_URL_TTU_HTTPS else BASE_URL_TTU_HTTP

const val PARAM_USERNAME = "username"

const val TOKEN_CHECK_USERNAME = "CheckUsername/{$PARAM_USERNAME}"
const val TOKEN_UPLOADMEDIA = "UploadMedia"
const val TOKEN_USER_GATEWAY_CUSTOM = "UserGatewayCustomFlow"
const val TOKEN_USER_GATEWAY_SOCIAL = "UserGatewaySocialFlow"
const val TOKEN_USER_GATEWAY_SOCIAL_CONFIRM = "UserGatewayConfirmSocial"
const val TOKEN_BRIDGE_DO_ACTION = "DoAction"

//endregion


//region == ERRORS ==

const val ERROR_EXISTING_USERNAME = 3001
const val ERROR_SOCIAL_TO_CONFIRM = 3026

//endregion
