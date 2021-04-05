package rs.highlande.app.tatatu.core.util

import android.content.Context
import io.realm.*
import io.realm.exceptions.RealmMigrationNeededException
import org.json.JSONObject
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.webSocket.realTime.RTCommHelper


/**
 * Class containing various utility methods concerning [Realm] operations.
 *
 * @author mbaldrighi on 10/7/2017.
 */
object RealmUtils : KoinComponent {

    val LOG_TAG = RealmUtils::class.java.simpleName

    private val realTimeHelper: RTCommHelper by inject()

    private lateinit var realmConfig: RealmConfiguration

    private val openRealms = mutableListOf<Realm>()


    /**
     * Initializes Realm library.
     * @param context the application [Context].
     */
    fun init(context: Context) {
        Realm.init(context)
        realmConfig = getTTUDefaultConfig()
    }

    /**
     * Initializes [RealmConfiguration] instance.
     * @return The [RealmConfiguration] assigned to the app.
     */
    private fun getTTUDefaultConfig(): RealmConfiguration {
        RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .schemaVersion(0)
//                .migration()

            // INFO: 2020-01-31    add method to reduce Realm file size and save storage space
            .compactOnLaunch()

            .build().apply {
                Realm.setDefaultConfiguration(this)
                return this
            }
    }

    /**
     * Performs the operations needed to logout the current user from the app.
     */
    fun doRealmLogout() {
        realTimeHelper.closeRealmInstance()
        closeAllRealms()
        Realm.deleteRealm(realmConfig)
        realTimeHelper.restoreRealmInstance()
    }


    /**
     * Instantiate a [Realm] with default [io.realm.RealmConfiguration] as defined by
     * [RealmUtils.getTTUDefaultConfig].
     * @return A default [Realm] instance.
     */
    // TODO: 10/7/2017   something with ANALYTICS FRAMEWORK
    val checkedRealm: Realm?
        get() {
            var realm: Realm? = null

            try {
                realm = Realm.getDefaultInstance()
            } catch (e: RealmMigrationNeededException) {
                LogUtils.e(LOG_TAG, e.message, e)

                try {
                    Realm.deleteRealm(realmConfig)
                    realm = Realm.getDefaultInstance()
                } catch (ex: Exception) {
                    LogUtils.e(LOG_TAG, e.message, ex)
                }

            } catch (e: IllegalArgumentException) {
                LogUtils.e(LOG_TAG, e.message, e)
                try {
                    Realm.deleteRealm(realmConfig)
                    realm = Realm.getDefaultInstance()
                } catch (ex: Exception) {
                    LogUtils.e(LOG_TAG, e.message, ex)
                }

            }

            if (realm != null)
                openRealms.add(realm)

            return realm
        }


