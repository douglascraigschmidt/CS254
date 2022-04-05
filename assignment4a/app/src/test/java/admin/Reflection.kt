package admin

import org.junit.Assert
import java.lang.reflect.Field
import java.lang.reflect.Modifier.*
import kotlin.reflect.KClass

inline fun <reified T> Any.value(vararg types: Class<*>): T {
    val field = if (types.isEmpty()) {
        this::class.java.findField(T::class.java)
    } else {
        this::class.java.findField(*types)
    }
    return field.runWithAccess {
        field.get(this@value) as T
    }
}

inline fun <reified T> Any.getField(name: String): T {
    return getField(name, T::class.java)
}

inline fun <reified T> Any.firstField(): Field? =
        javaClass.declaredFields.firstOrNull { field: Field ->
            field.type == T::class.java
        }

//TODO
inline fun <reified C, reified T> getStaticValue(name: String): T {
    val field = C::class.java.findField(T::class.java, name)
    return field.runWithAccess {
        get(null) as T
    }
}

inline fun <reified T> Any.getJavaPrimitiveField(name: String, type: Class<*>): T {
    return getField(name, type)
}

inline fun <reified T> Any.setField(name: String, value: T?) {
    setField(name, value, T::class.java)
}

inline fun <reified T> Any.setField(value: T) {
    javaClass.findField(T::class.java, "").let {
        it.runWithAccess {
            set(this, value)
        }
    }
}

inline fun <reified T> Any.setField(value: T, type: Class<*>, name: String = "") {
    javaClass.findField(type, name).let {
        it.runWithAccess {
            it.set(this@setField, value)
        }
    }
}

@SuppressWarnings("deprecation")
inline fun <reified T> Any.getField(name: String, type: Class<*>): T {
    return javaClass.findField(type, name).runWithAccess {
        get(this@getField) as T
    }
}

inline fun <reified T> T.injectInto(parent: Any, name: String = ""): T {
    val type = when (this) {
        is Int -> Int::class.javaPrimitiveType
        is Float -> Float::class.javaPrimitiveType
        is Double -> Double::class.javaPrimitiveType
        is Short -> Short::class.javaPrimitiveType
        else -> T::class.java
    }
    val field = parent::class.java.findField(type!!, name)
    parent.setField(this, type, field.name)
    return this
}

inline fun <reified T> T.injectInto(parent: Any, vararg types: Class<*>): T {
    val field = parent::class.java.findField(*types)
    parent.setField(this, field.type, field.name)
    return this
}

@SuppressWarnings("deprecation")
inline fun <reified T> Any.setJavaPrimitiveField(name: String, value: T) {
    val javaPrimitiveType = when (value) {
        is Int -> Int::class.javaPrimitiveType
        is Float -> Float::class.javaPrimitiveType
        is Double -> Double::class.javaPrimitiveType
        is Short -> Short::class.javaPrimitiveType
        else -> throw Exception("value is not a have an equivalent Java primitive type")
    }

    javaClass.findField(javaPrimitiveType!!, name).runWithAccess {
        set(this@setJavaPrimitiveField, value)
    }
}

fun Class<*>.findField(vararg types: Class<*>): Field {
    types.forEach { type ->
        try {
            return findField(type, "")
        } catch (t: Throwable) {
        }
    }
    throw Exception("Unable to field types $types in $this.name")
}

fun Class<*>.findField(type: Class<*>, name: String = ""): Field {
    try {
        return declaredFields.firstOrNull {
            val wasAccessible = it.isAccessible
            try {
                it.isAccessible = true
                (name.isBlank() || it.name == name) && type.isAssignableFrom(it.type)
            } finally {
                it.isAccessible = wasAccessible
            }
        } ?: superclass!!.findField(type, name)
    } catch (e: Exception) {
        throw Exception("Class field $name with type $type does not exist")
    }
}

inline fun <reified T> Any.primitiveValue(type: KClass<*>, name: String = ""): T {
    return javaClass.findField(type.javaPrimitiveType!!, name).let {
        val wasAccessible = it.isAccessible
        it.isAccessible = true
        val result = it.get(this)
        it.isAccessible = wasAccessible
        result as T
    }
}

inline fun Any.primitiveValueHasModifier(modifier: Int, type: KClass<*>, name: String = ""): Boolean {
    return javaClass.findField(type.javaPrimitiveType!!, name).let {
        val wasAccessible = it.isAccessible
        it.isAccessible = true
        val result = (it.modifiers and modifier) != 0
        it.isAccessible = wasAccessible
        result
    }
}

@SuppressWarnings("deprecation")
inline fun <reified T> Any.setField(name: String, value: T, type: Class<*>?) {
    javaClass.findField(type!!, name).runWithAccess {
        set(this@setField, value)
    }
}

var Any.outerClass: Any
    get() = javaClass.superclass!!
    @SuppressWarnings("deprecation")
    set(value) {
        javaClass.getDeclaredField("this$0").runWithAccess {
            set(this@outerClass, value)
        }
    }

fun Any.reflectiveEquals(expected: Any): Boolean {
    val fields = javaClass.declaredFields
    val expectedFields = expected.javaClass.declaredFields
    Assert.assertEquals(expectedFields.size, fields.size)
    for (i in 0..fields.lastIndex) {
        if (fields[i] != expectedFields[i]) {
            return false
        }
    }

    return true
}

inline fun <T> Field.runWithAccess(block: Field.() -> T): T {
    val wasAccessible = isAccessible
    isAccessible = true
    val result = block()
    isAccessible = wasAccessible
    return result
}

inline fun <reified T> Any.field(): Field? =
        javaClass.declaredFields.firstOrNull { field: Field ->
            field.type == T::class.java
        }

fun Field.hasModifiers(vararg modifiers: String): Boolean {
    return modifiers.map {
        modifierFromString(it)
    }.reduce { acc, modifier ->
        acc or modifier
    } and this.modifiers != 0
}

fun modifierFromString(modifier: String): Int {
    return when (modifier) {
        "public" -> PUBLIC
        "protected" -> PROTECTED
        "private" -> PRIVATE
        "static" -> STATIC
        "final" -> FINAL
        "transient" -> TRANSIENT
        "volatile" -> VOLATILE
        else -> 0x0
    }
}