package jafproductions.jaflist.services

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import jafproductions.jaflist.R
import jafproductions.jaflist.models.AppData
import kotlinx.coroutines.tasks.await
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FirebaseService private constructor(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val googleSignInClient: GoogleSignInClient

    val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, Iso8601DateAdapter())
        .create()

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    fun signOut() {
        googleSignInClient.signOut()
        auth.signOut()
    }

    suspend fun upload(appData: AppData) {
        val uid = auth.currentUser?.uid ?: return
        val jsonString = gson.toJson(appData)
        val docRef = firestore.collection("users").document(uid)
            .collection("data").document("appdata")
        docRef.set(mapOf("json" to jsonString)).await()
    }

    suspend fun download(): AppData? {
        val uid = auth.currentUser?.uid ?: return null
        val docRef = firestore.collection("users").document(uid)
            .collection("data").document("appdata")
        val snapshot = docRef.get().await()
        val jsonString = snapshot.getString("json") ?: return null
        return try {
            gson.fromJson(jsonString, AppData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseService? = null

        fun getInstance(context: Context): FirebaseService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseService(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Custom Gson TypeAdapter that handles ISO 8601 dates including both "Z" and "+00:00" suffixes,
 * compatible with iOS date format.
 */
class Iso8601DateAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {

    private val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    )

    override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return JsonPrimitive(sdf.format(src ?: Date()))
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date {
        val dateStr = json?.asString ?: throw JsonParseException("Null date string")

        // Normalize "+00:00" to "Z" style for parsing
        val normalized = dateStr.replace(Regex("\\+00:00$"), "Z")
            .replace(Regex("([+-]\\d{2}):(\\d{2})$")) { mr ->
                "${mr.groupValues[1]}${mr.groupValues[2]}"
            }

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.isLenient = false
                return sdf.parse(normalized) ?: continue
            } catch (e: Exception) {
                continue
            }
        }

        // Last resort: try with lenient parsing
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.isLenient = true
        return sdf.parse(dateStr) ?: Date()
    }
}
