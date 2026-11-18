package neth.iecal.questphone

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.platform.Fp16Conversions
import neth.iecal.questphone.app.screens.quest.setup.ai_snap.model.DEFAULT_MODEL_CATALOG
import neth.iecal.questphone.app.screens.quest.setup.ai_snap.model.resolveActiveLocalModelId
import neth.iecal.questphone.app.screens.quest.setup.ai_snap.model.resolveModelCatalog
import neth.iecal.questphone.core.utils.evaluateZeroShotFeatures
import neth.iecal.questphone.core.utils.extractLogitsFromBuffers
import neth.iecal.questphone.core.utils.sanitizeZeroShotQueries
import neth.iecal.questphone.core.utils.sigmoidProbability
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import java.nio.FloatBuffer

class OnDeviceAiPipelineTest {

    @Test
    fun defaultModelCatalog_hasOnlineAndAllQuantizedSiglip2Variants() {
        assertTrue(DEFAULT_MODEL_CATALOG.size >= 6)
        assertEquals("online", DEFAULT_MODEL_CATALOG.first().id)

        val offlineModels = DEFAULT_MODEL_CATALOG.filter { it.id != "online" }
        val expectedIds = setOf(
            "SIGLIP2_UINT8",
            "SIGLIP2_INT8",
            "SIGLIP2_Q4F16",
            "SIGLIP2_FP16",
            "SIGLIP2_FULL"
        )
        assertEquals(expectedIds, offlineModels.map { it.id }.toSet())

        for (model in offlineModels) {
            assertNotNull("URL must not be null for ${model.id}", model.url)
            assertTrue(
                "URL for ${model.id} must use https and end with .onnx",
                model.url.startsWith("https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/resolve/main/onnx/") &&
                    model.url.endsWith(".onnx")
            )
            assertTrue("Version must not be blank for ${model.id}", model.version.isNotBlank())
            assertTrue("Accuracy must not be blank for ${model.id}", model.accuracy.isNotBlank())
        }
    }

    @Test
    fun resolveModelCatalog_truncatedUpstreamJson_fallsBackToDefaultCatalog() {
        val truncatedRemoteJson = """
            [
              {
                "id": "online",
                "url": "",
                "sizeMB": 0,
                "accuracy": "HIGHEST",
                "version": "22/04/2025"
              },
              {
                "id": "SIGLIP2_Q4F16",
                "url": "https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/resolve/main/onnx/model_q4f16.onnx",
                "sizeMB": 497,
                "accuracy": "Balanced",
                "version": "22/04/2025"
              },
              {
                "id": "SIGLIP2_FP16",
                "url": "https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/resolve/main/onnx/model_f
        """.trimIndent()

        val catalog = resolveModelCatalog(truncatedRemoteJson)
        assertEquals(DEFAULT_MODEL_CATALOG, catalog)
    }

    @Test
    fun resolveModelCatalog_nullOrBlankOrEmptyArray_fallsBackToDefaultCatalog() {
        assertEquals(DEFAULT_MODEL_CATALOG, resolveModelCatalog(null))
        assertEquals(DEFAULT_MODEL_CATALOG, resolveModelCatalog(""))
        assertEquals(DEFAULT_MODEL_CATALOG, resolveModelCatalog("   "))
        assertEquals(DEFAULT_MODEL_CATALOG, resolveModelCatalog("[]"))
    }

    @Test
    fun resolveModelCatalog_validPartialRemoteJson_mergesWithDefaultCatalog() {
        val remoteJson = """
            [
              {
                "id": "online",
                "url": "",
                "sizeMB": 0,
                "accuracy": "HIGHEST",
                "version": "01/05/2025"
              },
              {
                "id": "CUSTOM_SIGLIP2",
                "url": "https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/resolve/main/onnx/model_q4.onnx",
                "sizeMB": 300,
                "accuracy": "Fast",
                "version": "01/05/2025"
              }
            ]
        """.trimIndent()

        val catalog = resolveModelCatalog(remoteJson)
        assertEquals("online", catalog.first().id)
        assertEquals("01/05/2025", catalog.first().version)
        assertTrue(catalog.any { it.id == "CUSTOM_SIGLIP2" })
        assertTrue(catalog.any { it.id == "SIGLIP2_UINT8" })
        assertTrue(catalog.any { it.id == "SIGLIP2_Q4F16" })
    }

    @Test
    fun resolveActiveLocalModelId_handlesOnlineSelectionAndDownloadingAndDiskModels() {
        assertNull(resolveActiveLocalModelId("online", null, emptySet()))
        assertNull(resolveActiveLocalModelId(null, null, emptySet()))

        assertEquals(
            "SIGLIP2_UINT8",
            resolveActiveLocalModelId("online", null, setOf("SIGLIP2_UINT8"))
        )
        assertEquals(
            "SIGLIP2_Q4F16",
            resolveActiveLocalModelId("SIGLIP2_Q4F16", null, setOf("SIGLIP2_UINT8", "SIGLIP2_Q4F16"))
        )
        assertEquals(
            "SIGLIP2_INT8",
            resolveActiveLocalModelId("online", "SIGLIP2_INT8", emptySet())
        )
    }

    @Test
    fun sanitizeZeroShotQueries_filtersBlanksAndFallsBackToTaskDescription() {
        assertEquals(
            listOf("a clean desk", "a laptop"),
            sanitizeZeroShotQueries(listOf("  a clean desk ", "", "   ", "a laptop"), "Organize desk")
        )
        assertEquals(
            listOf("Organize desk"),
            sanitizeZeroShotQueries(listOf("", "   "), "  Organize desk  ")
        )
        assertTrue(sanitizeZeroShotQueries(emptyList(), "   ").isEmpty())
    }

