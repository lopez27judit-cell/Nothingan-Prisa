
package cat.copernic.nothinganpri.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cat.copernic.nothinganpri.dto.DocumentClient;
import cat.copernic.nothinganpri.entity.ClientJpaEntity;
import cat.copernic.nothinganpri.service.ClientService;

@Controller
public class ClientController {

    @Autowired
    private ClientService clientService;

    // =========================================
    // MOSTRAR FITXA DEL CLIENT
    // =========================================

    @GetMapping("/clients/{dni}")
    public String mostrarClient(
            @PathVariable String dni,
            Model model) {

        Optional<ClientJpaEntity> client =
                clientService.findByDNI(dni);

        if (client.isEmpty()) {
            return "redirect:/clients";
        }

        model.addAttribute("client", client.get());

        return "fitxa-client";
    }

    // =========================================
    // MOSTRAR DOCUMENT DNI
    // =========================================

    @GetMapping("/clients/{dni}/document/dni")
    public ResponseEntity<byte[]> veureDni(
            @PathVariable String dni) {

        Optional<DocumentClient> document =
                clientService.obtenirDni(dni);

        return crearRespostaDocument(document);
    }

    // =========================================
    // MOSTRAR LLICENCIA
    // =========================================

    @GetMapping("/clients/{dni}/document/llicencia")
    public ResponseEntity<byte[]> veureLlicencia(
            @PathVariable String dni) {

        Optional<DocumentClient> document =
                clientService.obtenirLlicencia(dni);

        return crearRespostaDocument(document);
    }

    // =========================================
    // GENERAR RESPOSTA DEL DOCUMENT
    // =========================================

    private ResponseEntity<byte[]> crearRespostaDocument(
            Optional<DocumentClient> document) {

        if (document.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DocumentClient dades = document.get();

        MediaType mediaType =
                obtenirMediaType(dades.getTipus());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline")
                .header(
                        "X-Content-Type-Options",
                        "nosniff")
                .contentType(mediaType)
                .body(dades.getDades());
    }

    // =========================================
    // OBTENER TIPO MIME
    // =========================================

    private MediaType obtenirMediaType(String tipus) {

        if (tipus == null || tipus.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(tipus);

        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    // =========================================
    // ELIMINAR DEFINITIVAMENTE CLIENT
    // =========================================

    @PostMapping("/clients/eliminar/{id}")
    public String eliminarClient(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Optional<ClientJpaEntity> client =
                clientService.findById(id);

        if (client.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "El client no existeix.");

            return "redirect:/clients";
        }

        String dni = client.get().getDNI();

        boolean eliminat =
                clientService.eliminarClient(id);

        if (!eliminat) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No es pot eliminar el client perquè té reserves assignades.");

            return "redirect:/agent/clients/" + dni;
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "El client s'ha eliminat definitivament.");

        return "redirect:/agent/clients";
    }
}
