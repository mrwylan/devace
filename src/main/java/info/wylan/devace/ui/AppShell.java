package info.wylan.devace.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;

/**
 * Application shell: Aura theme, always in the dark color scheme (docs/architecture/stack.md). Server push carries
 * the results of AI calls that run in the background (ADR-005).
 */
@Push
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
@ColorScheme(ColorScheme.Value.DARK)
public class AppShell implements AppShellConfigurator {

}
