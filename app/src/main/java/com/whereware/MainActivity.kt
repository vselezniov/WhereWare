package com.whereware

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.play_services.mlkit.document.text.DocumentTextRecognition
import com.google.android.gms.play_services.mlkit.document.text.DocumentTextRecognizerOptions

class MainActivity : AppCompatActivity() {

    // Локальный распознаватель документов Google (работает офлайн)
    private val documentRecognizer = DocumentTextRecognition.getClient(
        DocumentTextRecognizerOptions.Builder().build()
    )

    private lateinit var resultTextView: TextView

    // Лаунчер для открытия галереи смартфона
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            resultTextView.text = "Обработка изображения..."
            try {
                val bitmap = loadBitmapFromUri(uri)
                if (bitmap != null) {
                    scanInvoiceLocally(bitmap)
                } else {
                    resultTextView.text = "Ошибка загрузки изображения"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resultTextView.text = "Ошибка: ${e.localizedMessage}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаем крупный интерфейс прямо в коде
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            padding = 32
        }

        val selectButton = Button(this).apply {
            text = "Выбрать фото накладной"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        resultTextView = TextView(this).apply {
            text = "Здесь появится распознанный текст накладной..."
            textSize = 18f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            movementMethod = android.text.method.ScrollingMovementMethod()
        }

        selectButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        rootLayout.addView(selectButton)
        rootLayout.addView(resultTextView)
        setContentView(rootLayout)
    }

    // Передаем картинку в локальный ML Kit для документов
    private fun scanInvoiceLocally(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        documentRecognizer.process(image)
            .addOnSuccessListener { documentResult ->
                val fullText = documentResult.text
                if (fullText.isNotEmpty()) {
                    resultTextView.text = fullText
                } else {
                    resultTextView.text = "Текст на изображении не найден."
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                resultTextView.text = "Ошибка распознавания: ${e.localizedMessage}"
            }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}
