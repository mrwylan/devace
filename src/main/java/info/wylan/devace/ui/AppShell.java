package info.wylan.devace.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.theme.aura.Aura;

/**
 * Application shell: Aura theme, always in the dark color scheme (docs/architecture/stack.md).
 */
@StyleSheet(Aura.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
public class AppShell implements AppShellConfigurator {

}
