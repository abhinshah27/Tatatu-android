/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat

import android.content.Context
import android.util.Patterns
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONException
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_PARSED_WEB_LINK
import rs.highlande.app.tatatu.core.util.BaseHelper
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.model.chat.ChatMessage
import rs.highlande.app.tatatu.model.chat.HLWebLink
import java.util.concurrent.ConcurrentHashMap


class WebLinkRecognizer(
    context: Context
): BaseHelper(context), OnServerMessageReceivedListener, KoinComponent {

    private val api: CommonApi by inject()

    /**
     * Map persisting messages and link, to be queried after server response
     */
    private val linksMap = ConcurrentHashMap<String, String>()


    fun recognize(text: String?, messageID: String?, listener: LinkRecognitionListener? = null) {
        text?.let {

            val result =
                extractAppendedText(
                    it,
                    null
                )

            if (!result?.second.isNullOrBlank()) {
                if (listener != null) {
                    listener.onLinkRecognized(result!!.second)
                } else if (result!!.second.matches(Regex("^(https?)://.*$"))) {
                    callForLinkParsing(result.second, messageID)
                }
            }
        }
    }

    private fun callForLinkParsing(link: String?, messageID: String?) {
        if (!link.isNullOrBlank() && !messageID.isNullOrBlank() && !linksMap.contains(messageID)) {
            try {
                val pair = api.parseWebLink(link, messageID, this)
                val disposable = pair.first
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribeWith(object : DisposableObserver<RequestStatus>() {
                        override fun onComplete() {
                            dispose()
                        }

                        override fun onNext(t: RequestStatus) {
                            pair.second?.let {
                                linksMap[it] = messageID
                            }
                        }

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                            dispose()
                        }
                    })
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        if (callCode == SERVER_OP_GET_PARSED_WEB_LINK) {
            if (response?.length() != 1 || response.optJSONObject(0).length() == 0) {
                handleErrorResponse(idOp, callCode, 0, "")
                return
            }

            val wl = HLWebLink.get(response.optJSONObject(0))
            if (wl == null) {
                handleErrorResponse(idOp, callCode, 0, "IllegalArgumentException: WebLink is null")
                return
            }

            RealmUtils.useTemporaryRealm { realm ->
                realm.executeTransaction {
                    val messageID = if (!idOp.isBlank()) linksMap[idOp] else null
                    if (!messageID.isNullOrBlank()) {
                        val message = RealmUtils.readFirstFromRealmWithId(it, ChatMessage::class.java, "messageID", messageID) as ChatMessage
                        message.webLink = it.copyToRealmOrUpdate(wl)
                    }

                    linksMap.remove(idOp)
                }
            }
        }
    }

    override fun handleErrorResponse(
        idOp: String,
        callCode: Int,
        errorCode: Int,
        description: String?
    ) {
        if (callCode == SERVER_OP_GET_PARSED_WEB_LINK)
            LogUtils.e(logTag, "Error recognizing Web Link with error: $errorCode and description: $description")
    }


    private fun isURLValid(url: String): Boolean {
        return url.startsWith("http://", true) || url.startsWith("https://", true) ||
                url.startsWith("rtsp://", true) || url.startsWith("www.", true) ||
                url.startsWith("ftp://", true)
    }


    interface LinkRecognitionListener {
        fun onLinkRecognized(group: String)
    }



    companion object {
        private val logTag = WebLinkRecognizer::class.java.simpleName

        private const val WEB_LINK_REGEX = "(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\." +
                "\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\." +
                "\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]" +
                "\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]" +
                "\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\." +
                "(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}" +
                "-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?"

        private const val WEB_LINK_REGEX_2 = "^((ftp|http|https):\\/\\/)?(www.)?(?!.*(ftp|http|https|www.))[a-zA-Z0-9_-]+(\\.[a-zA-Z]+)+((\\/)[\\w#]+)*(\\/\\w+\\?[a-zA-Z0-9_]+=\\w+(&[a-zA-Z0-9_]+=\\w+)*)?$"

        private const val WEB_LINK_REGEX_3 = "^" +
                // protocol identifier (optional)
                // short syntax // still required
                "(?:(?:(?:https?|ftp):)?\\/\\/)" +
                // user:pass BasicAuth (optional)
                "(?:\\S+(?::\\S*)?@)?" +
                "(?:" +
                // IP address exclusion
                // private & local networks
                "(?!(?:10|127)(?:\\.\\d{1,3}){3})" +
                "(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})" +
                "(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})" +
                // IP address dotted notation octets
                // excludes loopback network 0.0.0.0
                // excludes reserved space >= 224.0.0.0
                // excludes network & broadcast addresses
                // (first & last IP address of each class)
                "(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])" +
                "(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}" +
                "(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))" +
                "|" +
                // host & domain names, may end with dot
                // can be replaced by a shortest alternative
                // (?![-_])(?:[-\\w\\u00a1-\\uffff]{0,63}[^-_]\\.)+
                "(?:" +
                "(?:" +
                "[a-z0-9\\u00a1-\\uffff]" +
                "[a-z0-9\\u00a1-\\uffff_-]{0,62}" +
                ")?" +
                "[a-z0-9\\u00a1-\\uffff]\\." +
                ")+" +
                // TLD identifier name, may end with dot
                "(?:[a-z\\u00a1-\\uffff]{2,}\\.?)" +
                ")" +
                // port number (optional)
                "(?::\\d{2,5})?" +
                // resource path (optional)
                "(?:[/?#]\\S*)?" +
                "$"

        /**
         * Splits the given text message into web url and actual message if the already parsed url
         * is provided or if a [Patterns.WEB_URL] is recognized.
         * @param messageText The text message to check.
         * @param link The link already parsed if present.
         * @return A 2-String [Pair] containing message and url, `null` if the a blank message was
         * provided.
         */
        fun extractAppendedText(messageText: String, link: String?): Pair<String, String>? {
            if (messageText.isBlank()) return null

            return if (!link.isNullOrBlank()) {
                messageText
                    .replaceFirst(Regex(link), "")
                    .replace("\n", "")
                    .replace("\t", "")
                    .trim() to link
            } else {
                var finalLink = messageText
                var finalMessage = ""

                Patterns.WEB_URL.split(messageText).also {
                    for (i in it) {
                        finalMessage += i
                        finalLink = finalLink.replace(i, "")
                    }
                }
                finalMessage
                    .replace("\n", "")
                    .replace("\t", "")
                    .trim() to finalLink
            }
        }
    }

}