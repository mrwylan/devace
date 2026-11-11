package info.wylan.devace.ui;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import info.wylan.devace.service.AssessmentService.Auswertung;
import info.wylan.devace.service.AssessmentService.FeldScore;
import info.wylan.devace.service.KiModellClient.Abschnitt;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

/**
 * UC-003 step 7: der Textreport als PDF zum einmaligen Download. Er enthält keine Angabe zur Person und keine
 * Einladung, nur Rolle, Erstellungsdatum, Scores und den Text des KI-Modells.
 */
final class TextreportPdf {

	private TextreportPdf() {
	}

	static byte[] erstellen(Auswertung auswertung) {
		ByteArrayOutputStream ausgabe = new ByteArrayOutputStream();
		Document dokument = new Document(PageSize.A4, 56, 56, 56, 56);
		PdfWriter.getInstance(dokument, ausgabe);
		dokument.open();
		Font titel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
		Font ueberschrift = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
		Font text = FontFactory.getFont(FontFactory.HELVETICA, 10);
		Font klein = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

		dokument.add(new Paragraph("devace · Textreport", titel));
		dokument.add(new Paragraph("Rolle: " + auswertung.rolle() + " · erstellt am "
				+ auswertung.erstelltAm().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), text));
		dokument.add(abstand());
		dokument.add(new Paragraph(auswertung.textreport().ueberblick(), text));
		dokument.add(abstand());

		dokument.add(new Paragraph("Scores je Handlungsfeld (Skala 0 bis 5)", ueberschrift));
		for (FeldScore score : auswertung.scores()) {
			dokument.add(new Paragraph(score.name() + ": "
					+ ((score.score() != null) ? AssessmentView.format(score.score()) : "kein Score (keine Frage)"), text));
		}
		dokument.add(abstand());

		for (Abschnitt abschnitt : auswertung.textreport().abschnitte()) {
			dokument.add(new Paragraph(abschnitt.handlungsfeld(), ueberschrift));
			dokument.add(new Paragraph(abschnitt.wuerdigung(), text));
			if (!abschnitt.naechsterSchritt().isBlank()) {
				dokument.add(new Paragraph("Nächster Schritt: " + abschnitt.naechsterSchritt(), text));
			}
			dokument.add(abstand());
		}
		// UC-003 BR-003: kein Bestandteil der Leistungsbeurteilung
		dokument.add(new Paragraph(
				"Das sind Vorschläge zur Weiterentwicklung. Scores und Textreport sind kein Bestandteil der Leistungsbeurteilung.",
				klein));
		dokument.close();
		return ausgabe.toByteArray();
	}

	private static Paragraph abstand() {
		return new Paragraph(" ");
	}

}
