package cat.copernic.nothinganpri.validacio;

import java.text.Normalizer;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProvinciaValidator implements ConstraintValidator<ProvinciaValida, String> {

    // Provincias en catalán y con la primera letra en mayúscula
    private static final Set<String> PROVINCIES_VISIBLES = Stream.of(
            "Araba",
            "Albacete",
            "Alacant",
            "Almería",
            "Astúries",
            "Àvila",
            "Badajoz",
            "Barcelona",
            "Burgos",
            "Càceres",
            "Cadis",
            "Cantàbria",
            "Castelló",
            "Ciudad Real",
            "Còrdova",
            "A Corunya",
            "Conca",
            "Girona",
            "Granada",
            "Guadalajara",
            "Guipúscoa",
            "Huelva",
            "Osca",
            "Illes Balears",
            "Jaén",
            "Lleó",
            "Lleida",
            "Lugo",
            "Madrid",
            "Màlaga",
            "Múrcia",
            "Navarra",
            "Pontevedra",
            "La Rioja",
            "Salamanca",
            "Santa Cruz de Tenerife",
            "Segòvia",
            "Sevilla",
            "Sòria",
            "Tarragona",
            "Terol",
            "Toledo",
            "València",
            "Valladolid",
            "Biscaia",
            "Zamora",
            "Saragossa",
            "Ceuta",
            "Melilla",
            "Las Palmas"
    ).collect(Collectors.toSet());

    // Provincias normalizadas para validar sin distinguir acentos
    private static final Set<String> PROVINCIES_NORMALITZADES =
            PROVINCIES_VISIBLES.stream()
                    .map(ProvinciaValidator::normalitzar)
                    .collect(Collectors.toSet());

    // Normaliza el texto: elimina acentos, espacios sobrantes y diferencias de mayúsculas
    private static String normalitzar(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        if (valor == null || valor.isBlank()) {
            return true;
        }

        return PROVINCIES_NORMALITZADES.contains(normalitzar(valor));
    }

    // Retorna els noms correctes per mostrar al desplegable
    public static Set<String> obtenirProvincies() {
        return PROVINCIES_VISIBLES;
    }
}