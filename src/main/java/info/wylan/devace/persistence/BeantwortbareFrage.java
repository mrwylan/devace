package info.wylan.devace.persistence;

import java.util.List;

/**
 * Eine Frage eines Fragenkatalogs mit Position und Stufenvorgaben, wie UC-002 sie stellt. {@code vorgaben} enthält
 * die Beschreibung je Stufe; der Index ist die Stufe (0 bis 5).
 */
public record BeantwortbareFrage(long id, int position, String text, List<String> vorgaben) {
}
