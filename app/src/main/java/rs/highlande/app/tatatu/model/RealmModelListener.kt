/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.model

import io.realm.Realm
import io.realm.RealmModel
import org.json.JSONException
import org.json.JSONObject

/**
 * Implements the same abstract methods of {@link it.keybiz.lbsapp.corporate.base.HLBaseModel},
 * since Realm does not allow its classes to extend {@link HLModel}.
 *
 * @author mbaldrighi on 10/7/2017.
 */

interface  RealmModelListener {

	/**
	 * Generic RESET method.
	 */
	fun reset()

	/**
	 * Generic READ method.
	 * @param realm the {@link Realm} instance used for the transaction.
	 * @return An {@link Any}.
	 */
	fun read(realm: Realm?): Any?

	/**
	 * READ operation concerning {@link Realm} and made on a given {@link RealmModel} class.
	 * @param model the given {@link RealmModel} class.
	 * @return A {@link RealmModel}.
	 */
	fun read(realm: Realm?, model: Class<out RealmModel>?): RealmModel?

	/**
	 * Since {@link Realm} still does not support lists of primitive, nor nested classes
	 */
	@Throws(JSONException::class)
	fun deserializeStringListFromRealm()

	/**
	 * Since {@link Realm} still does not support lists of primitive, nor nested classes
	 */
	fun serializeStringListForRealm()
	
	/**
	 * Generic WRITE method.
	 * @param realm the {@link Realm} instance used for the transaction.
	 */
	fun write(realm: Realm?)

	/**
	 * Generic WRITE method given the {@link Any} to be written.
	 * @param obj the {@link Any} to be written.
	 */
	fun write(obj: Any?)

	/**
	 * Generic UPDATE operation from given {@link JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 */
	fun write(json: JSONObject?)

	/**
	 * WRITE operation concerning {@link Realm} given the {@link RealmModel} to be written.
	 * @param realm the {@link Realm} instance used for the transaction.
	 * @param model the {@link RealmModel} to be written.
	 */
	fun write(realm: Realm?, model: RealmModel?)

	/**
	 * Generic UPDATE operation.
	 */
	fun update()

	/**
	 * Generic UPDATE operation from given {@link Any}.
	 * @param obj the {@link Any} source of the update.
	 */
	fun update(obj: Any?)

	/**
	 * Generic UPDATE operation from given {@link JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 */
	fun update(json: JSONObject?)

	/**
	 * Generic UPDATE operation.
	 * @return The same object updated.
	 */
	fun updateWithReturn(): RealmModelListener?

	/**
	 * Generic UPDATE operation from given {@link Any}.
	 * @param obj the {@link Any} source of the update.
	 * @return The same object updated.
	 */
	fun updateWithReturn(obj: Any?): RealmModelListener?

	/**
	 * Generic UPDATE operation from given {@link JSONObject}.
	 * @param json the {@link JSONObject} source of the update.
	 * @return The same object updated.
	 */
	fun updateWithReturn(json: JSONObject?): RealmModelListener?
}
