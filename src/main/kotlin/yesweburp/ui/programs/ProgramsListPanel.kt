package yesweburp.ui.programs

import swinger.*
import swinger.Label
import swinger.ScrollPane
import swinger.TextField
import yesweburp.api.Program
import yesweburp.ui.*
import java.awt.*
import java.time.Instant
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder

class ProgramsListPanel(private val programs: List<Program>, onSelect: (Program) -> Unit) : BorderPanel() {

    private enum class Sort(val label: String, val comparator: Comparator<Program>) {
        NAME("Name", compareBy { it.title.lowercase() }),
        REWARD("Max reward", compareByDescending<Program> { it.bounty_reward_max ?: -1 }.thenBy { it.title.lowercase() }),
        ACTIVITY("Reports (7 days)", compareByDescending<Program> { it.stats?.total_reports_last7_days ?: -1 }.thenBy { it.title.lowercase() }),
        UPDATED("Last update", compareByDescending<Program> { it.updatedAt ?: Instant.EPOCH }.thenBy { it.title.lowercase() });
    }

    private class ProgramCell : BorderPanel() {
        val title = JLabel()
        val subtitle = JLabel()
        val badges = GridBagPanel { isOpaque = false }

        init {
            border = EmptyBorder(7, 10, 7, 10)
            hgap = 8
            title.font = Theme.font(1f, Font.BOLD)
            subtitle.font = Theme.font(-1f)
            subtitle.border = EmptyBorder(2, 0, 0, 0)
            center = ColumnPanel {
                isOpaque = false
                add(title)
                add(subtitle)
            }
            right = badges
        }
    }

    private class ProgramCellRenderer : ListCellRenderer<Program> {
        private val cell = ProgramCell()

        override fun getListCellRendererComponent(
            list: JList<out Program>,
            value: Program,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val fg = if (isSelected) list.selectionForeground else list.foreground
            cell.background = if (isSelected) list.selectionBackground else list.background
            cell.title.text = value.title
            cell.title.foreground = fg
            cell.subtitle.text = listOfNotNull(value.business_unit.name, rewardRange(value)).joinToString("  ·  ")
            cell.subtitle.foreground = if (isSelected) fg else Theme.secondary
            cell.badges.removeAll()
            if (!value.public) cell.badges.add(Badge("Private", Theme.accent), insets = Insets(0, 4, 0, 0))
            if (value.isVdp) cell.badges.add(Badge("VDP", Theme.info), insets = Insets(0, 4, 0, 0))
            return cell
        }
    }

    private val programList = object : SList<Program>(programs, {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = ProgramCellRenderer()
        onSelect(onSelect)
    }) {
        override fun getScrollableTracksViewportWidth() = true
    }
    private val search = TextField {
        placeholder = "Search programs, companies, scopes…"
        toolTipText = "Matches program title, company and scopes"
    }
    private val sort = ComboBox(Sort.values().map { it.label }.toTypedArray())
    private val privateOnly = CheckBox { text = "Private only" }
    private val count = Label { foreground = Theme.secondary; font = Theme.font(-1f) }

    init {
        vgap = 8
        addPaddingBorder(8)

        top = ColumnPanel {
            search.alignmentX = LEFT_ALIGNMENT
            add(search)
            add(BorderPanel {
                alignmentX = LEFT_ALIGNMENT
                addPaddingBorder(top = 6)
                hgap = 8
                left = Label("Sort") { foreground = Theme.secondary }
                center = sort
                right = privateOnly
            })
        }
        center = ScrollPane(programList) {
            border = MatteBorder(1, 0, 1, 0, Theme.border)
            verticalScrollBar.unitIncrement = 16
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        bottom = count

        search.onInput { refresh() }
        sort.onChange { refresh() }
        privateOnly.addItemListener { refresh() }
        refresh()
        minimumSize = Dimension(280, 0)
        preferredSize = Dimension(360, preferredSize.height)
    }

    fun selectFirst() {
        if (programList.model.size > 0) programList.selectedIndex = 0
    }

    private fun matches(p: Program, query: String): Boolean =
        query.isEmpty() ||
            p.title.contains(query, ignoreCase = true) ||
            p.business_unit.name?.contains(query, ignoreCase = true) == true ||
            p.scopes.any { it.scope.contains(query, ignoreCase = true) }

    private fun refresh() {
        val query = search.text.trim()
        val order = Sort.values().first { it.label == sort.selected }
        val shown = programs
            .filter { (!privateOnly.isSelected || !it.public) && matches(it, query) }
            .sortedWith(order.comparator)
        programList.show(shown)
        count.text = if (shown.size == programs.size) "${programs.size} programs"
        else "${shown.size} of ${programs.size} programs"
    }
}
