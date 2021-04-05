package rs.highlande.app.tatatu.core.util


/**
 * File containing all the const vals found in the app.
 * @author mbaldrighi on 2019-06-24.
 */

//region = ViewPager =

const val NAV_PAGE_HOME = 0
const val NAV_PAGE_ACCOUNT = 1
const val NAV_PAGE_SEARCH = 2
const val NAV_PAGE_NOTIF = 3

//endregion


//region = SharedPreferences =
//endregion


//region = Bundle keys =

const val BUNDLE_KEY_USER_ID = "USER_ID"
const val BUNDLE_KEY_POST_ID = "POST_ID"
const val BUNDLE_KEY_EDIT_POST = "EDIT_POST"
const val BUNDLE_KEY_IS_EDIT= "IS_EDIT_POST"
const val BUNDLE_KEY_POSITION= "POSITION"
const val BUNDLE_KEY_TIMELINE_TYPE= "TIMELINE_TYPE"

const val BUNDLE_KEY_PICTURE:String="picture"
const val BUNDLE_KEY_NAME:String="name"
const val BUNDLE_KEY_USERNAME:String="username"
const val BUNDLE_KEY_BIO:String="bio"
const val BUNDLE_KEY_WEBSITE:String="website"
const val BUNDLE_KEY_EMAIL:String="email"
const val BUNDLE_KEY_PASSWORD:String="password"
const val BUNDLE_KEY_PHONE_NO:String="phoneNo"
const val BUNDLE_KEY_COUNTRY:String="country"
const val BUNDLE_KEY_DATE_OF_BIRTH:String="dateOfBirth"
const val BUNDLE_KEY_GENDER:String="gender"

const val BUNDLE_KEY_ON_BOARD_TITLE= "onBoardTitle"
const val BUNDLE_KEY_ON_BOARD_SUB_TITLE= "onBoardSubTitle"
const val BUNDLE_KEY_ON_BOARD_IMAGE= "onBoardImage"
const val BUNDLE_KEY_PAGE_SIZE= "pageSize"
const val BUNDLE_KEY_PAGINATION_TOKEN= "paginationToken"

const val BUNDLE_KEY_FROM_NOTIFICATION= "BUNDLE_KEY_FROM_NOTIFICATION"

//endregion


//region = Activity Results =

const val REQUEST_CODE_KEY = "request_code"
const val NO_RESULT = -1
const val EMAIL = "EMAIL"
const val COPY_TEXT = "Copied Text"
//endregion

//region follow

const val FOLLOW_REQUESTS_INCOMING = 0
const val FOLLOW_REQUESTS_SENT = 1

const val SEARCH_FOLLOWERS = 0
const val SEARCH_FOLLOWING = 1

//endregion

const val PAGINATION_SIZE = 20
const val DEFAULT_ELEMENTS_HOME = 10

const val TIME_UNIT_SECOND: Long = 1000
const val TIME_UNIT_MINUTE = 60 * TIME_UNIT_SECOND
const val TIME_UNIT_HOUR = 60 * TIME_UNIT_MINUTE
const val TIME_UNIT_DAY = 24 * TIME_UNIT_HOUR
const val TIME_UNIT_WEEK = 7 * TIME_UNIT_DAY
const val TIME_UNIT_MONTH = 30 * TIME_UNIT_DAY
const val TIME_UNIT_YEAR = 365 * TIME_UNIT_DAY

const val ONE_MB_IN_BYTES = 1e6.toLong()

const val VIBE_SHORT = 100L
const val VIBE_LONG = 750L
const val SELECT_FILE = 1
const val REQUEST_CAMERA = 0
const val PERMISSIONS_REQUEST_GALLERY = 2
const val PERMISSIONS_REQUEST_CODE = 10003
const val PERMISSIONS_REQUEST_LOCATION = 7
const val PERMISSIONS_REQUEST_CAMERA_MIC_CALLS = 8
const val PERMISSIONS_REQUEST_PHONE_STATE = 9


//Key for ColorPrivacy Data
const val ColorPrivacy = "ColorPrivacy"

const val BitmapFileName = "TatatuCropper.png"
const val FILE_BODY_PART= "file"
const val CommonTAG= "Tag"

//regions notifications

const val CHANNEL_NAME = "ttu_channel"
const val NOTIFICATION_GROUP = "mygroup"
const val NOTIFICATION_SUMMARY = 101
const val NOTIFICATION_REQUEST_CODE = 101
const val NOTIFICATION_CHAT_REPLY_REQUEST_CODE = 102
const val BUNDLE_NOTIFICATION_TYPE = "BUNDLE_NOTIFICATION_TYPE"
const val BUNDLE_NOTIFICATION_COUNT = "BUNDLE_NOTIFICATION_COUNT"
const val BUNDLE_NOTIFICATION_ID = "BUNDLE_NOTIFICATION_ID"
const val BUNDLE_NOTIFICATION_APP_FOREGROUND = "BUNDLE_NOTIFICATION_APP_FOREGROUND"
const val NOTIFICATION_TYPE_PROFILE = "PROFILE"
const val NOTIFICATION_TYPE_FEED = "FEED"
const val NOTIFICATION_TYPE_CHAT = "CHAT"
const val NOTIFICATION_TYPE_CALL = "CALL"
const val NOTIFICATION_GROUP_CHAT = "mygroupchat"
const val NOTIFICATION_SUMMARY_CHAT = 102

const val REQUEST_RESULT_NOTIFICATION = 0


/** Command to the service to display a message  */
const val MSG_SAY_HELLO = 1
const val MSG_REGISTER_CLIENT = 1
const val MSG_UNREGISTER_CLIENT = 2
const val MSG_SET_VALUE = 3

// chat extra param
const val EXTRA_PARAM_1 = "extra_param_1"
const val EXTRA_PARAM_2 = "extra_param_2"
const val EXTRA_PARAM_3 = "extra_param_3"
const val EXTRA_PARAM_4 = "extra_param_4"
const val EXTRA_PARAM_5 = "extra_param_5"
const val EXTRA_PARAM_6 = "extra_param_6"
const val EXTRA_PARAM_7 = "extra_param_7"
const val EXTRA_PARAM_8 = "extra_param_8"

/* FILES and PATHS */
const val PATH_CUSTOM_HIGHLANDERS = "ttu-cache"
const val PATH_EXTERNAL_DIR_MEDIA_PHOTO = "ttu-pictures"
//			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
const val PATH_EXTERNAL_DIR_MEDIA_VIDEO = "ttu-videos"
//			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
const val PATH_EXTERNAL_DIR_MEDIA_AUDIO = "ttu-audio"
const val FILENAME_MEDIA_AUDIO = "ttu-audio"
const val FILENAME_MEDIA_PHOTO = "ttu-image"
const val FILENAME_MEDIA_VIDEO = "ttu-video"
const val MIME_VIDEO = "video/mp4"
const val MIME_AUDIO = "audio/mp4"
const val MIME_PIC = "image/jpg"

const val RESULT_FULL_VIEW_VIDEO = 18
const val WEB_LINK_PLACEHOLDER_URL = "https://s3.eu-west-3.amazonaws.com/luiss-media-storage/0Fixed/Empty.png"