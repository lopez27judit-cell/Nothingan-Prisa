
package cat.copernic.nothinganpri.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cat.copernic.nothinganpri.dto.AgentEditarForm;
import cat.copernic.nothinganpri.dto.DocumentacioForm;
import cat.copernic.nothinganpri.dto.TargetaForm;
import cat.copernic.nothinganpri.entity.ClientJpaEntity;
import cat.copernic.nothinganpri.service.ClientService;
import cat.copernic.nothinganpri.service.ReservaService;
import cat.copernic.nothinganpri.validacio.ProvinciaValidator;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/agent/clients")
@PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR', 'AGENT')")
public class AdminClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ReservaService reservaService;

    // Evitar modificar camps sensibles des del formulari personal
    @InitBinder("client")
    public void configurarBinder(WebDataBinder binder) {

        binder.setDisallowedFields(
                "password",
                "rol",
                "activo",
                "DNI",
                "dni",
                "fotoDniAnverso",
                "fotoDniAnversoTipus",
                "fotoPermiso",
                "fotoPermisoTipus",
                "fotoDniReverso",
                "titularTargeta",
                "numtarjetacredit",
                "caducitatTargeta"
        );
    }

    // Preparar tots els formularis de la vista
    private void prepararVista(
            Model model,
            ClientJpaEntity client,
            AgentEditarForm form,
            DocumentacioForm documentacioForm,
            TargetaForm targetaForm) {

        if (documentacioForm == null) {
            documentacioForm = new DocumentacioForm();
        }

        // Carregar el DNI actual des de la base de dades
        documentacioForm.setNumeroDocument(client.getDNI());

        // Carregar la caducitat actual del DNI
        if (client.getCadDNI() != null && !client.getCadDNI().isBlank()) {
            try {
                documentacioForm.setDataCaducitat(
                        LocalDate.parse(client.getCadDNI())
                );
            } catch (Exception e) {
                documentacioForm.setDataCaducitat(null);
            }
        }

        // Carregar el tipus de llicència actual
        documentacioForm.setTipusLlicencia(client.getLliconduir());

        // Carregar la caducitat actual de la llicència
        if (client.getCdLLiconduir() != null
                && !client.getCdLLiconduir().isBlank()) {
            try {
                documentacioForm.setDataCaducitatLlicencia(
                        LocalDate.parse(client.getCdLLiconduir())
                );
            } catch (Exception e) {
                documentacioForm.setDataCaducitatLlicencia(null);
            }
        }

        model.addAttribute("dni", client.getDNI());
        model.addAttribute("client", form);
        model.addAttribute("documentacioForm", documentacioForm);

        model.addAttribute(
                "provincias",
                ProvinciaValidator.obtenirProvincies()
                        .stream()
                        .sorted()
                        .toList()
        );

        // Carregar les dades de la targeta des de la base de dades
        if (targetaForm == null) {
            targetaForm = new TargetaForm();

            targetaForm.setTitularTargeta(client.getTitularTargeta());
            targetaForm.setNumtarjetacredit(client.getNumtarjetacredit());
            targetaForm.setCaducitatTargeta(client.getCaducitatTargeta());
        }

        model.addAttribute("targetaForm", targetaForm);

        model.addAttribute(
                "reserves",
                reservaService.obtenirHistoricClient(client.getId())
        );
    }

    // Crear el formulari personal amb les dades actuals del client
    private AgentEditarForm crearFormPersonal(ClientJpaEntity client) {

        AgentEditarForm form = new AgentEditarForm();

        form.setNom(client.getNom());
        form.setCognoms(client.getCognoms());
        form.setTelefon(client.getTelefon());
        form.setEmail(client.getEmail());
        form.setAdreca(client.getAdreca());
        form.setNacionalitat(client.getNacionalitat());
        form.setProvincia(client.getProvincia());

        return form;
    }

    // Mostrar perfil del client
    @GetMapping("/{dni}/perfil")
    public String mostrarPerfil(
            @PathVariable String dni,
            Model model,
            RedirectAttributes redirectAttributes) {

        ClientJpaEntity client = clientService.findByDNI(dni)
                .orElse(null);

        if (client == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No s'ha trobat el client."
            );

            return "redirect:/agent/clients";
        }

        prepararVista(
                model,
                client,
                crearFormPersonal(client),
                new DocumentacioForm(),
                null
        );

        return "edit-user";
    }

    // Guardar NOMÉS les dades personals
    @PostMapping("/{dni}/guardar")
    public String guardarDatos(
            @PathVariable String dni,

            @Valid @ModelAttribute("client") AgentEditarForm dades,
            BindingResult bindingResult,

            Model model,
            RedirectAttributes redirectAttributes) {

        ClientJpaEntity client = clientService.findByDNI(dni)
                .orElse(null);

        if (client == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No s'ha trobat el client."
            );

            return "redirect:/agent/clients";
        }

        if (bindingResult.hasErrors()) {
            prepararVista(
                    model,
                    client,
                    dades,
                    new DocumentacioForm(),
                    null
            );

            return "edit-user";
        }

        try {
            client.setNom(dades.getNom());
            client.setCognoms(dades.getCognoms());
            client.setTelefon(dades.getTelefon());
            client.setEmail(dades.getEmail());
            client.setAdreca(dades.getAdreca());
            client.setNacionalitat(dades.getNacionalitat());
            client.setProvincia(dades.getProvincia());

            clientService.actualizarClientAdmin(
                    dni,
                    client,
                    null,
                    null
            );

            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "Les dades personals s'han actualitzat correctament."
            );

            return "redirect:/agent/clients/" + dni + "/perfil";

        } catch (IllegalArgumentException | IOException e) {

            model.addAttribute("error", e.getMessage());

            prepararVista(
                    model,
                    client,
                    dades,
                    new DocumentacioForm(),
                    null
            );

            return "edit-user";
        }
    }

    // Guardar NOMÉS la documentació del DNI
    @PostMapping("/{dni}/guardar-documentacio")
    public String guardarDocumentacio(
            @PathVariable String dni,

            @ModelAttribute("documentacioForm")
            DocumentacioForm documentacioForm,

            @RequestParam(value = "fotoDniFile", required = false)
            MultipartFile fotoDni,

            Model model,
            RedirectAttributes redirectAttributes) {

        ClientJpaEntity client = clientService.findByDNI(dni)
                .orElse(null);

        if (client == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No s'ha trobat el client."
            );

            return "redirect:/agent/clients";
        }

        try {
            clientService.actualizarDocumentacio(
                    dni,
                    documentacioForm,
                    fotoDni
            );

            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "La documentació del DNI s'ha actualitzat correctament."
            );

            return "redirect:/agent/clients/" + dni + "/perfil";

        } catch (IllegalArgumentException e) {

            model.addAttribute("error", e.getMessage());

            prepararVista(
                    model,
                    client,
                    crearFormPersonal(client),
                    documentacioForm,
                    null
            );

            return "edit-user";

        } catch (IOException e) {

            model.addAttribute(
                    "error",
                    "No s'ha pogut actualitzar la documentació del DNI."
            );

            prepararVista(
                    model,
                    client,
                    crearFormPersonal(client),
                    documentacioForm,
                    null
            );

            return "edit-user";
        }
    }

    // Guardar NOMÉS la llicència de conduir
    @PostMapping("/{dni}/guardar-llicencia")
    public String guardarLlicencia(
            @PathVariable String dni,

            @ModelAttribute("documentacioForm")
            DocumentacioForm documentacioForm,

            @RequestParam(value = "fotoPermisoFile", required = false)
            MultipartFile fotoPermiso,

            Model model,
            RedirectAttributes redirectAttributes) {

        ClientJpaEntity client = clientService.findByDNI(dni)
                .orElse(null);

        if (client == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No s'ha trobat el client."
            );

            return "redirect:/agent/clients";
        }

        try {
            clientService.actualizarLlicencia(
                    dni,
                    documentacioForm,
                    fotoPermiso
            );

            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "La llicència s'ha actualitzat correctament."
            );

            return "redirect:/agent/clients/" + dni + "/perfil";

        } catch (IllegalArgumentException e) {

            model.addAttribute("error", e.getMessage());

            prepararVista(
                    model,
                    client,
                    crearFormPersonal(client),
                    documentacioForm,
                    null
            );

            return "edit-user";

        } catch (IOException e) {

            model.addAttribute(
                    "error",
                    "No s'ha pogut actualitzar la llicència."
            );

            prepararVista(
                    model,
                    client,
                    crearFormPersonal(client),
                    documentacioForm,
                    null
            );

            return "edit-user";
        }
    }

    // Guardar targeta
    @PostMapping("/{dni}/guardar-targeta")
    public String guardarTargeta(
            @PathVariable String dni,

            @Valid @ModelAttribute("targetaForm") TargetaForm targeta,
            BindingResult bindingResult,

            Model model,
            RedirectAttributes redirectAttributes) {

        ClientJpaEntity client = clientService.findByDNI(dni)
                .orElse(null);

        if (client == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No s'ha trobat el client."
            );

            return "redirect:/agent/clients";
        }

        if (bindingResult.hasErrors()) {
            prepararVista(
                    model,
                    client,
                    crearFormPersonal(client),
                    new DocumentacioForm(),
                    targeta
            );

            return "edit-user";
        }

        try {
            clientService.actualizarTargeta(
                    dni,
                    targeta.getTitularTargeta(),
                    targeta.getNumtarjetacredit(),
                    targeta.getCaducitatTargeta()
            );

            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "Les dades de la targeta s'han actualitzat correctament."
            );

            return "redirect:/agent/clients/" + dni + "/perfil";

        } catch (IllegalArgumentException e) {

            model.addAttribute("error", e.getMessage());

            prepararVista(
                    model,
                    client,
                    crearFormPersonal(client),
                    new DocumentacioForm(),
                    targeta
            );

            return "edit-user";

        } catch (Exception e) {

            model.addAttribute(
                    "error",
                    "No s'han pogut actualitzar les dades de la targeta."
            );

            prepararVista(
                    model,
                    client,
                    crearFormPersonal(client),
                    new DocumentacioForm(),
                    targeta
            );

            return "edit-user";
        }
    }

    // Canviar contrasenya
    @PostMapping("/{dni}/guardar-contrasenya")
    public String guardarContrasenya(
            @PathVariable String dni,

            @RequestParam String novaContrasenya,
            @RequestParam String confirmarContrasenya,

            RedirectAttributes redirectAttributes) {

        try {
            clientService.canviarContrasenya(
                    dni,
                    novaContrasenya,
                    confirmarContrasenya
            );

            redirectAttributes.addFlashAttribute(
                    "missatge",
                    "La contrasenya s'ha canviat correctament."
            );

            return "redirect:/agent/clients/" + dni + "/perfil";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/agent/clients/" + dni + "/perfil";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No s'ha pogut canviar la contrasenya."
            );

            return "redirect:/agent/clients/" + dni + "/perfil";
        }
    }
}
