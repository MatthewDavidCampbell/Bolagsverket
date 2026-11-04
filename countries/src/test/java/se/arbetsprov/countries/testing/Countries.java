package se.arbetsprov.countries.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import se.arbetsprov.countries.Country;

/** Test data: countries and v5 response bodies. */
public final class Countries {

    private Countries() {
    }

    public static Country country(String code, String name, String region, long population) {
        return new Country(code, name, name, region, null, null, population);
    }

    /** {@code count} countries named "Country 01".."Country NN" with codes C01..CNN in Europe. */
    public static List<Country> numbered(int count) {
        var list = new ArrayList<Country>();
        for (int i = 1; i <= count; i++) {
            list.add(country("C%02d".formatted(i), "Country %02d".formatted(i), "Europe", i * 1000L));
        }
        return list;
    }

    /** A v5 country object as JSON. {@code capital} may be null for an empty capitals list. */
    public static String v5Country(String alpha3, String name, String region, String capital, long population) {
        String capitals = capital == null ? "[]" : "[{\"name\":\"" + capital + "\"}]";
        return """
                {"names":{"common":"%s","official":"Official %s"},"codes":{"alpha_3":"%s"},\
                "capitals":%s,"region":"%s","subregion":"Sub %s","population":%d}"""
                .formatted(name, name, alpha3, capitals, region, region, population);
    }

    /** A v5 list page body. */
    public static String v5Page(List<String> objects, int total, int offset, boolean more) {
        return """
                {"data":{"objects":[%s],"meta":{"total":%d,"count":%d,"limit":100,"offset":%d,"more":%b}}}"""
                .formatted(objects.stream().collect(Collectors.joining(",")), total, objects.size(), offset, more);
    }

    /** {@code count} v5 objects numbered from {@code first}, with codes such as X007. */
    public static List<String> v5Countries(int first, int count) {
        var list = new ArrayList<String>();
        for (int i = first; i < first + count; i++) {
            list.add(v5Country("X%03d".formatted(i), "Country " + i, "Europe", "Capital " + i, i));
        }
        return list;
    }
}
