package info.wylan.devace.ui.bausteine;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import org.jspecify.annotations.Nullable;

/**
 * ADR-006: Liste mit Suchfeld, Anzeige „x von y“, sortierbaren Spalten und Zeilenaktionen als Symbolknöpfe. Die Liste
 * füllt ihre Karte und scrollt in sich selbst; kurze, eingebettete Listen zeigen mit {@link #alleZeilen()} alle Zeilen.
 * @param <T> der Typ einer Zeile
 */
public class DevaceListe<T> extends VerticalLayout {

	private static final Collator DEUTSCH = Collator.getInstance(Locale.GERMAN);

	private final Grid<T> grid = new Grid<>();

	private final TextField suche = new TextField();

	private final Span anzahl = new Span();

	private final List<Function<T, String>> suchtexte = new ArrayList<>();

	private final List<Grid<?>> weitere = new ArrayList<>();

	private @Nullable GridListDataView<T> daten;

	private int gesamt;

	/**
	 * @param id die Id der Tabelle; das Suchfeld erhält {@code <id>-suche}
	 * @param leer der Text, wenn die Liste keine Zeile hat
	 */
	public DevaceListe(String id, String leer) {
		addClassName("devace-liste");
		setPadding(false);
		setSpacing(false);
		this.grid.setId(id);
		this.grid.addClassName("devace-liste-tabelle");
		this.grid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);
		this.grid.setEmptyStateText(leer);
		this.grid.setMultiSort(false);

		this.suche.setId(id + "-suche");
		this.suche.setPlaceholder("Suchen …");
		this.suche.setAriaLabel("Liste durchsuchen");
		this.suche.setPrefixComponent(VaadinIcon.SEARCH.create());
		this.suche.setClearButtonVisible(true);
		this.suche.setValueChangeMode(ValueChangeMode.LAZY);
		this.suche.addClassName("devace-liste-suche");
		this.suche.addValueChangeListener((event) -> filtern());
		this.anzahl.setId(id + "-anzahl");
		this.anzahl.addClassName("devace-liste-anzahl");

