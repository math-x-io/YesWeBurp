package yesweburp.ui.options

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import swinger.*
import yesweburp.Events.programsLoaded
import yesweburp.Settings
import yesweburp.api.API
import yesweburp.api.AuthMethod
import yesweburp.ui.Padding
import yesweburp.ui.Theme
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.Box.createHorizontalGlue
import javax.swing.JLabel
import javax.swing.SwingUtilities

class APIPanel : GridBagPanel() {
    private val labels = mutableMapOf<Component, Component>()

    init {
        alignmentX = LEFT_ALIGNMENT
        addPaddingBorder(10)
        addTitleBorder("YesWeHack account")
        val auth = AuthMethod.fromValue(Settings.authMethod)

        val email = TextField(Settings.userEmail) { placeholder = "hunter@example.com" }
        val password = PasswordField(Settings.userPassword)
        val otp = TextField { placeholder = "000000" }
        val token = PasswordField {
            toolTipText = "Bearer token of a logged-in yeswehack.com session. Never saved."
        }
        val tokenHint = Label("Bearer token of a logged-in yeswehack.com session, never saved.") {
            foreground = Theme.secondary
            font = Theme.font(-1f)
        }
        val rememberPassword = CheckBox {
            text = "Keep a plaintext copy in Burp settings"
            isSelected = Settings.rememberPassword
        }
        val status = Label("Programs not loaded yet.") {
            foreground = Theme.secondary
        }

        fun showFields(method: AuthMethod) {
            setRowVisible(email, method.requireEmail)
            setRowVisible(password, method.requirePassword)
            setRowVisible(rememberPassword, method.requirePassword)
            setRowVisible(otp, method.requireOTP)
            setRowVisible(token, method.requireToken)
            setRowVisible(tokenHint, method.requireToken)
            fitWidth()
        }

        val authMethodList = ComboBox(AuthMethod.labels()) {
            selectedIndex = values.indexOf(auth.label)
            onChange { showFields(AuthMethod.fromValue(it)) }
        }

        fun setStatus(text: String, color: java.awt.Color) {
            status.text = text
            status.foreground = color
            fitWidth()
        }

        val fetchButton = Button("Fetch programs") {
            font = font.deriveFont(Font.BOLD)
        }
        fetchButton.onClick {
            val authEmail = email.text
            val authPassword = password.text
            val authOTP = otp.text
            val authToken = token.text
            val authMethod = AuthMethod.fromValue(authMethodList.selected)
            Settings.userEmail = authEmail
            if (rememberPassword.isSelected) {
                Settings.rememberPassword = true
                Settings.userPassword = authPassword
            } else {
                Settings.rememberPassword = false
                Settings.userPassword = ""
                password.text = ""
            }
            Settings.authMethod = authMethod.label
            otp.text = ""

            fetchButton.isEnabled = false
            fetchButton.text = "Fetching…"
            setStatus("Fetching programs, this can take a few seconds…", Theme.secondary)

            GlobalScope.launch {
                try {
                    when (authMethod) {
                        AuthMethod.ANONYMOUS -> API.auth()
                        AuthMethod.EMAIL_PASSWORD -> API.auth(authEmail, authPassword)
                        AuthMethod.EMAIL_PASSWORD_OTP -> API.auth(authEmail, authPassword, authOTP)
                        AuthMethod.TOKEN -> API.auth(authToken)
                    }
                    API.fetchPrograms()
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        fetchButton.isEnabled = true
                        fetchButton.text = "Fetch programs"
                        setStatus(e.message ?: "Unknown error", Theme.danger)
                    }
                }
            }
        }

        programsLoaded.listen { programs ->
            fetchButton.isEnabled = true
            fetchButton.text = "Fetch programs"
            val privateCount = programs.count { !it.public }
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            setStatus("${programs.size} programs loaded ($privateCount private) at $time.", Theme.success)
        }

        addSetting("Authentication", authMethodList)
        addSetting("Email", email)
        addSetting("Password", password)
        addSetting(null, rememberPassword)
        addSetting("OTP", otp)
        addSetting("Access token", token)
        addSetting(null, tokenHint)
        addSetting(null, fetchButton)
        addSetting(null, status)
        showFields(auth)
    }

    private fun fitWidth() {
        revalidate()
        maximumSize = Dimension(640, preferredSize.height)
    }

    private fun setRowVisible(setting: Component, visible: Boolean) {
        setting.isVisible = visible
        labels[setting]?.isVisible = visible
    }

    private fun addSetting(label: String?, setting: Component) {
        val left = if (label == null) createHorizontalGlue() else Label(label) {
            horizontalAlignment = JLabel.RIGHT
        }
        labels[setting] = left
        add(
            left,
            gridX = 0,
            insets = Padding(5),
            anchor = GridBagConstraints.EAST,
            fill = GridBagConstraints.HORIZONTAL,
        )
        add(
            setting,
            gridX = 1,
            weightX = 1.0,
            fill = GridBagConstraints.HORIZONTAL,
            insets = Padding(5),
            anchor = GridBagConstraints.WEST
        )
    }
}
