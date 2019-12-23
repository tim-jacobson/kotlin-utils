package eu.tjacobson.kotlin.utils.delegates.map

import java.lang.NullPointerException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Default means to provide access to the underlying map object to reference when performing delegation
 */
interface Mappable {
    val map: MutableMap<String, Any?>
}

/**
 * Provides a means to delegate a property to a underlying map through a [Mappable] interface.
 * Delegations can expect to throw [NullPointerException] when underlying key is missing or its value is null
 * Nullable fields are not able to be delegated to.
 *
 * @see setByNullableMap
 */
fun <V : Any> setByMap(alternativeName: String? = null): ReadWriteProperty<Mappable,V> {
    return MutableMapDelegate(alternativeName)
}

/**
 * Provides a means to delegate a property to a underlying map through a [Mappable] interface.
 * Delegations may receive a null object response if key is missing or its value is null.
 *
 * If delegation is assigned to a non-nullable field, exception will be thrown at runtime when accessing the value
 *
 * @see setByMap
 */
fun <V : Any?> setByNullableMap(): ReadWriteProperty<Mappable,V> {
    return NullableMutableMapDelegate(null)
}


class MutableMapDelegate<T : Any>(private val alternativeName: String?) : ReadWriteProperty<Mappable, T> {

    override fun getValue(thisRef: Mappable, property: KProperty<*>): T {
        return (thisRef.map[alternativeName ?: property.name]).also {
            @Suppress("SENSELESS_COMPARISON")
            if (it == null) throw NullPointerException("Map value [${property.name}] of [$thisRef] is either null or missing for non-null field!")
        } as T
    }

    override fun setValue(thisRef: Mappable, property: KProperty<*>, value: T) {
        thisRef.map[alternativeName ?: property.name] = value
    }
}

class NullableMutableMapDelegate<T : Any?>(private val alternativeName: String?) : ReadWriteProperty<Mappable, T> {
    override fun getValue(thisRef: Mappable, property: KProperty<*>): T {
        return (thisRef.map[alternativeName ?: property.name] as T).also { if (!property.returnType.isMarkedNullable) throw NullPointerException("Attempting to map key [${property.name}] to non-nullable field!")}
    }

    override fun setValue(thisRef: Mappable, property: KProperty<*>, value: T) {
        thisRef.map[alternativeName ?: property.name] = value
    }
}