		HorizontalLayout kopf = new HorizontalLayout(this.suche, this.anzahl);
		kopf.addClassName("devace-liste-kopf");
		kopf.setWidthFull();
		kopf.setAlignItems(FlexComponent.Alignment.CENTER);
		add(kopf, this.grid);
		setFlexGrow(1, this.grid);
	}

	/**
	 * Fügt eine Textspalte hinzu, die sortierbar ist und vom Suchfeld durchsucht wird.
	 * @param kopf die Spaltenüberschrift
	 * @param text der Text der Zelle
	 * @return die Spalte, für weitere Einstellungen
	 */
	public Grid.Column<T> spalte(String kopf, Function<T, String> text) {
		this.suchtexte.add(text);
		return this.grid.addColumn(text::apply)
			.setHeader(kopf)
			.setSortable(true)
			.setComparator(Comparator.comparing(text, DEUTSCH))
			.setResizable(true);
	}

	/**
	 * Fügt eine Spalte mit einer Komponente hinzu, etwa einem Status-Badge.
	 * @param kopf die Spaltenüberschrift
	 * @param komponente die Komponente der Zelle
	 * @param suchtext der Text, nach dem Suche und Sortierung gehen
	 * @return die Spalte, für weitere Einstellungen
	 */
	public Grid.Column<T> komponentenspalte(String kopf, ValueProvider<T, Component> komponente,
			Function<T, String> suchtext) {
		this.suchtexte.add(suchtext);
		return this.grid.addComponentColumn(komponente)
			.setHeader(kopf)
			.setSortable(true)
			.setComparator(Comparator.comparing(suchtext, DEUTSCH))
			.setResizable(true);
	}

	/**
	 * Fügt rechts die Spalte der Zeilenaktionen hinzu. Mit einer Aktion ist die Zelle die Schaltfläche selbst, mit
	 * mehreren eine Zeile von Schaltflächen.
	 * @param aktionen die Aktionen jeder Zeile
	 * @return die Spalte
	 */
	@SafeVarargs
	public final Grid.Column<T> aktionen(Aktion<T>... aktionen) {
		return this.grid.addComponentColumn((zeile) -> {
			if (aktionen.length == 1) {
				return aktionen[0].knopf(zeile);
			}
			HorizontalLayout leiste = new HorizontalLayout();
			leiste.setSpacing(false);
			leiste.addClassName("devace-zeilenaktionen");
			for (Aktion<T> aktion : aktionen) {
				leiste.add(aktion.knopf(zeile));
			}
			return leiste;
		}).setHeader("").setAutoWidth(true).setFlexGrow(0).setFrozenToEnd(true);
	}

	/**
	 * Setzt die Zeilen; ein eingegebener Suchbegriff bleibt wirksam.
	 * @param zeilen die Zeilen
	 * @return die Datensicht der Tabelle
	 */
	public GridListDataView<T> setItems(Collection<T> zeilen) {
		this.gesamt = zeilen.size();
		this.daten = this.grid.setItems(new ArrayList<>(zeilen));
		this.daten.addItemCountChangeListener((event) -> anzeigen());
		filtern();
		return this.daten;
	}

	/** Zeigt alle Zeilen ohne eigenen Bildlauf, für kurze Listen innerhalb einer längeren Seite. */
	public DevaceListe<T> alleZeilen() {
		this.grid.setAllRowsVisible(true);
		addClassName("devace-liste-alle");
		return this;
	}

	/** Blendet Suchfeld und Anzeige aus, etwa wenn mehrere Listen ein gemeinsames Suchfeld haben. */
	public DevaceListe<T> ohneSuche() {
		this.suche.getParent().ifPresent((kopf) -> kopf.setVisible(false));
		return this;
	}

	/**
	 * Filtert die Liste von außen, etwa über ein Suchfeld für mehrere Listen.
	 * @param begriff der Suchbegriff
	 */
	public void suchen(String begriff) {
		this.suche.setValue(begriff);
	}

	/** @return die Tabelle */
	public Grid<T> getGrid() {
		return this.grid;
	}

	/** @return das Suchfeld */
	public TextField getSuche() {
		return this.suche;
	}

	private void filtern() {
		if (this.daten == null) {
			return;
		}
		String begriff = this.suche.getValue().strip().toLowerCase(Locale.GERMAN);
		if (begriff.isEmpty()) {
			this.daten.removeFilters();
		}
		else {
			this.daten.setFilter((zeile) -> this.suchtexte.stream()
				.map((text) -> text.apply(zeile))
				.anyMatch((text) -> text != null && text.toLowerCase(Locale.GERMAN).contains(begriff)));
		}
		anzeigen();
	}

	private void anzeigen() {
		int sichtbar = (this.daten != null) ? this.daten.getItemCount() : 0;
		this.anzahl.setText((sichtbar == this.gesamt) ? this.gesamt + " Einträge" : sichtbar + " von " + this.gesamt);
	}

	/**
	 * Eine Zeilenaktion als Symbolknopf mit Tooltip und zugänglichem Namen.
	 * @param symbol das Symbol
	 * @param tooltip der kurze Text des Tooltips, etwa „Ändern“
	 * @param name der zugängliche Name je Zeile, etwa „MAG Alpha Nord ändern“
	 * @param ausfuehren was die Aktion tut
	 * @param gefahr ob die Aktion etwas entfernt
	 * @param id die Id der Schaltfläche je Zeile oder {@code null}
	 * @param <T> der Typ einer Zeile
	 */
	public record Aktion<T>(VaadinIcon symbol, String tooltip, Function<T, String> name, Consumer<T> ausfuehren,
			boolean gefahr, @Nullable Function<T, String> id) {

		/** Eine gewöhnliche Aktion. */
		public static <T> Aktion<T> von(VaadinIcon symbol, String tooltip, Function<T, String> name,
				Consumer<T> ausfuehren) {
			return new Aktion<>(symbol, tooltip, name, ausfuehren, false, null);
		}

		/** Eine Aktion, die etwas entfernt, in der Fehlerfarbe. */
		public static <T> Aktion<T> entfernen(Function<T, String> name, Consumer<T> ausfuehren) {
			return new Aktion<>(VaadinIcon.TRASH, "Entfernen", name, ausfuehren, true, null);
		}

		/**
		 * @param id die Id der Schaltfläche je Zeile
		 * @return die Aktion mit Id
		 */
		public Aktion<T> mitId(Function<T, String> id) {
			return new Aktion<>(this.symbol, this.tooltip, this.name, this.ausfuehren, this.gefahr, id);
		}

		Button knopf(T zeile) {
			Button knopf = Symbolknopf.erstellen(this.symbol, this.tooltip, this.name.apply(zeile));
			if (this.gefahr) {
				knopf.addThemeVariants(ButtonVariant.ERROR);
			}
			if (this.id != null) {
				knopf.setId(this.id.apply(zeile));
			}
			knopf.addClickListener((event) -> this.ausfuehren.accept(zeile));
			return knopf;
		}

	}

}
