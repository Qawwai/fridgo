package com.cnlab.fridgo.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnlab.fridgo.R
import com.cnlab.fridgo.data.FridgeStore
import com.cnlab.fridgo.data.ProductRepository
import com.cnlab.fridgo.data.ShelfLifePredictor
import com.cnlab.fridgo.databinding.ActivityFridgeBinding
import com.cnlab.fridgo.databinding.DialogAddItemBinding
import com.cnlab.fridgo.model.FridgeItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Home / launcher activity. Shows the fridge sorted by urgency with an animated
 * colored freshness bar per item, an add/edit dialog (with barcode scan, a real
 * expiry date picker, and grams), search, an urgency summary chip, and a
 * saved/wasted stat. Contextual sounds throughout.
 */
class FridgeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFridgeBinding
    private lateinit var adapter: FridgeAdapter
    private var query: String = ""

    private val productRepo = ProductRepository()
    /** The add/edit dialog currently on screen, so a returning scan can fill it. */
    private var activeDialog: DialogAddItemBinding? = null

    private val dateFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    /** Holds the editable expiry for the open dialog. */
    private class ExpiryState(var millis: Long, var manual: Boolean)

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { onBarcodeScanned(it) }
    }

    /** Pick an image from the gallery/files and decode a barcode out of it. */
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { decodeBarcodeFromImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFridgeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Anim.enter(binding.textTitle)
        Anim.enter(binding.textHint, delay = 80)

        adapter = FridgeAdapter(
            items = visibleItems(),
            onDelete = { item, _ -> confirmRemove(item) },
            onClick = { item -> showItemDialog(item) }
        )
        binding.recyclerFridge.adapter = adapter

        binding.fabAdd.setOnClickListener {
            Anim.bounce(binding.fabAdd) { showItemDialog(null) }
        }

        binding.btnWhatCanIMake.setOnClickListener {
            SoundManager.play(SoundManager.Fx.TAP)
            Anim.bounce(binding.btnWhatCanIMake) {
                startActivity(Intent(this, RecipeResultsActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        binding.btnWhatCanIMake.post { Anim.pulse(binding.btnWhatCanIMake) }

        binding.textStats.setOnClickListener { showScoreInfo() }

        binding.navBottom.selectedItemId = R.id.nav_fridge
        binding.navBottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_discover -> { openScreen(DiscoverActivity::class.java); false }
                R.id.nav_plan -> { openScreen(MealPlanActivity::class.java); false }
                R.id.nav_shop -> { openScreen(ShopActivity::class.java); false }
                else -> true
            }
        }

        // Live search filter.
        binding.editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                query = s?.toString().orEmpty().trim()
                refresh()
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        binding.navBottom.selectedItemId = R.id.nav_fridge
        refresh()
    }

    private fun openScreen(target: Class<*>) {
        SoundManager.play(SoundManager.Fx.TAP)
        startActivity(Intent(this, target))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** Items after applying the current search query. */
    private fun visibleItems(): List<FridgeItem> {
        val all = FridgeStore.sortedByUrgency()
        return if (query.isEmpty()) all
        else all.filter { it.name.contains(query, ignoreCase = true) }
    }

    private fun refresh() {
        val items = visibleItems()
        adapter.update(items)
        binding.recyclerFridge.scheduleLayoutAnimation()
        binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        // Urgency summary chip.
        val urgent = FridgeStore.urgentCount()
        val soon = FridgeStore.soonCount()
        binding.chipUrgency.text = when {
            urgent > 0 -> "⚠️ $urgent item${if (urgent > 1) "s" else ""} expiring now"
            soon > 0 -> "$soon item${if (soon > 1) "s" else ""} to use soon"
            else -> "All fresh ✅"
        }

        // Saved / wasted stat (tap for an explanation).
        binding.textStats.text =
            "🌱 ${FridgeStore.saved} saved  ·  🗑️ ${FridgeStore.wasted} wasted  ⓘ"
    }

    // ---- Remove with an explicit used / wasted choice ----

    private fun confirmRemove(item: FridgeItem) {
        SoundManager.play(SoundManager.Fx.TAP)
        AlertDialog.Builder(this)
            .setTitle("Remove ${item.name}?")
            .setMessage("Did you use it in time, or did it go to waste?\nYour choice updates your Saved vs Wasted score.")
            .setPositiveButton("✅ Used it") { _, _ ->
                SoundManager.play(SoundManager.Fx.SUCCESS)
                lifecycleScope.launch { FridgeStore.remove(item, asWasted = false); refresh() }
            }
            .setNegativeButton("🗑️ Wasted") { _, _ ->
                SoundManager.play(SoundManager.Fx.DELETE)
                lifecycleScope.launch { FridgeStore.remove(item, asWasted = true); refresh() }
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showScoreInfo() {
        AlertDialog.Builder(this)
            .setTitle("Your anti-waste score")
            .setMessage(
                "🌱 Saved — items you used before they expired.\n\n" +
                "🗑️ Wasted — items you threw away.\n\n" +
                "Whenever you remove an item, Fridgo asks which it was, so this score reflects how well you're using your food. Cooking a planned meal also counts its ingredients as saved."
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    // ---- Add / Edit (one shared dialog) ----

    private fun showItemDialog(existing: FridgeItem?) {
        if (existing != null) SoundManager.play(SoundManager.Fx.TAP)

        val b = DialogAddItemBinding.inflate(layoutInflater)
        val now = System.currentTimeMillis()

        // Initial expiry: existing item's expiry, else auto from the (empty) name.
        val state = if (existing != null) {
            ExpiryState(existing.expiryEpochMillis, manual = true)
        } else {
            ExpiryState(now + TimeUnit.DAYS.toMillis(ShelfLifePredictor.predict("").days.toLong()), manual = false)
        }

        b.btnScan.setOnClickListener { launchScanner() }
        b.btnPickDate.setOnClickListener { pickExpiryDate(b, state) }
        wireLivePrediction(b, state)

        existing?.let {
            b.editName.setText(it.name)
            b.editQuantity.setText(it.quantity.toString())
            it.grams?.let { g -> b.editGrams.setText(formatGrams(g)) }
        }
        refreshExpiryLabel(b, state)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add to fridge" else "Edit item")
            .setView(b.root)
            .setPositiveButton(if (existing == null) "Add" else "Save") { _, _ ->
                saveItem(b, state, existing)
            }
            .setNegativeButton("Cancel", null)
            .apply {
                if (existing != null) setNeutralButton("Remove") { _, _ -> confirmRemove(existing) }
            }
            .create()

        dialog.setOnDismissListener { if (activeDialog === b) activeDialog = null }
        activeDialog = b
        dialog.show()
    }

    private fun saveItem(b: DialogAddItemBinding, state: ExpiryState, existing: FridgeItem?) {
        val name = b.editName.text.toString().trim()
        if (name.isEmpty()) return
        val qty = b.editQuantity.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val grams = b.editGrams.text.toString().trim().toDoubleOrNull()?.takeIf { it > 0 }

        val purchase = existing?.purchaseEpochMillis ?: System.currentTimeMillis()
        val shelfDays = daysBetween(purchase, state.millis)

        lifecycleScope.launch {
            if (existing == null) {
                FridgeStore.add(FridgeItem(name, purchase, shelfDays, quantity = qty, grams = grams))
            } else {
                FridgeStore.update(
                    existing.copy(name = name, shelfLifeDays = shelfDays, quantity = qty, grams = grams)
                )
            }
            SoundManager.play(SoundManager.Fx.ADD)
            refresh()
        }
    }

    // ---- Barcode scanning ----

    /** Offer camera, a photo/file, or typing the number (camera is unreliable on emulators). */
    private fun launchScanner() {
        val options = arrayOf("📷  Use camera", "🖼️  Pick a photo / file", "⌨️  Type the number")
        AlertDialog.Builder(this)
            .setTitle("Add barcode")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickImageLauncher.launch("image/*")
                    2 -> promptBarcodeNumber()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchCamera() {
        val opts = ScanOptions()
            .setDesiredBarcodeFormats(listOf("EAN_13", "EAN_8", "UPC_A", "UPC_E"))
            .setPrompt("Point at a product barcode")
            .setBeepEnabled(true)
            .setOrientationLocked(false)
        scanLauncher.launch(opts)
    }

    private fun promptBarcodeNumber() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "e.g. 6281007040655"
        }
        AlertDialog.Builder(this)
            .setTitle("Enter barcode number")
            .setView(input)
            .setPositiveButton("Look up") { _, _ ->
                val code = input.text.toString().trim()
                if (code.isNotEmpty()) onBarcodeScanned(code)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Decode a barcode from a chosen image (off the main thread), then look it up. */
    private fun decodeBarcodeFromImage(uri: Uri) {
        Toast.makeText(this, "Reading image…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val code = withContext(Dispatchers.Default) {
                runCatching { decodeBitmap(uri) }.getOrNull()
            }
            if (code != null) {
                onBarcodeScanned(code)
            } else {
                Toast.makeText(
                    this@FridgeActivity,
                    "No barcode found in that image — try a clearer crop or type the number.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun decodeBitmap(uri: Uri): String {
        val bitmap = loadSoftwareBitmap(uri)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A, BarcodeFormat.UPC_E
            )
        )
        return MultiFormatReader().decode(binary, hints).text
    }

    /** Load a mutable software bitmap (getPixels can't read hardware bitmaps). */
    private fun loadSoftwareBitmap(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }

    private fun onBarcodeScanned(code: String) {
        if (activeDialog == null) return
        Toast.makeText(this, "Looking up barcode…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val info = productRepo.lookup(code)
            val bb = activeDialog ?: return@launch
            if (info != null) {
                bb.editName.setText(info.name)   // triggers live prediction
                info.grams?.let { bb.editGrams.setText(formatGrams(it)) }
                SoundManager.play(SoundManager.Fx.SUCCESS)
            } else {
                Toast.makeText(
                    this@FridgeActivity,
                    "Couldn't find that barcode — type the name in.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---- Expiry helpers ----

    private fun pickExpiryDate(b: DialogAddItemBinding, state: ExpiryState) {
        val cal = Calendar.getInstance().apply { timeInMillis = state.millis }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                state.millis = picked.timeInMillis
                state.manual = true
                refreshExpiryLabel(b, state)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        }.show()
    }

    private fun refreshExpiryLabel(b: DialogAddItemBinding, state: ExpiryState) {
        b.textExpiry.text = dateFmt.format(state.millis)
        val days = daysBetween(System.currentTimeMillis(), state.millis)
        b.textExpiryBasis.text = if (state.manual) {
            "Set manually · $days day${if (days == 1) "" else "s"} from now"
        } else {
            "Auto-set from the item name · keeps ${ShelfLifePredictor.humanize(days)}"
        }
    }

    private fun wireLivePrediction(b: DialogAddItemBinding, state: ExpiryState) {
        b.editName.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val name = s?.toString().orEmpty().trim()
                if (name.isEmpty()) {
                    b.textPredicted.text = "Type a name to auto-predict shelf life"
                    return
                }
                val p = ShelfLifePredictor.predict(name)
                b.textPredicted.text = "Auto: keeps ${ShelfLifePredictor.humanize(p.days)}"
                // While the user hasn't overridden the date, keep it in sync.
                if (!state.manual) {
                    state.millis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(p.days.toLong())
                    refreshExpiryLabel(b, state)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, bb: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, bb: Int, c: Int) {}
        })
    }

    /** Whole days between two instants, at least 1. */
    private fun daysBetween(fromMillis: Long, toMillis: Long): Int =
        max(1, ((toMillis - fromMillis).toDouble() / TimeUnit.DAYS.toMillis(1)).roundToInt())

    private fun formatGrams(g: Double): String =
        if (g % 1.0 == 0.0) g.toLong().toString() else "%.1f".format(g)
}
