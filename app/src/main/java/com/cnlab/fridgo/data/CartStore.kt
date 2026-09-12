package com.cnlab.fridgo.data

/**
 * The shopping cart. In-memory and device-local — DummyJSON's cart endpoints are
 * themselves simulated, so there is nothing durable to sync to; the cart simply
 * lives for the session. Reads are synchronous (like [FridgeStore]) so adapters
 * can render directly; screens re-read in onResume to reflect changes.
 */
object CartStore {

    /** One cart line: a product and how many of it the user wants. */
    data class CartLine(val product: ShopProductDto, var quantity: Int)

    private val lines = mutableListOf<CartLine>()

    /** Add one of [product], merging into an existing line if already present. */
    fun add(product: ShopProductDto, quantity: Int = 1) {
        val existing = lines.firstOrNull { it.product.id == product.id }
        if (existing != null) existing.quantity += quantity
        else lines.add(CartLine(product, quantity))
    }

    /** Set the quantity for a line; removing it if [quantity] drops to 0 or less. */
    fun setQuantity(productId: Int, quantity: Int) {
        val line = lines.firstOrNull { it.product.id == productId } ?: return
        if (quantity <= 0) lines.remove(line) else line.quantity = quantity
    }

    fun remove(productId: Int) {
        lines.removeAll { it.product.id == productId }
    }

    fun clear() = lines.clear()

    /** A snapshot copy of the current lines. */
    fun lines(): List<CartLine> = lines.map { it.copy() }

    /** Total number of individual items (sum of quantities) — used for the badge. */
    fun count(): Int = lines.sumOf { it.quantity }

    /** Whether the cart has anything in it. */
    fun isEmpty(): Boolean = lines.isEmpty()

    /** Money total across all lines. */
    fun subtotal(): Double = lines.sumOf { it.product.price * it.quantity }
}
