package yesweburp.ui.programs

import swinger.*
import swinger.Button
import swinger.Label
import swinger.ScrollPane
import yesweburp.Settings
import yesweburp.api.Program
import yesweburp.api.RewardGrid
import yesweburp.openInBrowser
import yesweburp.ui.*
import yesweburp.ui.BurpConfig.createConfigFrame
import java.awt.*
import java.lang.StrictMath.min
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.event.HyperlinkEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter


class ProgramInfoPanel(program: Program) : BorderPanel() {

    private class Header(program: Program) : BorderPanel() {
        init {
            hgap = 14
            val logo = JLabel().apply {
                preferredSize = Dimension(LOGO_SIZE, LOGO_SIZE)
                horizontalAlignment = JLabel.CENTER
            }
            Logos.load(program.thumbnail?.url, LOGO_SIZE) { logo.icon = it }

            val badges = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
                isOpaque = false
                border = EmptyBorder(8, -6, 0, 0)
                add(if (program.public) Badge("Public", Theme.success) else Badge("Private", Theme.accent))
                add(if (program.isVdp) Badge("VDP", Theme.info) else Badge("Bug bounty", Theme.info))
                if (program.vpn_active) add(Badge("VPN required", Theme.danger))
                if (program.user_agent != null) add(Badge("Custom User-Agent", Theme.accent))
            }
            val languages = program.supported_languages.orEmpty()
            val subtitle = listOfNotNull(
                program.business_unit.name,
                if (languages.isEmpty()) null else "Languages: ${languages.joinToString(", ")}"
            ).joinToString("  ·  ")

