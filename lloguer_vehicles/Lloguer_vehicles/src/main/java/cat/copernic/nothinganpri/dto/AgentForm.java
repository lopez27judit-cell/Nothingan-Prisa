package cat.copernic.nothinganpri.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AgentForm {

    // Lo rellena el controller desde la URL; permite usar ${agent.id} en la plantilla
    private Long id;

    @NotBlank(message = "El DNI és obligatori")
    @Pattern(
        regexp = "^([0-9]{8}[A-Z]|[XYZ][0-9]{7}[A-Z])$",
        message = "El format del DNI/NIE no és vàlid"
    )
    private String DNI;

    @NotBlank(message = "El nom és obligatori")
    @Size(max = 50, message = "El nom no pot superar els 50 caràcters")
    @Pattern(
        regexp = "^[\\p{L}]+(?:[ '\\u2019-][\\p{L}]+)*$",
        message = "El nom només pot contenir lletres i separadors entre paraules"
    )
    private String nom;

    @NotBlank(message = "Els cognoms són obligatoris")
    @Size(max = 100, message = "Els cognoms no poden superar els 100 caràcters")
    @Pattern(
        regexp = "^[\\p{L}]+(?:[ '\\u2019-][\\p{L}]+)*$",
        message = "Els cognoms només poden contenir lletres i separadors entre paraules"
    )
    private String cognoms;

    @NotBlank(message = "El telèfon és obligatori")
    @Pattern(
        regexp = "^[0-9]{9}$",
        message = "El telèfon ha de contenir exactament 9 números"
    )
    private String telefon;

    @NotBlank(message = "L'email és obligatori")
    @Email(message = "Introdueix un email vàlid")
    private String email;

    @NotNull(message = "La data de contracte és obligatòria")
    @PastOrPresent(message = "La data de contracte no pot ser futura")
    private LocalDate dataContracte;

    // Elimina espais sobrants (un valor només amb espais queda buit)
    private static String net(String s) {
        return s == null ? null : s.trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDNI() {
        return DNI;
    }

    public void setDNI(String DNI) {
        this.DNI = DNI == null ? null : DNI.trim().toUpperCase();
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = net(nom);
    }

    public String getCognoms() {
        return cognoms;
    }

    public void setCognoms(String cognoms) {
        this.cognoms = net(cognoms);
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = net(telefon);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = net(email);
    }

    public LocalDate getDataContracte() {
        return dataContracte;
    }

    public void setDataContracte(LocalDate dataContracte) {
        this.dataContracte = dataContracte;
    }
}