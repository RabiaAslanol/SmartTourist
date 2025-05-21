package com.example.smarttourist

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageClassifierHelper(context: Context) {

    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context)
        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(model, options)

        val inputShape = interpreter.getInputTensor(0).shape().joinToString()
        val inputType = interpreter.getInputTensor(0).dataType()
        val outputShape = interpreter.getOutputTensor(0).shape().joinToString()
        val outputType = interpreter.getOutputTensor(0).dataType()

        Log.d("TFLiteModel", "Input shape: [$inputShape], type: $inputType")
        Log.d("TFLiteModel", "Output shape: [$outputShape], type: $outputType")
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("mobilenet_yer_turu_modeli.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(bitmap: Bitmap): String {
        return try {
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 5), org.tensorflow.lite.DataType.FLOAT32)
            interpreter.run(inputBuffer, outputBuffer.buffer.rewind())

            val scores = outputBuffer.floatArray
            val labels = listOf("Doğal Yapı", "Müze", "Plaj", "Tarihi Yapı")
            val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: -1

            val tahmin = if (maxIndex != -1) labels[maxIndex] else "Tanımsız"
            Log.d("Tahmin", "Sonuç: $tahmin | Skor: ${scores[maxIndex]}")
            tahmin
        } catch (e: Exception) {
            e.printStackTrace()
            "Tahmin sırasında hata oluştu"
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 224
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }
}