    /**
     * Sets up a new Realm, invokes the provided function and closes the Realm instance.
     * @param block the lambda function hosting all the operations this Realm needs to perform before
     * closing.
     */
    fun useTemporaryRealm(block: (realm: Realm) -> Unit) {
        var realm: Realm? = null
        try {
            realm = checkedRealm
            realm?.let {
                block.invoke(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            closeRealm(realm)
        }
    }


    fun checkAndFetchRealm(realm: Realm): Realm? {
        return if (!isValid(realm)) checkedRealm else realm
    }

    fun closeRealm(realm: Realm?) {
        if (realm != null) {
            realm.close()
            openRealms.remove(realm)
        }
    }

    fun closeAllRealms() {
        if (openRealms.isNotEmpty()) {
            for (realm in openRealms) {
                try {
                    if (isValid(realm)) realm.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    LogUtils.e(LOG_TAG, "Close All Realms ERROR")
                }

            }

            openRealms.clear()
        }
    }

    /**
     * Checks whether the provided [Realm] instance is valid.
     * @param realm the provided [Realm] instance.
     * @return True if the instance is still valid, false otherwise.
     */
    fun isValid(realm: Realm?): Boolean {
        return realm != null && !realm.isClosed
    }

    /**
     * Checks if at least one object is present for a given [RealmModel].
     * @param model the given [RealmModel].
     * @return True if at least one object is present, false otherwise.
     */
    fun hasTableObject(realm: Realm, model: Class<out RealmModel>): Boolean {
        val results = readFromRealm(realm, model)
        return results != null && !results.isEmpty()
    }

    /**
     * Retrieves all the objects for a given [RealmModel].
     * @param model the given [RealmModel].
     * @return The [RealmResults] containing all the wanted objects.
     */
    fun readFromRealm(realm: Realm, model: Class<out RealmModel>): RealmResults<RealmModel>? {
        return if (isValid(realm)) {
            readFromRealmWithIdSorted(realm, model, null, null, null as Array<String>?, null)
        } else null

    }

    /**
     * Retrieves all the objects for a given [RealmModel].
     * @param model the given [RealmModel].
     * @return The [RealmResults] containing all the wanted objects.
     */
    fun readFromRealmSorted(
        realm: Realm,
        model: Class<out RealmModel>,
        sortFieldName: String,
        sortOrder: Sort
    ): RealmResults<RealmModel>? {
        return if (isValid(realm)) {
            readFromRealmWithIdSorted(realm, model, null, null, sortFieldName, sortOrder)
        } else null

    }

    /**
     * Retrieves only the first object for a given [RealmModel] and containing a given "_id".
     * @param realm the [Realm] used to operate the query.
     * @param model the given [RealmModel].
     * @param id the given id string.
     * @return The wanted [RealmModel] instance if present, null otherwise.
     */
    fun readFromRealmWithId(
        realm: Realm,
        model: Class<out RealmModel>,
        fieldName: String?,
        id: String
    ): RealmResults<RealmModel>? {
        return if (isValid(realm)) readFromRealmWithIdSorted(
            realm,
            model,
            fieldName,
            id,
            null as Array<String>?,
            null
        ) else null

    }

    /**
     * Retrieves only the first object for a given [RealmModel] and containing a given "_id".
     * @param realm the [Realm] used to operate the query.
     * @param model the given [RealmModel].
     * @param fieldName the given id string.
     * @param id the given id string.
     * @param sortFieldName the given string carrying the name of the field on which perform sorting ops.
     * @param sortOrder the given [Sort] value carrying the sort order for the field name.
     * @return The wanted [RealmModel] instance if present, null otherwise.
     */
    fun readFromRealmWithIdSorted(
        realm: Realm,
        model: Class<out RealmModel>,
        fieldName: String?,
        id: String?,
        sortFieldName: String,
        sortOrder: Sort
    ): RealmResults<RealmModel>? {
        return if (isValid(realm)) readFromRealmWithIdSorted(
            realm,
            model,
            fieldName,
            id,
            arrayOf(sortFieldName),
            arrayOf(sortOrder)
        ) else null

    }

    /**
     * Retrieves only the first object for a given [RealmModel] and containing a given "_id".
     * @param realm the [Realm] used to operate the query.
     * @param model the given [RealmModel].
     * @param fieldName the given id string.
     * @param id the given id string.
     * @param fieldNames the given string array containing the fields on which perform sorting ops.
     * @param sortOrders the given [Sort] array containing the sort orders for each sorting field name.
     * @return The wanted [RealmModel] instance if present, null otherwise.
     */
    fun readFromRealmWithIdSorted(
        realm: Realm,
        model: Class<out RealmModel>,
        fieldName: String?,
        id: String?,
        fieldNames: Array<String>?,
        sortOrders: Array<Sort>?
    ): RealmResults<RealmModel>? {
        if (isValid(realm)) {
            val query = realm.where(model)
            if (isStringValid(id))
                query.equalTo(if (isStringValid(fieldName)) fieldName else "id", id)
            if (fieldNames != null && fieldNames.isNotEmpty() &&
                sortOrders != null && sortOrders.isNotEmpty()
            )
                query.sort(fieldNames, sortOrders)
            return query.findAll() as RealmResults<RealmModel>
        }

        return null
    }

    /**
     * Retrieves only the first object for a given [RealmModel].
     * @param model the given [RealmModel].
     * @return The wanted [RealmModel] instance if present, null otherwise.
     */
    fun readFirstFromRealm(realm: Realm?, model: Class<out RealmModel>): RealmModel? {
        return if (isValid(realm)) realm!!.where(model).findFirst() else null

    }

    /**
     * Retrieves only the first object for a given [RealmModel] and containing a given "_id".
     * @param realm the [Realm] used to operate the query.
     * @param model the given [RealmModel].
     * @param id the given id string.
     * @return The wanted [RealmModel] instance if present, null otherwise.
     */
    fun readFirstFromRealmWithId(
        realm: Realm,
        model: Class<out RealmModel>,
        fieldName: String?,
        id: String
    ): RealmModel? {
        return if (isValid(realm)) realm.where(model).equalTo(
            if (isStringValid(fieldName)) fieldName else "id", id).findFirst() else null

    }

    /**
     * Writes given [RealmModel] to [Realm] through the action of
     * [Realm.copyToRealmOrUpdate] method.
     * @param model the given [RealmModel].
     */
    fun writeToRealm(realm: Realm?, model: RealmModel?) {
        if (model != null && isValid(realm)) {
            realm!!.executeTransaction { realm1 -> realm1.copyToRealmOrUpdate(model) }
        }
    }

    /**
     * Writes given [RealmModel] to [Realm] through the action of
     * [Realm.copyToRealmOrUpdate] method.
     * @param model the given [RealmModel].
     */
    fun writeToRealmNoTransaction(realm: Realm, model: RealmModel?) {
        if (model != null && isValid(realm)) {
            realm.copyToRealmOrUpdate(model)
        }
    }

    /**
     * Writes given [RealmModel] to [Realm] through the action of
     * [Realm.copyToRealmOrUpdate] method.
     * @param model the given [RealmModel].
     * @param json the given [JSONObject].
     */
    fun writeToRealmFromJson(realm: Realm, model: Class<out RealmModel>, json: JSONObject?) {
        if (json != null && isValid(realm)) {
            realm.executeTransaction { realm1 ->
                realm1.createOrUpdateObjectFromJson(model, json)
            }
        }
    }

    /**
     * Deletes all the entries for the provided [RealmModel].
     * @param realm the [Realm] instance to be used.
     * @param model the provided [RealmModel].
     */
    fun deleteTable(realm: Realm, model: Class<out RealmModel>?) {
        if (isValid(realm) && model != null)
            realm.delete(model)
    }

}
