package com.walletconnect.android.impl.json_rpc.data

import com.squareup.moshi.Moshi
import com.walletconnect.android.internal.common.JsonRpcResponse
import com.walletconnect.android.internal.common.SerializableJsonRpc
import com.walletconnect.android.internal.common.model.ClientParams
import com.walletconnect.android.internal.common.model.JsonRpcClientSync
import com.walletconnect.android.internal.common.wcKoinApp
import kotlin.reflect.KClass

class JsonRpcSerializer(
    val serializerEntries: Set<KClass<*>>,
    val deserializerEntries: Map<String, KClass<*>>,
) {
    val moshi: Moshi
        get() = wcKoinApp.koin.getAll<Moshi.Builder>().first().build()

    fun deserialize(method: String, json: String): ClientParams? {
        val type = deserializerEntries[method] ?: return null
        val deserializedObject = tryDeserialize(json, type) ?: return null

        return if (deserializedObject::class == type && deserializedObject is JsonRpcClientSync<*>) {
            deserializedObject.params
        } else {
            null
        }
    }

    fun serialize(payload: SerializableJsonRpc): String? {
        lateinit var payloadType: KClass<*>
        return when {
            payload is JsonRpcResponse.JsonRpcResult -> trySerialize(payload)
            payload is JsonRpcResponse.JsonRpcError -> trySerialize(payload)
            serializerEntries.any { type: KClass<*> ->
                payloadType = type
                payload::class == type
            } -> trySerialize(payload, payloadType)
            else -> null
        }
    }

    inline fun <reified T> tryDeserialize(json: String): T? = runCatching { moshi.adapter(T::class.java).fromJson(json) }.getOrNull()
    private fun tryDeserialize(json: String, type: KClass<*>): Any? = runCatching { moshi.adapter(type.java).fromJson(json) }.getOrNull()
    private inline fun <reified T> trySerialize(type: T): String = moshi.adapter(T::class.java).toJson(type)
    private fun trySerialize(payload: Any, type: KClass<*>): String = moshi.adapter<Any>(type.java).toJson(payload)
}