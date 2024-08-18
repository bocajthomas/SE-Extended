package me.rhunk.snapenhance.e2ee

import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.bridge.e2ee.E2eeInterface
import me.rhunk.snapenhance.bridge.e2ee.EncryptionResult
import me.rhunk.snapenhance.core.util.EvictingMap
import org.bouncycastle.pqc.crypto.crystals.kyber.*
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class E2EEImplementation (
    private val context: RemoteSideContext
) : E2eeInterface.Stub() {
    private val kyberDefaultParameters = KyberParameters.kyber1024
    private val secureRandom = SecureRandom()

    private val e2eeFolder by lazy { File(context.androidContext.filesDir, "e2ee").also {
        if (!it.exists()) it.mkdirs()
    }}
    private val pairingFolder by lazy { File(context.androidContext.cacheDir, "e2ee-pairing").also {
        if (!it.exists()) it.mkdirs()
        else {
            it.deleteRecursively()
            it.mkdirs()
        }
    } }

    private val sharedSecretKeyCache = EvictingMap<String, ByteArray?>(100)

    fun storeSharedSecretKey(friendId: String, key: ByteArray) {
        File(e2eeFolder, "$friendId.key").writeBytes(key)
        sharedSecretKeyCache[friendId] = key
    }

    fun getSharedSecretKey(friendId: String): ByteArray? {
        return sharedSecretKeyCache.getOrPut(friendId) {
            runCatching {
                File(e2eeFolder, "$friendId.key").readBytes()
            }.onFailure {
                context.log.warn("Failed to read shared secret key: ${it.message}")
            }.getOrNull()
        }
    }

    fun deleteSharedSecretKey(friendId: String) {
        File(e2eeFolder, "$friendId.key").delete()
    }

    override fun createKeyExchange(friendId: String): ByteArray? {
        val keyPairGenerator = KyberKeyPairGenerator()
        keyPairGenerator.init(
            KyberKeyGenerationParameters(secureRandom, kyberDefaultParameters)
        )
        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKey = keyPair.public as KyberPublicKeyParameters
        val privateKey = keyPair.private as KyberPrivateKeyParameters
        runCatching {
            File(pairingFolder, "$friendId.private").writeBytes(privateKey.encoded)
            File(pairingFolder, "$friendId.public").writeBytes(publicKey.encoded)
        }.onFailure {
            context.log.error("Failed to write private key to file", it)
            return null
        }
        return publicKey.encoded
    }

    override fun acceptPairingRequest(friendId: String, publicKey: ByteArray): ByteArray? {
        val kemGen = KyberKEMGenerator(secureRandom)
        val encapsulatedSecret = runCatching {
            kemGen.generateEncapsulated(
                KyberPublicKeyParameters(
                    kyberDefaultParameters,
                    publicKey
                )
            )
        }.onFailure {
            context.log.error("Failed to generate encapsulated secret", it)
            return null
        }.getOrThrow()

        runCatching {
            storeSharedSecretKey(friendId, encapsulatedSecret.secret)
        }.onFailure {
            context.log.error("Failed to store shared secret key", it)
            return null
        }
        return encapsulatedSecret.encapsulation
    }

    override fun acceptPairingResponse(friendId: String, encapsulatedSecret: ByteArray): Boolean {
        val privateKey = runCatching {
            val secretKey = File(pairingFolder, "$friendId.private").readBytes()
            object: KyberPrivateKeyParameters(kyberDefaultParameters, null, null, null, null, null) {
                override fun getEncoded() = secretKey
            }
        }.onFailure {
            context.log.error("Failed to read private key from file", it)
            return false
        }.getOrThrow()

        val kemExtractor = KyberKEMExtractor(privateKey)
        val sharedSecret = runCatching {
            kemExtractor.extractSecret(encapsulatedSecret)
        }.onFailure {
            context.log.error("Failed to extract shared secret", it)
            return false
        }.getOrThrow()

        runCatching {
            storeSharedSecretKey(friendId, sharedSecret)
        }.onFailure {
            context.log.error("Failed to store shared secret key", it)
            return false
        }

        return true
    }

    override fun friendKeyExists(friendId: String): Boolean {
        return File(e2eeFolder, "$friendId.key").exists()
    }

    override fun getSecretFingerprint(friendId: String): String? {
        val sharedSecretKey = getSharedSecretKey(friendId) ?: return null

        return MessageDigest.getInstance("SHA-256")
            .digest(sharedSecretKey)
            .joinToString("") { "%02x".format(it) }
            .chunked(5)
            .joinToString(" ")
    }

    override fun encryptMessage(friendId: String, message: ByteArray): EncryptionResult? {
        val encryptionKey = getSharedSecretKey(friendId) ?: return null

        return runCatching {
            val iv = ByteArray(16).apply { secureRandom.nextBytes(this) }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(iv))
            EncryptionResult().apply {
                this.iv = iv
                this.ciphertext = cipher.doFinal(message)
            }
        }.onFailure {
            context.log.error("Failed to encrypt message for $friendId", it)
        }.getOrNull()
    }

    override fun decryptMessage(friendId: String, message: ByteArray, iv: ByteArray): ByteArray? {
        val encryptionKey = getSharedSecretKey(friendId) ?: return null

        return runCatching {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(iv))
            cipher.doFinal(message)
        }.onFailure {
            context.log.warn("Failed to decrypt message for $friendId")
            return null
        }.getOrNull()
    }
}