            left = logo
            center = ColumnPanel {
                add(Label(program.title) {
                    font = Theme.font(8f, Font.BOLD)
                    alignmentX = LEFT_ALIGNMENT
                })
                add(Label(subtitle) {
                    foreground = Theme.secondary
                    alignmentX = LEFT_ALIGNMENT
                    border = EmptyBorder(2, 0, 0, 0)
                })
                badges.alignmentX = LEFT_ALIGNMENT
                add(badges)
            }
            right = GridBagPanel {
                val fill = GridBagConstraints.HORIZONTAL
                add(Button("Open on YesWeHack") {
                    onClick { openInBrowser("https://yeswehack.com/programs/${program.slug}") }
                }, gridX = 0, fill = fill)
                add(Button("Configure Burp") {
                    onClick { createConfigFrame(program) }
                }, gridX = 0, fill = fill, insets = Insets(6, 0, 0, 0))
            }
        }
    }

    private class StatsRow(program: Program) : GridPanel(1, 0) {
        init {
            (layout as GridLayout).hgap = 10
            val stats = program.stats
            add(StatCard(rewardRange(program) ?: if (program.isVdp) "No bounty" else "–", "Rewards"))
            add(StatCard(formatCount(program.reports_count ?: stats?.total_reports), "Reports"))
            add(StatCard(formatCount(stats?.total_reports_last7_days), "Reports, last 7 days"))
            add(StatCard(formatCount(stats?.total_reports_last24_hours), "Reports, last 24 h"))
            add(StatCard(program.scopes.size.toString(), "Scopes"))
            add(StatCard(formatDate(program.last_update_at) ?: "–", "Last update"))
        }
    }

    private class HTMLBox(html: String) : BorderPanel() {
        init {
            val font = Theme.font()
            val css = "body { font-family: '${font.family}'; font-size: ${font.size}pt; color: ${Theme.hex(Theme.foreground)}; margin: 12px; }" +
                "a { color: ${Theme.hex(Theme.info)}; }" +
                "h1, h2, h3, h4 { margin-top: 14px; margin-bottom: 6px; }" +
                "p { margin-top: 4px; margin-bottom: 8px; }" +
                "code, pre { font-family: monospace; }"
            val pane = JEditorPane("text/html", "").apply {
                isEditable = false
                text = "<html><head><style>$css</style></head><body>$html</body></html>"
                caretPosition = 0
                addHyperlinkListener { e ->
                    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) e.url?.let { openInBrowser(it.toString()) }
                }
            }
            center = ScrollPane(pane) {
                border = null
                verticalScrollBar.unitIncrement = 16
            }
        }
    }

    private class TitledList(title: String, values: List<String>) : BorderPanel() {
        init {
            vgap = 6
            top = Label("$title (${values.size})") { font = Theme.font(0f, Font.BOLD) }
            center = ScrollPane(SList(values) {
                cellRenderer = object : DefaultListCellRenderer() {
                    override fun getListCellRendererComponent(
                        list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
                    ): Component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).apply {
                        (this as JLabel).border = EmptyBorder(4, 8, 4, 8)
                    }
                }
            }) {
                border = MatteBorder(1, 1, 1, 1, Theme.border)
            }
        }
    }

    private class ScopeTable(program: Program) : JTable() {
        init {
            val currency = program.business_unit.currency
            val columns = arrayOf("Scope", "Type", "Asset value", "Low", "Medium", "High", "Critical")
            val rows = program.scopes.map { scope ->
                val grid = rewardGrid(program, scope.asset_value)
                arrayOf<Any?>(
                    scope.scope,
                    scope.scope_type_name ?: scope.scope_type,
                    assetLabel(scope.asset_value),
                    grid?.bounty_low, grid?.bounty_medium, grid?.bounty_high, grid?.bounty_critical
                )
            }.toTypedArray()

            model = object : DefaultTableModel(rows, columns) {
                override fun isCellEditable(row: Int, column: Int) = false
                override fun getColumnClass(column: Int): Class<*> =
                    if (column >= 3) Integer::class.java else String::class.java
            }
            rowSorter = TableRowSorter(model).apply {
                setComparator(2, compareBy<String> { ASSET_ORDER.indexOf(it) })
            }

            val money = object : DefaultTableCellRenderer() {
                init {
                    horizontalAlignment = RIGHT
                }

                override fun setValue(value: Any?) {
                    text = (value as Int?)?.takeIf { it > 0 }?.let { formatMoney(it, currency) } ?: "–"
                }
            }
            for (i in 3 until columns.size) columnModel.getColumn(i).cellRenderer = money
            columnModel.getColumn(0).preferredWidth = 320
            columnModel.getColumn(1).preferredWidth = 120
            columnModel.getColumn(2).preferredWidth = 80
            for (i in 3 until columns.size) columnModel.getColumn(i).preferredWidth = 70

            rowHeight = getFontMetrics(font).height + 10
            showHorizontalLines = true
            showVerticalLines = false
            gridColor = Theme.border
            fillsViewportHeight = true
            tableHeader.reorderingAllowed = false
        }

        override fun getToolTipText(e: java.awt.event.MouseEvent): String? {
            val row = rowAtPoint(e.point)
            val col = columnAtPoint(e.point)
            if (row < 0 || col != 0) return null
            return getValueAt(row, col) as String?
        }
    }

    init {
        border = EmptyBorder(14, 16, 12, 16)
        vgap = 14
        top = Header(program)
        center = BorderPanel {
            vgap = 14
            top = StatsRow(program)
            center = TabbedPane {
                addTab("Rules", HTMLBox(program.rules_html))
                addTab("Scopes (${program.scopes.size})", JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
                    resizeWeight = 0.7
                    border = null
                    topComponent = ScrollPane(ScopeTable(program)) { border = MatteBorder(1, 1, 1, 1, Theme.border) }
                    bottomComponent = TitledList("Out of scope", program.out_of_scope).apply {
                        border = EmptyBorder(10, 0, 0, 0)
                    }
                })
                addTab("Vulnerabilities", GridPanel(1, 2) {
                    (layout as GridLayout).hgap = 12
                    addPaddingBorder(top = 10)
                    add(TitledList("Qualifying", program.qualifying_vulnerability))
                    add(TitledList("Non qualifying", program.non_qualifying_vulnerability))
                })
                if (program.account_access_html is String) {
                    addTab("Account access", HTMLBox(program.account_access_html))
                }
                selectedIndex = min(Settings.favoriteTab, tabCount - 1)
                addChangeListener {
                    Settings.favoriteTab = selectedIndex
                }
            }
        }
    }

    companion object {
        private const val LOGO_SIZE = 64
        private val ASSET_ORDER = listOf("Critical", "High", "Medium", "Low", "Very low", "Default")

        private fun rewardGrid(program: Program, assetValue: String): RewardGrid? = when (assetValue) {
            "VERY_LOW" -> program.reward_grid_very_low
            "LOW" -> program.reward_grid_low
            "MEDIUM" -> program.reward_grid_medium
            "HIGH" -> program.reward_grid_high
            "CRITICAL" -> program.reward_grid_critical
            else -> program.reward_grid_default
        }

        private fun assetLabel(assetValue: String) =
            if (assetValue.isBlank()) "Default"
            else assetValue.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
