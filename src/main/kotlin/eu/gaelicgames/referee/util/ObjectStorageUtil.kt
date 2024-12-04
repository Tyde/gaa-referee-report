package eu.gaelicgames.referee.util

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.url.Url

object ObjectStorage {
    private val bucket = GGERefereeConfig.objectStorageBucket


    private fun client(): S3Client {
        return S3Client {
            endpointUrl = Url.parse(GGERefereeConfig.objectStorageEndpoint)
            credentialsProvider = GGERefereeConfig.objectStorageCredentialProvider
            region = "eu-central-1" //This is a region that is actually not used as I have my custom endpoint anyway.
            forcePathStyle = true
        }
    }
    suspend fun uploadObject(key: String, data: ByteStream):Result<Unit> {
        val request = PutObjectRequest {
            this.bucket = ObjectStorage.bucket
            this.key = key
            this.body = data
        }

        val response = kotlin.runCatching {  client().use { s3 ->
            s3.putObject(request)
        }}

        return response.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun uploadObject(key: String, data: ByteArray):Result<Unit> {
        return uploadObject(key, ByteStream.fromBytes(data))
    }

    suspend fun getObject(key: String):Result<ByteArray> {
        val getObjectRequest = GetObjectRequest {
            this.bucket = ObjectStorage.bucket
            this.key = key
        }

        val response = kotlin.runCatching { client().use { s3 ->
            s3.getObject(getObjectRequest) { resp ->
                resp.body?.toByteArray()
            }
        }}

        return response.fold(
            onSuccess = {
                if (it == null)
                    Result.failure(Exception("No data returned"))
                else
                    Result.success(it)
                        },
            onFailure = { Result.failure(it) }
        )
    }
}
