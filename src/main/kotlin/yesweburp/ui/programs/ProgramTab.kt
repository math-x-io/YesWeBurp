package yesweburp.ui.programs

import swinger.BorderPanel
import swinger.Label
import swinger.SplitPanel
import yesweburp.api.Program
import yesweburp.ui.Theme
import javax.swing.JLabel
import javax.swing.JSplitPane


class ProgramTab(programs: List<Program>) : BorderPanel() {
    private val splitPanel = SplitPanel(JSplitPane.HORIZONTAL_SPLIT) {
        isOneTouchExpandable = true
        dividerLocation = 360
        rightComponent = Label(if (programs.isEmpty()) "No program available" else "Select a program") {
            horizontalAlignment = JLabel.CENTER
            foreground = Theme.secondary
        }
    }

    init {
        val listPanel = ProgramsListPanel(programs, ::showProgramInfo)
        splitPanel.leftComponent = listPanel
        center = splitPanel
        listPanel.selectFirst()
    }

    private fun showProgramInfo(program: Program) {
        splitPanel.withFixedDivider {
            rightComponent = ProgramInfoPanel(program)
        }
    }
}
