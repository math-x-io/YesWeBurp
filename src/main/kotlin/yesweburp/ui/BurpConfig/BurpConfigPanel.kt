package yesweburp.ui.BurpConfig

import swinger.*
import yesweburp.api.Program
import yesweburp.api.Scope
import yesweburp.callbacks
import yesweburp.config.BurpConfig
import yesweburp.config.BurpReplaceRule
import yesweburp.config.BurpScopeRule
import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Font
import javax.swing.*
import javax.swing.table.DefaultTableModel


fun hostToRegex(s: String): String {
    var host = s
    var reg = "^"
    if (host.startsWith("*.")) {
        reg += "(.*\\.)?"
        host = host.substring(2)
    }
    reg += host.replace(".", "\\.")
    reg += "$"
    return reg
}

private val storeHosts = setOf("play.google.com", "apps.apple.com", "itunes.apple.com", "appgallery.huawei.com")

fun guessRule(s: String): BurpScopeRule? {
    val reg = arrayOf(
            "^(?:(?<protocol>https?):\\/\\/)?",
            "(?<host>[^/:?#\\s]+)",
            "(?::(?<port>(?:\\d+|\\(?(?:\\d+\\|)*\\d+\\))))?",
            "(?<path>\\/.*)?",
    ).joinToString("")
    val groups = Regex(reg).matchEntire(s.trim())?.groups ?: return null
    val host = groups[2]!!.value
    if ('.' !in host || host.lowercase() in storeHosts) return null

    val rule = BurpScopeRule()
    rule.protocol = groups[1]?.value ?: "any"
    rule.host = hostToRegex(host)
    rule.port = groups[3]?.value ?: ""
    rule.file = groups[4]?.value ?: ""
    return rule
}

fun guessScope(scope: Scope): BurpScopeRule? {
    if (scope.scope_type == "ip-address") {
        return BurpScopeRule(host = scope.scope)
    }
    return guessRule(scope.scope)
}


fun createConfigFrame(program: Program) {
    EventQueue.invokeLater {
        BurpConfigWindow(program)
    }
}

class BurpConfigWindow(program: Program) : JFrame("Configure Burp for ${program.title}") {
    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        callbacks.customizeUiComponent(this)
        contentPane = BurpConfigPanel(program)
        val mainWindow = SwingUtilities.getWindowAncestor(this)
        pack()
        setLocationRelativeTo(mainWindow)
        isLocationByPlatform = true
        isVisible = true
        requestFocus()
    }
}

class BurpConfigPanel(program: Program) : ColumnPanel() {

    class ScopeTable(private val scopeRuleInfos: List<Pair<String, BurpScopeRule?>>) : JTable() {
        private val columns = arrayOf("Program rule", "Protocol", "Host", "Port", "File")


        init {
            getTableHeader().reorderingAllowed = false
            val values = scopeRuleInfos.map { (scope, rule) ->
                        arrayOf(
                                scope,
                                rule?.protocol ?: "",
                                rule?.host ?: "",
                                rule?.port ?: "",
                                rule?.file ?: ""
                        )
                    }.toTypedArray()

            model = DefaultTableModel(values, columns)

            with(columnModel.getColumn(1)) {
                val comboBox = JComboBox<String>()
                comboBox.addItem("any")
                comboBox.addItem("http")
                comboBox.addItem("https")
                cellEditor = DefaultCellEditor(comboBox)
            }
            columnModel.getColumn(1).preferredWidth = 5
            columnModel.getColumn(3).preferredWidth = 5

        }

        fun getSelectedRules(): List<BurpScopeRule> {
            return selectedRows.filter { (model.getValueAt(it, 2) as String).isNotBlank() }.map { rowIdx ->
                BurpScopeRule(
                        enabled = true,
                        protocol = model.getValueAt(rowIdx, 1) as String,
                        host = model.getValueAt(rowIdx, 2) as String,
                        port = model.getValueAt(rowIdx, 3) as String,
                        file = model.getValueAt(rowIdx, 4) as String,
                )
            }

        }
    }


    private fun scopeBox(title: String, table: ScopeTable, action: String, apply: (List<BurpScopeRule>) -> Unit) =
        BorderPanel {
            vgap = 5
            addTitleBorder(title)
            val addButton = Button("$action (0)") {
                isEnabled = false
                onClick {
                    apply(table.getSelectedRules())
                }
            }
            table.selectionModel.addListSelectionListener {
                val count = table.getSelectedRules().size
                addButton.text = "$action ($count)"
                addButton.isEnabled = count > 0
            }
            center = ScrollPane(table)
            bottom = addButton
            preferredSize = Dimension(800, preferredSize.height)
        }

    init {
        addPaddingBorder(top = 5)
        val scopes = scopeBox(
            "Scopes",
            ScopeTable(program.scopes.map { Pair(it.scope, guessScope(it)) }),
            "Add to scope",
            BurpConfig::addTargetScope
        )

        if (program.vpn_active) {
            val lbl = Label("Using YesWeHack VPN is required for this program") {

                foreground = Color(0xff6633)
                font = font.deriveFont(font.style or Font.BOLD)
            }
            lbl.alignmentX = CENTER_ALIGNMENT
            add(lbl)
        }

        add(scopes)

        if (program.out_of_scope.isNotEmpty()) {
            add(scopeBox(
                "Out of scope",
                ScopeTable(program.out_of_scope.map { Pair(it, guessRule(it)) }),
                "Add to exclusions",
                BurpConfig::addTargetExclude
            ))
        }

        if (program.user_agent is String) {
            val uaBox = BorderPanel {
                vgap = 5
                addTitleBorder("User-Agent")
                val uaTable = JTable(
                        arrayOf(arrayOf("^User-Agent: (.*)\$", "User-Agent: \$1 ${program.user_agent}")),
                        arrayOf("Match", "Replace")
                )
                uaTable.minimumSize = uaTable.preferredSize
                uaTable.preferredScrollableViewportSize = uaTable.preferredSize
                center = ScrollPane(uaTable)
                bottom = Button("Add match/replace rule (all tools)") {
                    onClick {
                        val match = uaTable.model.getValueAt(0, 0) as String;
                        val replace = uaTable.model.getValueAt(0, 1) as String;
                        BurpConfig.addMatchReplace(BurpReplaceRule(
                            rule_type = "request_header",
                            string_match = match,
                            string_replace = replace,
                            comment = "${program.slug}${BurpConfig.RULE_COMMENT_SUFFIX}"
                        ))

                    }
                }

            }
            add(uaBox)
        }
    }
}
