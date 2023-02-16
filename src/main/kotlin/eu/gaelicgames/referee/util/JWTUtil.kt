package eu.gaelicgames.referee.util

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.nimbusds.jose.jwk.RSAKey
import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit
import java.util.Base64
import java.util.concurrent.TimeUnit

object JWTUtil {
    val issuer: String
    val jwkProvider: JwkProvider
    val realm : String
    val privateKey : PrivateKey
    val publicKey : PublicKey
    val refereeAudience = GGERefereeConfig.serverUrl+"/referee"
    const val userClaimKey = "userid"
    val expirationDuration: Duration = Duration.ofMinutes(15)

    init {
        issuer = GGERefereeConfig.serverUrl
        //val privateKey= GGERefereeConfig.jwtPrivateKey
        val audience = GGERefereeConfig.serverUrl + "/api"
        realm = " Access to /api"
        jwkProvider = JwkProviderBuilder(issuer)
            .cached(10,24, TimeUnit.MINUTES)
            .rateLimited(10,1, TimeUnit.MINUTES)
            .build()
        privateKey = loadPrivateKey()
        publicKey = loadPublicKey()
        println(publicKey)

    }

    fun generateMissingJWK() {

        val kpGen = KeyPairGenerator.getInstance("RSA")
        kpGen.initialize(2048)
        val kp = kpGen.genKeyPair()

        val publicKey = kp.public as RSAPublicKey
        val jwk = RSAKey.Builder(publicKey).build()

        File("data/jwk.json").writeText(jwk.toJSONString())
        val private = Base64.getEncoder().encode(kp.private.encoded)
        File("data/jwt-private.pem").writeText(String(private))
    }


    fun loadPublicKey(): PublicKey {
        val file = File("data/jwk.json")
        if(!file.exists()) {
            generateMissingJWK()
        }
        val key = RSAKey.parse(file.readText()).toPublicKey()

        return key
    }



    fun loadPrivateKey(): PrivateKey {
        val file = File("data/jwt-private.pem")
        if(!file.exists()) {
            generateMissingJWK()
        }
        val pkS = file.readText()
        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(pkS))
        return KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)
    }

}