    @Test
    fun sigmoidProbability_computesExpectedValues() {
        assertEquals(0.5f, sigmoidProbability(0f), 1e-5f)
        assertTrue(sigmoidProbability(5f) > 0.99f)
        assertTrue(sigmoidProbability(-5f) < 0.01f)
        assertEquals(0f, sigmoidProbability(-100f), 1e-6f)
        assertEquals(1f, sigmoidProbability(100f), 1e-6f)
    }

    @Test
    fun evaluateZeroShotFeatures_explicitFeaturesAllAboveThreshold_returnsCleanDetectedListAndDetailedReason() {
        val queries = listOf("a clean desk", "a laptop")
        // Logits of 0.0 -> probability 0.50 >= 0.08 threshold
        val logits = floatArrayOf(0.0f, 1.2f)

        val (detected, result) = evaluateZeroShotFeatures(
            queries = queries,
            hasExplicitFeatures = true,
            taskDescription = "Organize workspace",
            logitsArray = logits
        )

        assertEquals(listOf("a clean desk", "a laptop"), detected)
        assertTrue(result.isValid)
        assertTrue(result.reason.contains("[OK] a clean desk (Confidence: 50.0%)"))
        assertTrue(result.reason.contains("[OK] a laptop"))
    }

    @Test
    fun evaluateZeroShotFeatures_explicitFeaturesOneBelowThreshold_returnsInvalidWithScores() {
        val queries = listOf("a clean desk", "a green plant")
        // -5.0f -> prob ~0.0067 < 0.08 threshold
        val logits = floatArrayOf(0.5f, -5.0f)

        val (detected, result) = evaluateZeroShotFeatures(
            queries = queries,
            hasExplicitFeatures = true,
            taskDescription = "Organize workspace with plant",
            logitsArray = logits
        )

        assertEquals(listOf("a clean desk"), detected)
        assertFalse(result.isValid)
        assertTrue(result.reason.contains("Could not confidently verify all required features"))
        assertTrue(result.reason.contains("a green plant: 0.7%"))
    }

    @Test
    fun evaluateZeroShotFeatures_emptyLogitsOrQueries_handlesSafelyWithoutCrash() {
        val (detected1, result1) = evaluateZeroShotFeatures(
            queries = listOf("a clean desk"),
            hasExplicitFeatures = false,
            taskDescription = "a clean desk",
            logitsArray = floatArrayOf()
        )
        assertTrue(detected1.isEmpty())
        assertFalse(result1.isValid)

        val (detected2, result2) = evaluateZeroShotFeatures(
            queries = emptyList(),
            hasExplicitFeatures = true,
            taskDescription = "a clean desk",
            logitsArray = floatArrayOf(1.0f)
        )
        assertTrue(detected2.isEmpty())
        assertFalse(result2.isValid)
    }

    @Test
    fun extractLogitsFromBuffers_handlesFloatDirectBufferAndFp16AndBf16AndDouble() {
        val expected = floatArrayOf(-2.5f, 0.0f, 1.25f)

        // 1. Direct FloatBuffer (simulating JNI OnnxTensor.floatBuffer without backing array)
        val directFloatBuf = ByteBuffer.allocateDirect(expected.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        directFloatBuf.put(expected)
        directFloatBuf.flip()
        val fromFloat = extractLogitsFromBuffers(
            type = OnnxJavaType.FLOAT,
            floatBuffer = directFloatBuf,
            shortBuffer = null
        )
        assertArrayEquals(expected, fromFloat, 1e-5f)

        // 2. FLOAT16 output tensor (SIGLIP2_Q4F16 and SIGLIP2_FP16 return floatBuffer = null, shortBuffer != null)
        val fp16ShortBuf = Fp16Conversions.convertFloatBufferToFp16Buffer(FloatBuffer.wrap(expected))
        val fromFp16 = extractLogitsFromBuffers(
            type = OnnxJavaType.FLOAT16,
            floatBuffer = null,
            shortBuffer = fp16ShortBuf
        )
        assertArrayEquals(expected, fromFp16, 1e-3f)

        // 3. BFLOAT16 output tensor (floatBuffer = null, shortBuffer != null)
        val bf16ShortBuf = Fp16Conversions.convertFloatBufferToBf16Buffer(FloatBuffer.wrap(expected))
        val fromBf16 = extractLogitsFromBuffers(
            type = OnnxJavaType.BFLOAT16,
            floatBuffer = null,
            shortBuffer = bf16ShortBuf
        )
        assertArrayEquals(expected, fromBf16, 1e-2f)

        // 4. DOUBLE output tensor
        val doubleBuf = DoubleBuffer.wrap(doubleArrayOf(-2.5, 0.0, 1.25))
        val fromDouble = extractLogitsFromBuffers(
            type = OnnxJavaType.DOUBLE,
            floatBuffer = null,
            shortBuffer = null,
            doubleBuffer = doubleBuf
        )
        assertArrayEquals(expected, fromDouble, 1e-5f)

        // 5. Missing buffer throws IllegalStateException
        assertThrows(IllegalStateException::class.java) {
            extractLogitsFromBuffers(
                type = OnnxJavaType.FLOAT16,
                floatBuffer = null,
                shortBuffer = null
            )
        }
    }
}
