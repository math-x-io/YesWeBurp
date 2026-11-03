package yesweburp.ui

import yesweburp.api.API
import yesweburp.api.Program
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.border.EmptyBorder

object Theme {
    val accent = Color(0xff6633)
    val danger = Color(0xe5534b)
    val success = Color(0x3fa66b)
    val info = Color(0x4a8fe7)

    val foreground: Color get() = UIManager.getColor("Label.foreground") ?: Color.BLACK
    val background: Color get() = UIManager.getColor("Panel.background") ?: Color.WHITE
    val secondary: Color get() = blend(foreground, background, 0.45)
    val border: Color get() = blend(foreground, background, 0.82)

    fun blend(a: Color, b: Color, ratio: Double): Color = Color(
        (a.red * (1 - ratio) + b.red * ratio).toInt(),
        (a.green * (1 - ratio) + b.green * ratio).toInt(),
        (a.blue * (1 - ratio) + b.blue * ratio).toInt()
    )

    fun font(sizeDelta: Float = 0f, style: Int = Font.PLAIN): Font {
        val base = UIManager.getFont("Label.font") ?: Font(Font.SANS_SERIF, Font.PLAIN, 13)
        return base.deriveFont(style, base.size2D + sizeDelta)
    }

    fun hex(c: Color) = String.format("#%02x%02x%02x", c.red, c.green, c.blue)
}

class RoundedBorder(private val color: Color, private val radius: Int = 10, private val padding: Int = 0) : AbstractBorder() {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = color
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius)
        g2.dispose()
    }

    override fun getBorderInsets(c: Component) = Insets(padding + 1, padding + 1, padding + 1, padding + 1)
}

class Badge(text: String, private val color: Color) : JLabel(text) {
    init {
        font = Theme.font(-1f, Font.BOLD)
        foreground = color
        border = EmptyBorder(2, 8, 2, 8)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = Color(color.red, color.green, color.blue, 40)
        g2.fillRoundRect(0, 0, width - 1, height - 1, height, height)
        g2.color = Color(color.red, color.green, color.blue, 150)
        g2.drawRoundRect(0, 0, width - 1, height - 1, height, height)
        g2.dispose()
        super.paintComponent(g)
    }
}

class StatCard(value: String, caption: String) : JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        border = RoundedBorder(Theme.border, 12, 8)
        add(JLabel(value).apply { font = Theme.font(4f, Font.BOLD) })
        add(JLabel(caption).apply {
            font = Theme.font(-1f)
            foreground = Theme.secondary
        })
    }
}

val Program.isVdp: Boolean get() = type?.startsWith("vdp") == true || bounty == false

val Program.updatedAt: Instant? get() = runCatching { OffsetDateTime.parse(last_update_at).toInstant() }.getOrNull()

fun formatMoney(amount: Int, currency: String): String {
    val n = "%,d".format(Locale.US, amount)
    return when (currency) {
        "USD" -> "$$n"
        "EUR" -> "${n.replace(',', ' ')} €"
        else -> "$n $currency"
    }
}

fun formatCount(n: Int?): String = n?.let { "%,d".format(Locale.US, it) } ?: "–"

fun formatDate(iso: String?): String? = runCatching {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
}.getOrNull()

fun rewardRange(p: Program): String? {
    val currency = p.business_unit.currency
    val min = p.bounty_reward_min?.takeIf { it > 0 }
    val max = p.bounty_reward_max?.takeIf { it > 0 } ?: return null
    return if (min == null || min == max) formatMoney(max, currency)
    else "${formatMoney(min, currency)} – ${formatMoney(max, currency)}"
}

object Logos {
    private val cache = ConcurrentHashMap<String, Optional<ImageIcon>>()

    fun load(url: String?, size: Int, onLoaded: (ImageIcon) -> Unit) {
        if (url == null) return
        val key = "$size@$url"
        cache[key]?.let { cached -> cached.ifPresent(onLoaded); return }
        Thread {
            val image = runCatching { API.fetchImage(url)?.let { ImageIO.read(ByteArrayInputStream(it)) } }.getOrNull()
            val icon = image?.let { ImageIcon(scale(it, size)) }
            cache[key] = Optional.ofNullable(icon)
            if (icon != null) SwingUtilities.invokeLater { onLoaded(icon) }
        }.apply { isDaemon = true }.start()
    }

    private fun scale(image: BufferedImage, size: Int): Image {
        val ratio = minOf(size.toDouble() / image.width, size.toDouble() / image.height)
        return image.getScaledInstance((image.width * ratio).toInt(), (image.height * ratio).toInt(), Image.SCALE_SMOOTH)
    }
}
