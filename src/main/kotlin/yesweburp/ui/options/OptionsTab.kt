package yesweburp.ui.options

import swinger.ColumnPanel
import swinger.GridBagPanel
import swinger.Label
import yesweburp.VERSION
import yesweburp.ui.Padding
import yesweburp.ui.Theme
import java.awt.Dimension
import java.awt.GridBagConstraints
import javax.swing.JScrollPane


class OptionsTab : JScrollPane() {

    init {
        border = null
        setViewportView(ColumnPanel {
            addPaddingBorder(12)
            add(APIPanel())
            add(GridBagPanel {
                alignmentX = LEFT_ALIGNMENT
                addPaddingBorder(10)
                addTitleBorder("About")
                addPaddingBorder(top = 12)
                for ((label, value) in listOf("Version" to VERSION, "API" to "https://api.yeswehack.com")) {
                    add(Label(label) { foreground = Theme.secondary }, gridX = 0, insets = Padding(5), anchor = GridBagConstraints.EAST)
                    add(Label(value), gridX = 1, weightX = 1.0, insets = Padding(5), anchor = GridBagConstraints.WEST)
                }
                maximumSize = Dimension(640, preferredSize.height)
            })
        })
    }
}
