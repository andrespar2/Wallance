package com.andrespar2.wallance.nucleo.seguridad

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Gestión de seguridad de Wallance: PIN local y autenticación biométrica.
 *
 * El PIN nunca se guarda en texto plano: se almacena como huella SHA-256 con
 * sal aleatoria. La clave usada para cifrar respaldos se deriva del PIN con
 * SHA-256 (los respaldos se exportan cifrados con AES/GCM por la capa de
 * respaldos).
 */
object GestorSeguridad {

    /** Calcula la huella SHA-256 de un PIN con sal aleatoria. */
    fun hashPin(
        pin: String,
        sal: ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sal)
        val huella = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return sal.joinToString("") { "%02x".format(it) } + ":" +
            huella.joinToString("") { "%02x".format(it) }
    }

    /** Verifica un PIN contra su huella guardada (comparación constante). */
    fun verificarPin(pin: String, huellaGuardada: String): Boolean {
        if (huellaGuardada.isBlank() || !huellaGuardada.contains(":")) return false
        val partes = huellaGuardada.split(":")
        val sal = partes[0].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val esperada = partes[1]
        val calculada = hashPin(pin, sal).split(":")[1]
        return MessageDigest.isEqual(esperada.toByteArray(), calculada.toByteArray())
    }

    /** Indica si el dispositivo tiene biometría o credencial utilizables. */
    fun puedeUsarBiometria(contexto: Context): Boolean {
        val gestor = BiometricManager.from(contexto)
        return gestor.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Solicita autenticación biométrica (o credencial del dispositivo).
     * El resultado se entrega mediante los callbacks indicados.
     */
    fun autenticar(
        actividad: androidx.fragment.app.FragmentActivity,
        titulo: String = "Desbloquear Wallance",
        subtitulo: String = "Confirma tu identidad",
        onExito: () -> Unit,
        onError: (String) -> Unit,
        onCancelar: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(actividad)
        val prompt = BiometricPrompt(
            actividad,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    onExito()
                }

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    if (codigo == BiometricPrompt.ERROR_USER_CANCELED ||
                        codigo == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onCancelar()
                    } else {
                        onError(mensaje.toString())
                    }
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            prompt.authenticate(info)
        } catch (error: Exception) {
            onError("No fue posible iniciar la autenticación biométrica.")
        }
    }

    /**
     * Clave simétrica derivada del PIN para cifrar respaldos.
     * Derivación local: no requiere servicios del sistema.
     */
    fun claveDeCifrado(pin: String): SecretKey {
        val material = MessageDigest.getInstance("SHA-256")
            .digest(("wallance:" + pin).toByteArray(Charsets.UTF_8))
        return SecretKeySpec(material, "AES")
    }
}
