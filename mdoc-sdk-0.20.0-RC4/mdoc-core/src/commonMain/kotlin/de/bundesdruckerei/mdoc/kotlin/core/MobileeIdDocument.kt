/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORTypeMapper
import com.upokecenter.cbor.ICBORConverter
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerSigned
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable

class MobileeIDdocument(
    val docType: DocType,
    val issuerSigned: IssuerSigned,
    val docPrivateKey: COSEKey?
) : ICBORable {

    override fun asCBOR(): CBORObject = CBORObject.FromObject(this, MobileeIDdocument.cborMapper)

    companion object {
        @Suppress("ObjectLiteralToLambda")
        val cborConverter = object : ICBORConverter<MobileeIDdocument> {
            override fun ToCBORObject(obj: MobileeIDdocument): CBORObject {
                return CBORObject.NewMap().apply {
                    Set("0", obj.docType)
                    Set("1", obj.issuerSigned.asCBOR())
                    obj.docPrivateKey?.let {
                        Set("2", obj.docPrivateKey)
                    }
                }
            }
        }

        private val cborMapper: CBORTypeMapper =
            CBORTypeMapper().AddConverter(MobileeIDdocument::class.java, cborConverter)

        fun fromCBOR(cborObject: CBORObject): MobileeIDdocument {
            val docType = cborObject["0"].AsString()
            val issuerSigned = IssuerSigned.fromCBOR(cborObject["1"])
            val docPrivateKey = cborObject["2"]?.let { COSEKey(it) }
            return MobileeIDdocument(docType, issuerSigned, docPrivateKey)
        }

        fun fromBytes(data: ByteArray): MobileeIDdocument {
            return fromCBOR(CBORObject.DecodeFromBytes(data))
        }
    }
}

fun MobileeIDdocuments.asCBOR(): CBORObject = CBORObject.NewArray().apply {
    forEach { Add(it.asCBOR()) }
}

fun mobileeIDDocumentsFromCBOR(cborObject: CBORObject): MobileeIDdocuments {
    val documents = mutableListOf<MobileeIDdocument>()
    cborObject.values.forEach {
        documents.add(MobileeIDdocument.fromCBOR(it))
    }
    return documents.toTypedArray()
}
