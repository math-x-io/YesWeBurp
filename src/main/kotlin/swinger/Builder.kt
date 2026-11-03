package  swinger

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EtchedBorder
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.reflect.KProperty


class Button(text: String, build: (Button.() -> Unit)? = null) : JButton(text) {
    init {
        build?.invoke(this)
    }

    fun onClick(action: () -> Unit) {
        addActionListener {
            action()
        }
    }
}

open class Panel(build: (Panel.() -> Unit)? = null) : JPanel() {
    init {
        build?.invoke(this)
    }

    fun addBorder(b: Border) {
        border = if (border is Border) BorderFactory.createCompoundBorder(b, border) else b
    }

    fun addTitleBorder(title: String) {
        val border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            title
        )
        border.titlePosition = TitledBorder.TOP
        border.titleJustification = TitledBorder.CENTER
        addBorder(border)
    }

    fun addPaddingBorder(padding: Int = 0) {
        addPaddingBorder(padding, padding, padding, padding)
    }

    fun addPaddingBorder(top: Int = 0, left: Int = 0, bottom: Int = 0, right: Int = 0) {
        addBorder(BorderFactory.createEmptyBorder(top, left, bottom, right))
    }
}

class ScrollPane(component: Component, build: (ScrollPane.() -> Unit)? = null) : JScrollPane(component) {
    init {
        build?.invoke(this)
    }
}

open class BorderPanel(build: (BorderPanel.() -> Unit)? = null) : Panel() {
    class PositionedComponent(private val panel: BorderPanel, private val pos: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Component {
            return (panel.layout as BorderLayout).getLayoutComponent(pos)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Component) {
            panel.add(value, pos)
        }
    }

    var left: Component by PositionedComponent(this, BorderLayout.LINE_START)
    var right: Component by PositionedComponent(this, BorderLayout.LINE_END)
    var top: Component by PositionedComponent(this, BorderLayout.PAGE_START)
    var bottom: Component by PositionedComponent(this, BorderLayout.PAGE_END)
    var center: Component by PositionedComponent(this, BorderLayout.CENTER)

    var hgap: Int
        get() {
            return (layout as BorderLayout).hgap
        }
        set(value) {
            (layout as BorderLayout).hgap = value
        }
    var vgap: Int
        get() {
            return (layout as BorderLayout).vgap
        }
        set(value) {
            (layout as BorderLayout).vgap = value
        }

    init {
        layout = BorderLayout()
        build?.invoke(this)
    }
}



open class GridPanel(rows: Int, cols: Int, build: (GridPanel.() -> Unit)? = null) : Panel() {
    init {
        layout = GridLayout(rows, cols)
        build?.invoke(this)
    }
}

open class TabbedPane(build: (TabbedPane.() -> Unit)? = null) : JTabbedPane() {
    init {
        build?.invoke(this)
    }
}

open class GridBagPanel(build: (GridBagPanel.() -> Unit)? = null) : Panel() {

    init {
        layout = GridBagLayout()
        build?.invoke(this)
    }

    fun add(
        component: Component,
        gridX: Int = GridBagConstraints.RELATIVE,
        gridY: Int = GridBagConstraints.RELATIVE,
        gridWidth: Int = 1,
        gridHeight: Int = 1,
        weightX: Double = 0.0,
        weightY: Double = 0.0,
        anchor: Int = GridBagConstraints.CENTER,
        fill: Int = GridBagConstraints.NONE,
        insets: Insets = Insets(0, 0, 0, 0),
        ipadX: Int = 0,
        ipadY: Int = 0
    ) {
        super.add(
            component,
            GridBagConstraints(
                gridX,
                gridY,
                gridWidth,
                gridHeight,
                weightX,
                weightY,
                anchor,
                fill,
                insets,
                ipadX,
                ipadY
            )
        )
    }
}

open class ColumnPanel(build: (ColumnPanel.() -> Unit)? = null) : Panel() {
    init {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        build?.invoke(this)
    }
}

class SplitPanel(direction: Int, build: (SplitPanel.() -> Unit)? = null) : JSplitPane(direction) {

    init {
        build?.invoke(this)
    }

    fun withFixedDivider(action: SplitPanel.() -> Unit) {
        val loc = dividerLocation
        action(this)
        dividerLocation = loc
    }
}

class TextField(text: String? = null, build: (TextField.() -> Unit)? = null) : JTextField(text) {
    var placeholder: String? = null

    init {
        build?.invoke(this)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val hint = placeholder
        if (hint != null && text.isEmpty()) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.color = disabledTextColor
            g2.font = font
            val fm = g2.fontMetrics
            g2.drawString(hint, insets.left, (height - fm.height) / 2 + fm.ascent)
            g2.dispose()
        }
    }

    fun onInput(action: (String) -> Unit) {
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = action(text)
            override fun removeUpdate(e: DocumentEvent?) = action(text)
            override fun changedUpdate(e: DocumentEvent?) = action(text)
        })
    }
}

class PasswordField(text: String? = null, build: (PasswordField.() -> Unit)? = null) : JPasswordField(text) {
    override fun getText(): String {
        return String(super.getPassword())
    }

    init {
        build?.invoke(this)
    }
}

class Label(text: String? = null, build: (Label.() -> Unit)? = null) : JLabel(text) {
    init {
        build?.invoke(this)
    }
}

open class ComboBox<T>(val values: Array<T>, build: (ComboBox<T>.() -> Unit)? = null) : JComboBox<T>(values) {
    init {
        build?.invoke(this)
    }

    val selected: T get() = selectedItem as T

    fun onChange(callback: (T) -> Unit) {
        addItemListener { evt ->
            if (evt.stateChange == ItemEvent.SELECTED) {
                callback(selected)
            }
        }
    }
}

class SListModel<T>(values: List<T>) : AbstractListModel<T>() {
    var values: List<T> = values
        set(value) {
            val oldSize = field.size
            field = value
            fireContentsChanged(this, 0, maxOf(oldSize, value.size))
        }

    override fun getSize(): Int {
        return values.size
    }

    override fun getElementAt(pos: Int): T {
        return values[pos]
    }
}

open class SList<T>(private val values: List<T>, build: (SList<T>.() -> Unit)? = null) : JList<T>() {
    init {
        model = SListModel(values)
        build?.invoke(this)
    }

    fun applyFilter(filter: (T) -> Boolean) {
        show(values.filter(filter))
    }

    fun show(items: List<T>) {
        val selected = selectedValue
        (model as SListModel<T>).values = items
        if (selected != null && selected in items) setSelectedValue(selected, true) else clearSelection()
        revalidate()
        repaint()
    }

    fun onSelect(action: (T) -> Unit) {
        addListSelectionListener { e ->
            val value = selectedValue
            if (!e.valueIsAdjusting && value != null) action(value)
        }
    }
}

open class CheckBox(build: (CheckBox.() -> Unit)? = null) : JCheckBox() {
    init {
        build?.invoke(this)
    }
}

