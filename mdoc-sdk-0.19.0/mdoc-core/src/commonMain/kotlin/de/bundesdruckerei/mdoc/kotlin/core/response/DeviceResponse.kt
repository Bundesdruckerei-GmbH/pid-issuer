/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.response

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORTypeMapper
import com.upokecenter.cbor.ICBORConverter
import de.bundesdruckerei.mdoc.kotlin.core.DocType
import de.bundesdruckerei.mdoc.kotlin.core.Document
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.errors.ErrorCode
import de.bundesdruckerei.mdoc.kotlin.core.tstr

typealias DocumentError = Map<DocType, ErrorCode>

typealias DocumentsArray = Array<Document>

typealias DocumentErrorsArray = Array<DocumentError>

class DeviceResponse(
    val version: tstr = "1.0",
    val documents: DocumentsArray? = null,
    val documentErrors: DocumentErrorsArray? = null,
    val status: ResponseStatus
) :
    ICBORable {
    override fun asCBOR(): CBORObject = CBORObject.FromObject(this, cborTypeMapper)

    companion object {
        private val VERSION = "version"
        private val DOCUMENTS = "documents"
        private val DOCUMENT_ERRORS = "documentErrors"
        private val STATUS = "status"

        @Suppress("ObjectLiteralToLambda")
        private val cborConverter = object : ICBORConverter<DeviceResponse> {
            override fun ToCBORObject(obj: DeviceResponse): CBORObject {
                return CBORObject.NewMap().apply {
                    Set(VERSION, obj.version)

                    obj.documents?.let {
                        if (it.isNotEmpty()) {
                            Set(DOCUMENTS, docsArrayConverter.ToCBORObject(it))
                        }
                    }

                    obj.documentErrors?.let {
                        if (it.isNotEmpty()) {
                            Set(DOCUMENT_ERRORS, docErrorsArrayConverter.ToCBORObject(it))
                        }
                    }

                    Set(STATUS, obj.status.value)
                }

            }
        }

        @Suppress("ObjectLiteralToLambda")
        private val docsConverter = object : ICBORConverter<Document> {
            override fun ToCBORObject(obj: Document): CBORObject {
                return CBORObject.NewArray().Add(obj.asCBOR())
            }
        }

        @Suppress("ObjectLiteralToLambda")
        private val docsArrayConverter = object : ICBORConverter<DocumentsArray> {
            override fun ToCBORObject(obj: DocumentsArray): CBORObject {
                return CBORObject.NewArray().apply {
                    obj.forEach {
                        Add(it.asCBOR())
                    }
                }
            }
        }

        @Suppress("ObjectLiteralToLambda")
        private val docErrorsConverter = object : ICBORConverter<DocumentError> {
            override fun ToCBORObject(obj: DocumentError): CBORObject {
                return CBORObject.NewMap().apply {
                    obj.forEach { (docType, errorCode) ->
                        Set(docType, errorCode)
                    }
                }
            }
        }

        @Suppress("ObjectLiteralToLambda")
        private val docErrorsArrayConverter = object : ICBORConverter<DocumentErrorsArray> {
            override fun ToCBORObject(obj: DocumentErrorsArray): CBORObject {
                return CBORObject.NewArray().apply {
                    obj.forEach {
                        Add(docErrorsConverter.ToCBORObject(it))
                    }
                }
            }
        }

        val cborTypeMapper: CBORTypeMapper =
            CBORTypeMapper().AddConverter(DeviceResponse::class.java, cborConverter)


        fun getStatusFromCBOR(cborObject: CBORObject): ResponseStatus {
            return cborObject[STATUS].let { ResponseStatus.valueOf(it.AsInt64Value()) }
        }

        fun fromCBOR(cborObject: CBORObject): DeviceResponse {
            val status = getStatusFromCBOR(cborObject)

            val documentsList = mutableListOf<Document>()
            val documentErrorsList = mutableListOf<DocumentError>()

            val version = cborObject[VERSION].AsString()

            val docs: CBORObject? = cborObject[DOCUMENTS]
            docs?.values?.forEach { documentsCBOR ->
                val documents: Document = Document.fromCBOR(documentsCBOR)

                documentsList.add(documents)
            }

            val docErrors: CBORObject? = cborObject[DOCUMENT_ERRORS]
            docErrors?.values?.forEach { documentErrorsCBOR ->
                val documentErrors: DocumentError = mutableMapOf<DocType, ErrorCode>().apply()
                {
                    documentErrorsCBOR.keys.forEach {
                        this[it.AsString()] = documentErrorsCBOR.get(it).AsInt32Value()
                    }
                }
                documentErrorsList.add(documentErrors)
            }

            if (status == ResponseStatus.OK) {
                return DeviceResponse(
                    version,
                    documentsList.ifEmpty { null }?.toTypedArray(),
                    documentErrorsList.ifEmpty { null }?.toTypedArray(),
                    status
                )
            }

            return DeviceResponse(version, null, null, status)
        }
    }
}
